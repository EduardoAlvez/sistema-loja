package com.portfolio.sistemaloja.service;

public record ConfiguracaoPix(String chave, String nomeRecebedor, String cidade) {

    public static ConfiguracaoPix padrao() {
        String chave = System.getProperty("pix.chave");
        if (chave == null || chave.isBlank()) {
            chave = System.getenv("PIX_CHAVE");
        }
        if (chave == null || chave.isBlank()) {
            chave = "pagamentos@lojaexemplo.com.br";
        }
        String nome = System.getProperty("pix.nome");
        if (nome == null || nome.isBlank()) {
            nome = System.getenv("PIX_NOME");
        }
        if (nome == null || nome.isBlank()) {
            nome = "LOJA EXEMPLO LTDA";
        }
        String cidade = System.getProperty("pix.cidade");
        if (cidade == null || cidade.isBlank()) {
            cidade = System.getenv("PIX_CIDADE");
        }
        if (cidade == null || cidade.isBlank()) {
            cidade = "SAO PAULO";
        }
        return new ConfiguracaoPix(chave, nome, cidade);
    }

    public static final int CHAVE_MAX = 77;
    public static final int NOME_MAX = 25;
    public static final int CIDADE_MAX = 15;

    public void validar() {
        if (chave == null || chave.isBlank()) {
            throw new ValidacaoException("Informe a chave PIX da loja.");
        }
        if (nomeRecebedor == null || nomeRecebedor.isBlank()) {
            throw new ValidacaoException("Informe o nome do recebedor do PIX.");
        }
        if (cidade == null || cidade.isBlank()) {
            throw new ValidacaoException("Informe a cidade do recebedor do PIX.");
        }
    }
}
