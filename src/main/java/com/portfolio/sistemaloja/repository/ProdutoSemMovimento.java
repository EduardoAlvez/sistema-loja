package com.portfolio.sistemaloja.repository;

import java.time.LocalDate;

public record ProdutoSemMovimento(Long produtoId, String nome, String categoria,
                           int estoque, LocalDate ultimoMovimento) {
}
