package com.portfolio.sistemaloja.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record FidelidadeConfig(BigDecimal pontosPorReal, BigDecimal valorPorPonto) {

    public static final BigDecimal PONTOS_POR_REAL_PADRAO = new BigDecimal("1");
    public static final BigDecimal VALOR_POR_PONTO_PADRAO = new BigDecimal("0.05");

    public FidelidadeConfig {
        pontosPorReal = pontosPorReal == null ? BigDecimal.ZERO : pontosPorReal;
        valorPorPonto = valorPorPonto == null ? BigDecimal.ZERO : valorPorPonto;
        if (pontosPorReal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidacaoException("Pontos por real deve ser maior que zero.");
        }
        if (valorPorPonto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidacaoException("Valor por ponto deve ser maior que zero.");
        }
        if (valorPorPonto.multiply(pontosPorReal).compareTo(BigDecimal.ONE) > 0) {
            throw new ValidacaoException(
                    "O resgate de pontos nao pode valer mais que o valor pago (fidelidade.valorPorPonto x "
                            + "fidelidade.pontosPorReal deve ser no maximo 1).");
        }
    }

    public static FidelidadeConfig padrao() {
        return new FidelidadeConfig(
                decimal("fidelidade.pontosPorReal", "FIDELIDADE_PONTOS_POR_REAL", PONTOS_POR_REAL_PADRAO),
                decimal("fidelidade.valorPorPonto", "FIDELIDADE_VALOR_PONTO", VALOR_POR_PONTO_PADRAO));
    }

    public int calcularPontos(BigDecimal valorVenda) {
        if (valorVenda == null || valorVenda.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return valorVenda.multiply(pontosPorReal).setScale(0, RoundingMode.FLOOR).intValue();
    }

    public BigDecimal valorDosPontos(int pontos) {
        if (pontos <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return valorPorPonto.multiply(BigDecimal.valueOf(pontos)).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal decimal(String propriedade, String variavel, BigDecimal padrao) {
        String valor = System.getProperty(propriedade);
        if (valor == null || valor.isBlank()) {
            valor = System.getenv(variavel);
        }
        if (valor == null || valor.isBlank()) {
            return padrao;
        }
        try {
            return new BigDecimal(valor.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new ValidacaoException("Valor invalido para " + propriedade + ": " + valor);
        }
    }
}
