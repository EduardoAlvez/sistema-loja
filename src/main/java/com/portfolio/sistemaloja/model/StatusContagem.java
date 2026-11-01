package com.portfolio.sistemaloja.model;

public enum StatusContagem {
    ABERTA("Aberta"),
    FECHADA("Fechada"),
    CANCELADA("Cancelada");

    private final String descricao;

    StatusContagem(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public static StatusContagem porNome(String nome) {
        if (nome == null) {
            return null;
        }
        String normalizado = nome.trim().toUpperCase();
        for (StatusContagem status : values()) {
            if (status.name().equals(normalizado)) {
                return status;
            }
        }
        return null;
    }
}
