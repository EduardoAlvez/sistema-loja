package com.portfolio.sistemaloja.model;

public enum PerfilUsuario {
    ADMIN("Administrador"),
    OPERADOR("Operador de caixa");

    private final String descricao;

    PerfilUsuario(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
