package com.portfolio.sistemaloja.model;

public enum PerfilUsuario {
    ADMIN("Administrador", 50),
    OPERADOR("Operador de caixa", 10);

    private final String descricao;
    private final int percentualMaximoDesconto;

    PerfilUsuario(String descricao, int percentualMaximoDesconto) {
        this.descricao = descricao;
        this.percentualMaximoDesconto = percentualMaximoDesconto;
    }

    public String getDescricao() {
        return descricao;
    }

    public int getPercentualMaximoDesconto() {
        return percentualMaximoDesconto;
    }
}
