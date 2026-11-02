package com.portfolio.sistemaloja.repository;

import java.math.BigDecimal;

public record TotaisFormaPagamento(String forma, int quantidade, BigDecimal total) {
}
