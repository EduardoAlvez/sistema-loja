package com.portfolio.sistemaloja.db;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

public final class Senhas {

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private Senhas() {
    }

    public static String gerarSal() {
        byte[] sal = new byte[16];
        ALEATORIO.nextBytes(sal);
        return HexFormat.of().formatHex(sal);
    }

    public static String hash(String senha, String sal) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] entrada = (sal + ":" + senha).getBytes(StandardCharsets.UTF_8);
            byte[] resultado = digest.digest(entrada);
            return HexFormat.of().formatHex(resultado);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo de hash indisponível", e);
        }
    }

    public static boolean confere(String senha, String sal, String hashEsperado) {
        if (senha == null || sal == null || hashEsperado == null) {
            return false;
        }
        return MessageDigest.isEqual(
                hash(senha, sal).getBytes(StandardCharsets.UTF_8),
                hashEsperado.getBytes(StandardCharsets.UTF_8));
    }
}
