package com.portfolio.sistemaloja.model;

public enum FormaPagamento {
    DINHEIRO("Dinheiro"),
    DEBITO("Cartão de débito"),
    CREDITO("Cartão de crédito"),
    PIX("PIX");

    private final String descricao;

    FormaPagamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
