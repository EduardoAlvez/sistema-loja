package com.portfolio.sistemaloja.impressao;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.FormaPagamento;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.model.VendaItem;
import com.portfolio.sistemaloja.service.ConfiguracaoPix;
import com.portfolio.sistemaloja.service.CupomTermicoService;
import com.portfolio.sistemaloja.service.ValidacaoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImpressaoCupomServiceTest {

    private static final ConfiguracaoPix CONFIG =
            new ConfiguracaoPix("pagamentos@lojaexemplo.com.br", "LOJA EXEMPLO LTDA", "SAO PAULO");
    private static final Charset CP850 = Charset.forName("Cp850");

    private Aplicacao app;
    private Usuario operador;
    private Venda venda;
    private ImpressorasFalsas impressoras;

    @BeforeEach
    void preparar() {
        app = BancoTeste.criar("impressao" + System.nanoTime());
        operador = app.getAuth().autenticar("operador", "caixa123");
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        venda = new Venda();
        venda.setUsuarioId(operador.getId());
        venda.setFormaPagamento(FormaPagamento.DINHEIRO);
        venda.setValorPago(new BigDecimal("10.00"));
        venda.adicionarItem(new VendaItem(agua.getId(), agua.getNome(), 4, agua.getPrecoVenda()));
        venda = app.getVendas().registrar(venda);
        impressoras = new ImpressorasFalsas("POS-80", "Cozinha");
    }

    @Test
    void enviaOCupomParaAImpressoraEscolhida() {
        CupomEscPosService cupom = new CupomEscPosService(new CupomTermicoService(CONFIG));
        ImpressaoCupomService servico = new ImpressaoCupomService(cupom, impressoras);

        servico.imprimir(venda, "POS-80");

        ImpressoraMemoria usada = impressoras.buscar("POS-80");
        assertEquals(1, usada.total());
        String conteudo = new String(usada.ultimoTrabalho(), CP850);
        assertTrue(conteudo.contains("CUPOM NAO FISCAL - 80mm"), conteudo);
        assertTrue(conteudo.contains("Água Mineral 500ml"), conteudo);
    }

    @Test
    void geraOsBytesParaSalvarEmBin() {
        CupomEscPosService cupom = new CupomEscPosService(new CupomTermicoService(CONFIG));
        ImpressaoCupomService servico = new ImpressaoCupomService(cupom, impressoras);

        byte[] dados = servico.bytes(venda);

        assertEquals(0x1B, dados[0]);
        assertEquals(0x40, dados[1]);
        assertEquals(0x1B, dados[2]);
        assertEquals(0x74, dados[3]);
        assertEquals(0x02, dados[4]);
        assertEquals(0x1D, dados[dados.length - 3]);
        assertEquals(0x56, dados[dados.length - 2]);
        assertEquals(0x01, dados[dados.length - 1]);
        assertEquals(0, impressoras.buscar("POS-80").total());
    }

    @Test
    void listaAsImpressorasDisponiveis() {        CupomEscPosService cupom = new CupomEscPosService(new CupomTermicoService(CONFIG));
        ImpressaoCupomService servico = new ImpressaoCupomService(cupom, impressoras);

        assertEquals(List.of("POS-80", "Cozinha"), servico.impressoras());
    }

    @Test
    void falhaSemImpressoraValida() {
        CupomEscPosService cupom = new CupomEscPosService(new CupomTermicoService(CONFIG));
        ImpressaoCupomService servico = new ImpressaoCupomService(cupom, impressoras);

        assertThrows(ImpressaoException.class, () -> servico.imprimir(venda, "Inexistente"));
        assertEquals(0, impressoras.buscar("POS-80").total());
    }

    @Test
    void exigeVenda() {
        CupomEscPosService cupom = new CupomEscPosService(new CupomTermicoService(CONFIG));
        ImpressaoCupomService servico = new ImpressaoCupomService(cupom, impressoras);

        assertThrows(ValidacaoException.class, () -> servico.imprimir(null, "POS-80"));
    }

    private static final class ImpressorasFalsas implements Impressoras {

        private final Map<String, ImpressoraMemoria> porNome = new LinkedHashMap<>();

        private ImpressorasFalsas(String... nomes) {
            for (String nome : nomes) {
                porNome.put(nome, new ImpressoraMemoria(nome));
            }
        }

        @Override
        public List<String> nomes() {
            return new ArrayList<>(porNome.keySet());
        }

        @Override
        public ImpressoraCupom selecionar(String nome) {
            ImpressoraMemoria impressora = porNome.get(nome);
            if (impressora == null) {
                throw new ImpressaoException("Impressora nao encontrada: " + nome);
            }
            return impressora;
        }

        private ImpressoraMemoria buscar(String nome) {
            return porNome.get(nome);
        }
    }
}
