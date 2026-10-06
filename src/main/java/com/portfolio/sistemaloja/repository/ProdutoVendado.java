package com.portfolio.sistemaloja.repository;

import java.math.BigDecimal;

public record ProdutoVendado(long produtoId, String nome, int quantidade, BigDecimal totalVendido) {
}
