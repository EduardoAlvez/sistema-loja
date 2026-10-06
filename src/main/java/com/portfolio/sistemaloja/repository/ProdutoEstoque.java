package com.portfolio.sistemaloja.repository;

import java.math.BigDecimal;

public record ProdutoEstoque(long produtoId, String nome, int estoque, BigDecimal precoVenda) {
}
