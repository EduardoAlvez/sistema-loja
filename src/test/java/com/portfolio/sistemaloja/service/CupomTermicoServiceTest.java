package com.portfolio.sistemaloja.service;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.FormaPagamento;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.model.VendaItem;
import com.portfolio.sistemaloja.model.VendaPagamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CupomTermicoServiceTest {

    private static final ConfiguracaoPix CONFIG =
            new ConfiguracaoPix("pagamentos@lojaexemplo.com.br", "LOJA EXEMPLO LTDA", "SAO PAULO");

    private Aplicacao app;
    private Usuario operador;
    private CupomTermicoService cupom;

    @BeforeEach
    void preparar() {
        app = BancoTeste.criar("termico" + System.nanoTime());
        operador = app.getAuth().autenticar("operador", "caixa123");
        cupom = new CupomTermicoService(CONFIG);
    }

    @Test
    void cupomTemLarguraItensETotal() {
        Venda venda = registrar(FormaPagamento.DINHEIRO, 4);

        String texto = cupom.formatar(venda);

        assertTrue(texto.contains("LOJA EXEMPLO LTDA"), texto);
        assertTrue(texto.contains("CUPOM NAO FISCAL - 80mm"), texto);
        assertTrue(texto.contains("Venda: " + venda.getNumero()), texto);
        assertTrue(texto.contains("Operador: Operador Caixa"), texto);
        assertTrue(texto.contains("Água Mineral 500ml"), texto);
        assertTrue(texto.contains("10,00"), texto);
        assertFalse(texto.contains("PIX - QR CODE"), "venda em dinheiro nao deve ter QR do PIX");
        for (String linha : texto.split("\n")) {
            assertTrue(linha.length() <= CupomTermicoService.LARGURA,
                    "linha mais larga que a bobina de 80mm: [" + linha + "]");
        }
    }

    @Test
    void qrDoPixEhDecodificavelEOValorEhODaVenda() {
        Venda venda = registrar(FormaPagamento.PIX, 4);

        String texto = cupom.formatar(venda);
        String payload = PixPayload.gerar(CONFIG, venda.getTotal(), venda.getNumero());

        assertTrue(texto.contains("PIX - QR CODE"), texto);
        assertTrue(decodificarQr(texto).equals(payload),
                "QR decodificado difere do payload esperado");
        assertTrue(payload.contains("540510.00"), payload);
    }

    @Test
    void pagamentoDivididoUsaApenasOPixNoQr() {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        Venda venda = new Venda();
        venda.setUsuarioId(operador.getId());
        venda.setFormaPagamento(FormaPagamento.DINHEIRO);
        venda.adicionarItem(new VendaItem(agua.getId(), agua.getNome(), 4, agua.getPrecoVenda()));
        venda.adicionarPagamento(new VendaPagamento(FormaPagamento.DINHEIRO, new BigDecimal("6.00")));
        venda.adicionarPagamento(new VendaPagamento(FormaPagamento.PIX, new BigDecimal("4.00")));
        Venda registrada = app.getVendas().registrar(venda);

        String texto = cupom.formatar(registrada);
        String payload = PixPayload.gerar(CONFIG, new BigDecimal("4.00"), registrada.getNumero());

        assertEquals(0, new BigDecimal("4.00").compareTo(cupom.valorPix(registrada)));
        assertTrue(texto.contains("Pagamento dividido:"), texto);
        assertEquals(payload, decodificarQr(texto));
    }

    @Test
    void vendaSemPixNaoGeraQr() {
        Venda venda = registrar(FormaPagamento.CREDITO, 2);

        String texto = cupom.formatar(venda);

        assertFalse(texto.contains(QrAscii.MODULO));
        assertEquals(0, BigDecimal.ZERO.compareTo(cupom.valorPix(venda)));
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

    private String decodificarQr(String cupom) {
        List<String> linhasQr = new ArrayList<>();
        for (String linha : cupom.split("\n")) {
            if (linha.contains(QrAscii.MODULO)) {
                linhasQr.add(linha);
            }
        }
        assertFalse(linhasQr.isEmpty(), "cupom sem bloco de QR Code");
        int escala = 4;
        int largura = linhasQr.stream().mapToInt(String::length).max().orElseThrow();
        int altura = linhasQr.size();
        int margem = 4;
        int total = (largura + margem * 2) * escala;
        int[] pixels = new int[total * (altura + margem * 2) * escala];
        java.util.Arrays.fill(pixels, 0xFFFFFFFF);
        for (int y = 0; y < altura; y++) {
            String linha = linhasQr.get(y);
            for (int x = 0; x < linha.length(); x++) {
                if (linha.charAt(x) != QrAscii.MODULO.charAt(0)) {
                    continue;
                }
                for (int dy = 0; dy < escala; dy++) {
                    for (int dx = 0; dx < escala; dx++) {
                        int px = (x + margem) * escala + dx;
                        int py = (y + margem) * escala + dy;
                        pixels[py * total + px] = 0xFF000000;
                    }
                }
            }
        }
        try {
            RGBLuminanceSource fonte = new RGBLuminanceSource(total, (altura + margem * 2) * escala, pixels);
            Map<DecodeHintType, Object> dicas = new EnumMap<>(DecodeHintType.class);
            dicas.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
            Result resultado = new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(fonte)),
                    dicas);
            return resultado.getText();
        } catch (Exception e) {
            throw new AssertionError("QR Code do cupom nao pode ser lido: " + e.getMessage(), e);
        }
    }
}
