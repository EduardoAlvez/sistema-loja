package com.portfolio.sistemaloja.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VendaDoDia(LocalDate dia, int quantidade, BigDecimal total) {
}
