package com.portfolio.sistemaloja.impressao;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.FormaPagamento;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.model.VendaItem;
import com.portfolio.sistemaloja.model.VendaPagamento;
import com.portfolio.sistemaloja.service.ConfiguracaoPix;
import com.portfolio.sistemaloja.service.CupomTermicoService;
import com.portfolio.sistemaloja.service.QrAscii;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CupomEscPosServiceTest {

    private static final ConfiguracaoPix CONFIG =
            new ConfiguracaoPix("pagamentos@lojaexemplo.com.br", "LOJA EXEMPLO LTDA", "SAO PAULO");
    private static final Charset CP850 = Charset.forName("Cp850");

    private Aplicacao app;
    private Usuario operador;
    private CupomTermicoService texto;
    private CupomEscPosService cupom;

    @BeforeEach
    void preparar() {
        app = BancoTeste.criar("escpos" + System.nanoTime());
        operador = app.getAuth().autenticar("operador", "caixa123");
        texto = new CupomTermicoService(CONFIG);
        cupom = new CupomEscPosService(texto);
    }

    @Test
    void cupomTemCabecalhoItensTotalCorteEGaveta() {
        Venda venda = registrar(FormaPagamento.DINHEIRO, 4);

        byte[] bytes = cupom.formatar(venda);
        String conteudo = new String(bytes, CP850);
        String hex = hex(bytes);

        assertTrue(conteudo.contains("LOJA EXEMPLO LTDA"), conteudo);
        assertTrue(conteudo.contains("CUPOM NAO FISCAL - 80mm"), conteudo);
        assertTrue(conteudo.contains("Venda: " + venda.getNumero()), conteudo);
        assertTrue(conteudo.contains("Operador: Operador Caixa"), conteudo);
        assertTrue(conteudo.contains("Água Mineral 500ml"), conteudo);
        assertTrue(conteudo.contains("10,00"), conteudo);
        assertTrue(hex.startsWith("1b401b7402"), hex);
        assertTrue(hex.contains("1b700032fa"), "venda em dinheiro deve abrir a gaveta");
        assertTrue(hex.endsWith("0a1b64031d5601"), "cupom deve terminar com o corte parcial");
    }

    @Test
    void qrDoPixEhNativoDoModeloDois() {
        Venda venda = registrar(FormaPagamento.PIX, 4);

        byte[] bytes = cupom.formatar(venda);
        String conteudo = new String(bytes, CP850);
        String hex = hex(bytes);
        String payload = texto.payloadPix(venda);
        int tamanho = payload.getBytes(StandardCharsets.UTF_8).length + 3;
        String cabecalho = "1d286b"
                + String.format("%02x%02x", tamanho & 0xFF, (tamanho >> 8) & 0xFF)
                + "315030";

        assertTrue(conteudo.contains("PIX - QR CODE"), conteudo);
        assertFalse(conteudo.contains(QrAscii.MODULO), "ESC/POS nao deve usar a arte ASCII");
        assertTrue(hex.contains("1d286b040031413200"), "modelo 2 do QR");
        assertTrue(hex.contains("1d286b0300314304"), "modulo 4");
        assertTrue(hex.contains("1d286b0300314531"), "correcao de erro M");
        assertTrue(hex.contains(cabecalho + hex(payload.getBytes(StandardCharsets.UTF_8))),
                "payload do PIX deve ir no comando de armazenamento");
        assertTrue(hex.contains("1d286b0300315130"), "comando de impressao do QR");
        assertTrue(hex.endsWith("0a1b64031d5601"), "cupom deve terminar com o corte parcial");
        assertFalse(hex.contains("1b700032fa"), "venda no PIX nao deve abrir a gaveta");
    }

    @Test
    void vendaNoCartaoNaoAbreGavetaNemGeraQr() {
        Venda venda = registrar(FormaPagamento.CREDITO, 2);

        byte[] bytes = cupom.formatar(venda);
        String conteudo = new String(bytes, CP850);
        String hex = hex(bytes);

        assertFalse(hex.contains("1b700032fa"), hex);
        assertFalse(conteudo.contains("PIX - QR CODE"), conteudo);
        assertTrue(hex.endsWith("0a1b64031d5601"), hex);
    }

    @Test
    void divididoComDinheiroAbreGavetaEUsaSoOPixNoQr() {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        Venda venda = new Venda();
        venda.setUsuarioId(operador.getId());
        venda.setFormaPagamento(FormaPagamento.DINHEIRO);
        venda.adicionarItem(new VendaItem(agua.getId(), agua.getNome(), 4, agua.getPrecoVenda()));
        venda.adicionarPagamento(new VendaPagamento(FormaPagamento.DINHEIRO, new BigDecimal("6.00")));
        venda.adicionarPagamento(new VendaPagamento(FormaPagamento.PIX, new BigDecimal("4.00")));
        Venda registrada = app.getVendas().registrar(venda);

        String payload = texto.payloadPix(registrada);
        byte[] bytes = cupom.formatar(registrada);
        String conteudo = new String(bytes, CP850);
        String hex = hex(bytes);

        assertTrue(conteudo.contains("Pagamento dividido:"), conteudo);
        assertTrue(hex.contains("1b700032fa"), "parcela em dinheiro deve abrir a gaveta");
        assertTrue(hex.contains(hex(payload.getBytes(StandardCharsets.UTF_8))), hex);
        assertFalse(payload.contains("54056.00"), "QR deve cobrar apenas a parcela do PIX");
    }

    @Test
    void divididoSemDinheiroNaoAbreGaveta() {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        Venda venda = new Venda();
        venda.setUsuarioId(operador.getId());
        venda.setFormaPagamento(FormaPagamento.CREDITO);
        venda.adicionarItem(new VendaItem(agua.getId(), agua.getNome(), 4, agua.getPrecoVenda()));
        venda.adicionarPagamento(new VendaPagamento(FormaPagamento.CREDITO, new BigDecimal("6.00")));
        venda.adicionarPagamento(new VendaPagamento(FormaPagamento.PIX, new BigDecimal("4.00")));
        Venda registrada = app.getVendas().registrar(venda);

        byte[] bytes = cupom.formatar(registrada);
        String conteudo = new String(bytes, CP850);

        assertFalse(hex(bytes).contains("1b700032fa"), hex(bytes));
        assertTrue(conteudo.contains("PIX - QR CODE"), conteudo);
    }

    @Test
    void usaOModuloDoQrConfigurado() {
        CupomEscPosService comModulo6 = new CupomEscPosService(texto, 6);
        Venda venda = registrar(FormaPagamento.PIX, 4);

        assertTrue(hex(comModulo6.formatar(venda)).contains("1d286b0300314306"));
    }

    @Test
    void usaACodepageDaConfiguracao() {
        CupomEscPosService cp1252 = new CupomEscPosService(texto, new ImpressoraConfig("", 16, 4));
        Venda venda = registrar(FormaPagamento.PIX, 4);

        assertTrue(hex(cp1252.formatar(venda)).startsWith("1b401b7410"));
    }

    private Venda registrar(FormaPagamento forma, int quantidade) {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        Venda venda = new Venda();
        venda.setUsuarioId(operador.getId());
        venda.setFormaPagamento(forma);
        if (forma == FormaPagamento.DINHEIRO) {
            venda.setValorPago(agua.getPrecoVenda().multiply(BigDecimal.valueOf(quantidade)));
        }
        venda.adicionarItem(new VendaItem(agua.getId(), agua.getNome(), quantidade, agua.getPrecoVenda()));
        return app.getVendas().registrar(venda);
    }

    private static String hex(byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }
}
