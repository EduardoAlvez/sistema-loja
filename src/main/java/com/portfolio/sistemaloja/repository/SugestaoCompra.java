package com.portfolio.sistemaloja.repository;

public record SugestaoCompra(long produtoId, String nome, int estoqueAtual, int estoqueMinimo,
                            int vendidoPeriodo, int mediaDiaria, int quantidadeSugerida) {
}
