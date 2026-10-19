package com.portfolio.sistemaloja.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PixPayloadTest {

    private static final ConfiguracaoPix CONFIG =
            new ConfiguracaoPix("pagamentos@lojaexemplo.com.br", "LOJA EXEMPLO LTDA", "SAO PAULO");

    @Test
    void geraPayloadComOsCamposObrigatorios() {
        String payload = PixPayload.gerar(CONFIG, new BigDecimal("10.50"), "V000012");

        assertTrue(payload.startsWith("000201"), payload);
        assertTrue(payload.contains("26"), payload);
        assertTrue(payload.contains("BR.GOV.BCB.PIX"), payload);
        assertTrue(payload.contains("0129pagamentos@lojaexemplo.com.br"), payload);
        assertTrue(payload.contains("5303986"), payload);
        assertTrue(payload.contains("540510.50"), payload);
        assertTrue(payload.contains("5802BR"), payload);
        assertTrue(payload.contains("5917LOJA EXEMPLO LTDA"), payload);
        assertTrue(payload.contains("6009SAO PAULO"), payload);
        assertTrue(payload.contains("0507V000012"), payload);
        assertTrue(payload.endsWith(crc16(payload.substring(0, payload.length() - 4))), payload);
    }

    @Test
    void usaIdentificadorPadraoQuandoNaoInformado() {
        String payload = PixPayload.gerar(CONFIG, BigDecimal.ONE, null);

        assertTrue(payload.contains("0503***"), payload);
    }

    @Test
    void removeAcentosEUsaTxidPadraoCurto() {
        ConfiguracaoPix acentuada = new ConfiguracaoPix("+5511999998888", "Mercado São João & Cia Ltda.",
                "São Paulo");
        String payload = PixPayload.gerar(acentuada, new BigDecimal("7.5"), "VENDA-123456789012345678901234567890");

        assertTrue(payload.contains("BR.GOV.BCB.PIX"), payload);
        assertTrue(payload.contains("5925Mercado Sao Joao  Cia Ltd"), payload);
        assertTrue(payload.contains("0113551199999888"), payload);
        assertTrue(payload.contains("6009Sao Paulo"), payload);
        assertFalse(payload.contains("São"), payload);
        assertTrue(payload.contains("0525VENDA-1234567890123456789"), payload);
    }

    @Test
    void recusaValorInvalidoOuChaveVazia() {
        assertThrows(ValidacaoException.class,
                () -> PixPayload.gerar(CONFIG, BigDecimal.ZERO, "V1"));
        assertThrows(ValidacaoException.class,
                () -> PixPayload.gerar(CONFIG, null, "V1"));
        assertThrows(ValidacaoException.class,
                () -> PixPayload.gerar(new ConfiguracaoPix("  ", "Loja", "Cidade"), BigDecimal.ONE, "V1"));
        assertThrows(ValidacaoException.class,
                () -> PixPayload.gerar(new ConfiguracaoPix("chave", "  ", "Cidade"), BigDecimal.ONE, "V1"));
        assertThrows(ValidacaoException.class,
                () -> PixPayload.gerar(new ConfiguracaoPix("chave", "Loja", "  "), BigDecimal.ONE, "V1"));
    }

    private static String crc16(String conteudo) {
        int registro = 0xFFFF;
        byte[] bytes = conteudo.getBytes(StandardCharsets.UTF_8);
        for (byte b : bytes) {
            for (int bit = 7; bit >= 0; bit--) {
                boolean bitLigado = ((registro >> 15) & 1) == 1;
                boolean bitEntrada = ((b >> bit) & 1) == 1;
                registro = (registro << 1) & 0xFFFF;
                if (bitLigado ^ bitEntrada) {
                    registro ^= 0x1021;
                }
            }
        }
        return String.format("%04X", registro);
    }
}
