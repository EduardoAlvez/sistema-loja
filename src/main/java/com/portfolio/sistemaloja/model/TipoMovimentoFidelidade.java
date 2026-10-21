package com.portfolio.sistemaloja.model;

public enum TipoMovimentoFidelidade {

    CREDITO,
    DEBITO,
    ESTORNO_CREDITO,
    ESTORNO_DEBITO;

    public boolean creditaSaldo() {
        return this == CREDITO || this == ESTORNO_DEBITO;
    }

    public boolean debitaSaldo() {
        return this == DEBITO || this == ESTORNO_CREDITO;
    }
}
