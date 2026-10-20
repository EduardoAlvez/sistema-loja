package com.portfolio.sistemaloja.impressao;

import com.portfolio.sistemaloja.service.ValidacaoException;

import java.util.Set;

public record ImpressoraConfig(String nome, int codepage, int moduloQr) {

    private static final Set<Integer> PAGINAS = Set.of(EscPos.PAGINA_CP850, EscPos.PAGINA_CP1252);

    public ImpressoraConfig {
        nome = nome == null ? "" : nome.trim();
        if (!PAGINAS.contains(codepage)) {
            throw new ValidacaoException("Codepage da impressora deve ser 2 (Cp850) ou 16 (Cp1252).");
        }
        if (moduloQr < 1 || moduloQr > 16) {
            throw new ValidacaoException("Modulo do QR deve ficar entre 1 e 16.");
        }
    }

    public static ImpressoraConfig padrao() {
        return new ImpressoraConfig(
                System.getProperty("impressora.nome",
                        System.getenv().getOrDefault("IMPRESSORA_NOME", "")),
                inteiro("impressora.codepage", "IMPRESSORA_CODEPAGE", EscPos.PAGINA_CP850),
                inteiro("impressora.qr.modulo", "IMPRESSORA_QR_MODULO", EscPos.MODULO_QR_PADRAO));
    }

    private static int inteiro(String propriedade, String variavel, int padrao) {
        String valor = System.getProperty(propriedade);
        if (valor == null || valor.isBlank()) {
            valor = System.getenv(variavel);
        }
        if (valor == null || valor.isBlank()) {
            return padrao;
        }
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            throw new ValidacaoException("Valor invalido para " + propriedade + ": " + valor);
        }
    }
}
