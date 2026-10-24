package com.portfolio.sistemaloja.model;

import java.math.BigDecimal;
import java.util.Objects;

public record ConferenciaCaixa(BigDecimal valor, String observacao) {

    public ConferenciaCaixa {
        Objects.requireNonNull(valor, "valor");
    }
}
