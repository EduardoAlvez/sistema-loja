package com.portfolio.sistemaloja.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;

public final class PixPayload {

    private static final String ID_PAYLOAD = "01";
    private static final String GUI = "BR.GOV.BCB.PIX";
    private static final String MOEDA_BRASIL = "986";
    private static final String PAIS = "BR";

    private PixPayload() {
    }

    public static String gerar(ConfiguracaoPix configuracao, BigDecimal valor, String identificador) {
        configuracao.validar();
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidacaoException("O valor do PIX deve ser maior que zero.");
        }
        String txid = limparTxid(identificador == null || identificador.isBlank() ? "***" : identificador);

        String chave = limpar(configuracao.chave());
        if (chave.isBlank()) {
            throw new ValidacaoException("Informe uma chave PIX válida.");
        }
        chave = truncar(chave, 77);

        StringBuilder conta = new StringBuilder();
        conta.append(campo("00", GUI));
        conta.append(campo("01", chave));

        StringBuilder payload = new StringBuilder();
        payload.append(campo("00", ID_PAYLOAD));
        payload.append(campo("01", "11"));
        payload.append(campo("26", conta.toString()));
        payload.append(campo("52", "0000"));
        payload.append(campo("53", MOEDA_BRASIL));
        payload.append(campo("54", valor.setScale(2, RoundingMode.HALF_UP).toPlainString()));
        payload.append(campo("58", PAIS));
        payload.append(campo("59", truncar(limpar(configuracao.nomeRecebedor()), 25)));
        payload.append(campo("60", truncar(limpar(configuracao.cidade()), 15)));
        payload.append(campo("62", campo("05", txid)));
        payload.append("6304");

        return payload + crc16(payload.toString());
    }

    static String campo(String id, String valor) {
        return id + String.format("%02d", valor.length()) + valor;
    }

    static String crc16(String conteudo) {
        int crc = 0xFFFF;
        for (byte b : conteudo.getBytes(StandardCharsets.UTF_8)) {
            crc ^= (b & 0xFF) << 8;
            for (int bit = 0; bit < 8; bit++) {
                if ((crc & 0x8000) != 0) {
                    crc = (crc << 1) ^ 0x1021;
                } else {
                    crc <<= 1;
                }
                crc &= 0xFFFF;
            }
        }
        return String.format("%04X", crc);
    }

    private static String truncar(String texto, int limite) {
        return texto.length() <= limite ? texto : texto.substring(0, limite);
    }

    private static String limpar(String texto) {
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento.replaceAll("[^A-Za-z0-9 .@\\-_/]", "").strip();
    }

    private static String limparTxid(String texto) {
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String limpo = semAcento.replaceAll("[^A-Za-z0-9*\\-]", "");
        return truncar(limpo.isBlank() ? "***" : limpo, 25);
    }
}
