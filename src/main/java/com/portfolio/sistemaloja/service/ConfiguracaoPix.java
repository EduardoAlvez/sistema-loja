package com.portfolio.sistemaloja.service;

public record ConfiguracaoPix(String chave, String nomeRecebedor, String cidade) {

    public static ConfiguracaoPix padrao() {
        return new ConfiguracaoPix(
                System.getProperty("pix.chave", System.getenv().getOrDefault("PIX_CHAVE",
                        "pagamentos@lojaexemplo.com.br")),
                System.getProperty("pix.nome", "LOJA EXEMPLO LTDA"),
                System.getProperty("pix.cidade", "SAO PAULO"));
    }

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
