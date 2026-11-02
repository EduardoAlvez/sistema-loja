package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.Cliente;
import com.portfolio.sistemaloja.model.Fiado;
import com.portfolio.sistemaloja.model.FormaPagamento;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.model.VendaItem;
import com.portfolio.sistemaloja.repository.EventoAuditoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Trava da issue #42: venda a prazo e fiado no MESMO commit. Os cenarios espelham as
 * duas formas como o fluxo antigo (registrar e depois gerarFiado, em duas transacoes)
 * corrompia dados: o hook que falha dentro do INSERT do fiado (o que "cliente deletado
 * entre telas" ou banco fora faria) e o limite estourado sem PIN, cujo retry re-registrava
 * a venda. O ultimo teste documenta que o caminho antigo de duas chamadas diretas ainda
 * e capaz da orphan -- por isso o PDV nao o usa mais.
 */
class VendaFiadoTest {

    private Aplicacao app;
    private Usuario operador;
    private Usuario admin;
    private Cliente cliente;
    private Produto agua;

    @BeforeEach
    void preparar() {
        app = BancoTeste.criar("vendafiado" + System.nanoTime());
        admin = app.getAuth().autenticar("admin", "admin123");
        BancoTeste.abrirCaixa(app, admin);
        app.getConfiguracoes().definirFiadoHabilitado(true);
        cliente = app.getClientes().salvar(new Cliente(null, "Joao Lima", "52998224725",
                "11988887777", "joao@exemplo.com", true));
        app.getFiados().definirLimiteCredito(cliente.getId(), new BigDecimal("100.00"), admin.getId());
        operador = app.getAuth().autenticar("operador", "caixa123");
        agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
    }

    @Test
    void felizVendaAFiacadoVinculaAuditaEAbreConta() {
        Venda venda = novaVenda(2);

        Venda registrada = app.getVendas().registrarVendaFiado(venda,
                LocalDate.now().plusDays(15), "Venda a prazo - PDV", null);

        assertTrue(registrada.getId() > 0);
        List<Fiado> fiados = app.getFiados().listarPorCliente(cliente.getId());
        assertEquals(1, fiados.size());
        Fiado fiado = fiados.get(0);
        assertEquals(registrada.getId(), fiado.vendaId());
        assertEquals("ABERTO", fiado.status());
        assertEquals(0, registrada.getTotal().compareTo(fiado.valorDevedor()));
        assertEquals(agua.getEstoque() - 2,
                app.getProdutos().buscarPorCodigoBarras("7891000100104").getEstoque());

        List<String> acoes = acoesDeHoje();
        assertTrue(acoes.contains("VENDA_REGISTRADA"));
        assertTrue(acoes.contains("FIADO_GERADO"));
    }

    @Test
    void falhaNoInsertDoFiadoDesfazAteACompra() {
        app.getFiadoRepository().falharAoSalvarParaTeste(conexao -> {
            throw new ValidacaoException("fiado nao nasceu (falha simulada)");
        });
        Venda venda = novaVenda(2);

        assertThrows(ValidacaoException.class, () -> app.getVendas()
                .registrarVendaFiado(venda, LocalDate.now().plusDays(15), "obs", null));

        assertTrue(app.getRelatorios().vendasDoPeriodo(LocalDate.now(), LocalDate.now()).isEmpty(),
                "nenhuma venda pode sobreviver ao fiado que falhou");
        assertEquals(0, app.getFiados().listarPorCliente(cliente.getId()).size());
        assertEquals(120, app.getProdutos().buscarPorCodigoBarras("7891000100104").getEstoque(),
                "estoque nao pode baixar");
        List<String> acoes = acoesDeHoje();
        assertFalse(acoes.contains("VENDA_REGISTRADA"), "auditoria nao pode sobreviver ao rollback");
        assertFalse(acoes.contains("FIADO_GERADO"));
    }

    @Test
    void limiteEstouradoSemPinNaoPersisteNada() {
        // 50 aguas = 125.00 > limite 100.00: o exigir tem de vir ANTES de qualquer INSERT
        Venda venda = novaVenda(50);

        assertThrows(AutorizacaoNecessariaException.class, () -> app.getVendas()
                .registrarVendaFiado(venda, LocalDate.now().plusDays(15), "obs", null));

        assertTrue(app.getRelatorios().vendasDoPeriodo(LocalDate.now(), LocalDate.now()).isEmpty(),
                "a tentativa que precisa de PIN nao pode ter comitado venda");
        assertEquals(120, app.getProdutos().buscarPorCodigoBarras("7891000100104").getEstoque());
        assertTrue(app.getFiados().listarPorCliente(cliente.getId()).isEmpty());
    }

    @Test
    void fluxoAntigoDeDuasChamadasAindaDeixaOrfaPorIssoNaoUSamosMais() {
        Venda venda = novaVenda(2);
        Venda registrada = app.getVendas().registrar(venda);

        app.getAuth().autenticar("admin", "admin123");
        app.getConfiguracoes().definirFiadoHabilitado(false);
        app.getAuth().autenticar("operador", "caixa123");

        assertThrows(ValidacaoException.class, () -> app.getFiados().gerarFiado(
                cliente.getId(), registrada.getId(), registrada.getTotal(),
                LocalDate.now().plusDays(15), "obs", null));

        assertEquals(1, app.getRelatorios().vendasDoPeriodo(LocalDate.now(), LocalDate.now()).size(),
                "documenta a orphan do fluxo antigo: venda comitada sem recebivel");
        assertTrue(app.getFiados().listarPorCliente(cliente.getId()).isEmpty());
    }

    private Venda novaVenda(int quantidadeAgua) {
        Venda venda = new Venda();
        venda.setUsuarioId(operador.getId());
        venda.setClienteId(cliente.getId());
        venda.setFormaPagamento(FormaPagamento.DINHEIRO);
        venda.adicionarItem(new VendaItem(agua.getId(), agua.getNome(), quantidadeAgua,
                agua.getPrecoVenda()));
        return venda;
    }

    private List<String> acoesDeHoje() {
        return app.getAuditoria().listar(LocalDate.now(), LocalDate.now()).stream()
                .map(EventoAuditoria::acao)
                .toList();
    }
}
