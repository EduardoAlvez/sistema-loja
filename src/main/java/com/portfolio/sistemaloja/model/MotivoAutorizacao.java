package com.portfolio.sistemaloja.model;

public enum MotivoAutorizacao {
    DESCONTO_ACIMA_DA_ALCADA("desconto acima da alçada do perfil"),
    CANCELAMENTO_VENDA("cancelamento de venda"),
    SANGRIA("sangria de caixa");

    private final String descricao;

    MotivoAutorizacao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
