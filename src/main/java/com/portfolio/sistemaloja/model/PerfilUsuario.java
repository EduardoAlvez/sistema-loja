package com.portfolio.sistemaloja.model;

public enum PerfilUsuario {
    ADMIN("Administrador", 50, 3),
    GERENTE("Gerente", 30, 2),
    OPERADOR("Operador de caixa", 10, 1);

    private final String descricao;
    private final int percentualMaximoDesconto;
    private final int nivel;

    PerfilUsuario(String descricao, int percentualMaximoDesconto, int nivel) {
        this.descricao = descricao;
        this.percentualMaximoDesconto = percentualMaximoDesconto;
        this.nivel = nivel;
    }

    public String getDescricao() {
        return descricao;
    }

    public int getPercentualMaximoDesconto() {
        return percentualMaximoDesconto;
    }

    public int getNivel() {
        return nivel;
    }

    public boolean autoriza(PerfilUsuario operador) {
        return operador != null && nivel > operador.nivel;
    }
}
