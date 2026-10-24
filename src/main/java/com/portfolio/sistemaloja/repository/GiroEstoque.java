package com.portfolio.sistemaloja.repository;

import java.math.BigDecimal;

public record GiroEstoque(Long produtoId, String nome, String categoria, int estoque,
                   int estoqueMinimo, int vendasPeriodo, BigDecimal giro,
                   boolean ruptura) {
}
