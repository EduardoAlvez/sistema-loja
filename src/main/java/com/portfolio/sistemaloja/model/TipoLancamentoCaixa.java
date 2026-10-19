package com.portfolio.sistemaloja.model;

public enum TipoLancamentoCaixa {
    SUPRIMENTO("Suprimento"),
    SANGRIA("Sangria");

    private final String descricao;

    TipoLancamentoCaixa(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
