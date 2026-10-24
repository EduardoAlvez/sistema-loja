package com.portfolio.sistemaloja.repository;

import java.math.BigDecimal;

public record CurvaABC(String nome, String tipo, int quantidade, BigDecimal total,
                BigDecimal percentual, String classe) {
}
