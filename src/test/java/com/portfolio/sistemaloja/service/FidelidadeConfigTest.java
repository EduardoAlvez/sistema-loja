package com.portfolio.sistemaloja.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FidelidadeConfigTest {

    @AfterEach
    void limparPropriedades() {
        System.clearProperty("fidelidade.pontosPorReal");
        System.clearProperty("fidelidade.valorPorPonto");
    }

    @Test
    void usaOsPadroesDaLoja() {
        FidelidadeConfig config = FidelidadeConfig.padrao();

        assertEquals(0, new BigDecimal("1").compareTo(config.pontosPorReal()));
        assertEquals(0, new BigDecimal("0.05").compareTo(config.valorPorPonto()));
    }

    @Test
    void leAPropriedadeDoSistema() {
        System.setProperty("fidelidade.pontosPorReal", "2");
        System.setProperty("fidelidade.valorPorPonto", "0.03");

        FidelidadeConfig config = FidelidadeConfig.padrao();

        assertEquals(0, new BigDecimal("2").compareTo(config.pontosPorReal()));
        assertEquals(0, new BigDecimal("0.03").compareTo(config.valorPorPonto()));
    }

    @Test
    void recusaValorInvalido() {
        System.setProperty("fidelidade.pontosPorReal", "abc");

        assertThrows(ValidacaoException.class, FidelidadeConfig::padrao);
    }

    @Test
    void recusaResgateQueValeMaisQueOPagamento() {
        ValidacaoException erro = assertThrows(ValidacaoException.class,
                () -> new FidelidadeConfig(new BigDecimal("1"), new BigDecimal("2")));

        assertEquals(true, erro.getMessage().contains("fidelidade.valorPorPonto"));
    }

    @Test
    void calculaPontosParaBaixo() {
        FidelidadeConfig config = new FidelidadeConfig(new BigDecimal("1"), new BigDecimal("0.05"));

        assertEquals(10, config.calcularPontos(new BigDecimal("10.99")));
        assertEquals(0, config.calcularPontos(BigDecimal.ZERO));
    }

    @Test
    void convertePontosEmDinheiro() {
        FidelidadeConfig config = new FidelidadeConfig(new BigDecimal("1"), new BigDecimal("0.05"));

        assertEquals(0, new BigDecimal("5.00").compareTo(config.valorDosPontos(100)));
        assertEquals(0, BigDecimal.ZERO.setScale(2).compareTo(config.valorDosPontos(0)));
    }
}
