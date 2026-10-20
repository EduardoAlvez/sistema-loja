package com.portfolio.sistemaloja.impressao;

public enum Alinhamento {

    ESQUERDA(0),
    CENTRO(1),
    DIREITA(2);

    private final int comando;

    Alinhamento(int comando) {
        this.comando = comando;
    }

    public int comando() {
        return comando;
    }
}
