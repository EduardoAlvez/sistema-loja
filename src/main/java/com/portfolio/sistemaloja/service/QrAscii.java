package com.portfolio.sistemaloja.service;

import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.ByteMatrix;
import com.google.zxing.qrcode.encoder.Encoder;

import java.util.EnumMap;
import java.util.Map;

public final class QrAscii {

    public static final String MODULO = "█";
    private static final int MARGEM_MODULOS = 2;

    private QrAscii() {
    }

    public static String gerar(String conteudo) {
        if (conteudo == null || conteudo.isBlank()) {
            throw new ValidacaoException("Conteúdo vazio para gerar o QR Code.");
        }
        ByteMatrix matriz = matriz(conteudo);
        int largura = matriz.getWidth() + MARGEM_MODULOS * 2;
        String vazia = " ".repeat(largura);
        StringBuilder texto = new StringBuilder();
        for (int linha = 0; linha < MARGEM_MODULOS; linha++) {
            texto.append(vazia).append('\n');
        }
        for (int y = 0; y < matriz.getHeight(); y++) {
            texto.append(" ".repeat(MARGEM_MODULOS));
            for (int x = 0; x < matriz.getWidth(); x++) {
                texto.append(matriz.get(x, y) != 0 ? MODULO : " ");
            }
            texto.append(" ".repeat(MARGEM_MODULOS)).append('\n');
        }
        for (int linha = 0; linha < MARGEM_MODULOS; linha++) {
            texto.append(vazia).append('\n');
        }
        return texto.toString();
    }

    static ByteMatrix matriz(String conteudo) {
        Map<EncodeHintType, Object> dicas = new EnumMap<>(EncodeHintType.class);
        dicas.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        dicas.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        dicas.put(EncodeHintType.MARGIN, 0);
        try {
            return Encoder.encode(conteudo, ErrorCorrectionLevel.M, dicas).getMatrix();
        } catch (WriterException e) {
            throw new ValidacaoException("Falha ao gerar o QR Code do PIX: " + e.getMessage());
        }
    }
}
