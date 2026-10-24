package com.portfolio.sistemaloja.repository;

import java.math.BigDecimal;
import java.util.Objects;

public record ComparativoPeriodo(String rotulo, BigDecimal total, int quantidade,
                                 BigDecimal variacao, BigDecimal variacaoPercentual) {
    public ComparativoPeriodo {
        Objects.requireNonNull(rotulo, "rotulo");
        Objects.requireNonNull(total, "total");
        Objects.requireNonNull(variacao, "variacao");
        Objects.requireNonNull(variacaoPercentual, "variacaoPercentual");
    }
}
