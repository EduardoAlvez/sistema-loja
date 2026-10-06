package com.portfolio.sistemaloja.service;

public final class Cpf {

    private Cpf() {
    }

    public static String limpar(String cpf) {
        return cpf == null ? "" : cpf.replaceAll("[^0-9]", "");
    }

    public static String formatar(String cpf) {
        String numeros = limpar(cpf);
        if (numeros.length() != 11) {
            return cpf == null ? "" : cpf;
        }
        return numeros.substring(0, 3) + "." + numeros.substring(3, 6) + "."
                + numeros.substring(6, 9) + "-" + numeros.substring(9);
    }

    public static boolean valido(String cpf) {
        String numeros = limpar(cpf);
        if (numeros.length() != 11 || numeros.chars().distinct().count() == 1) {
            return false;
        }
        int primeiro = digitoVerificador(numeros, 0);
        int segundo = digitoVerificador(numeros, 1);
        return primeiro == Character.digit(numeros.charAt(9), 10)
                && segundo == Character.digit(numeros.charAt(10), 10);
    }

    private static int digitoVerificador(String cpf, int posicao) {
        int total = 0;
        int peso = posicao + 10;
        for (int i = 0; i <= 8 + posicao; i++) {
            total += Character.digit(cpf.charAt(i), 10) * peso;
            peso--;
        }
        int resto = total % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
