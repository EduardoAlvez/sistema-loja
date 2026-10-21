package com.portfolio.sistemaloja.model;

public enum TipoMovimentoFidelidade {

    CREDITO,
    DEBITO,
    ESTORNO_CREDITO,
    ESTORNO_DEBITO;

    public boolean creditaSaldo() {
        return this != DEBITO;
    }
}
