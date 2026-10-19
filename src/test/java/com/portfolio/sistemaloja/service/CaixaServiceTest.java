package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.Caixa;
import com.portfolio.sistemaloja.model.StatusCaixa;
import com.portfolio.sistemaloja.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CaixaServiceTest {

    private Aplicacao app;
    private Usuario operador;

    @BeforeEach
    void preparar() {
        app = BancoTeste.criar("caixa" + System.nanoTime());
        operador = app.getAuth().autenticar("operador", "caixa123");
    }

    @Test
    void abreCaixaComFundoDeTroco() {
        Caixa caixa = app.getCaixa().abrir(new BigDecimal("100.00"), operador);

        assertNotNull(caixa.getId());
        assertEquals(StatusCaixa.ABERTO, caixa.getStatus());
        assertEquals(0, new BigDecimal("100.00").compareTo(caixa.getValorAbertura()));

        Caixa aberto = app.getCaixa().caixaAberto().orElseThrow();
        assertEquals(caixa.getId(), aberto.getId());
        assertEquals("Operador Caixa", aberto.getUsuarioAberturaNome());
    }

    @Test
    void naoPermiteAbrirCaixaComOutroAberto() {
        Caixa primeiro = app.getCaixa().abrir(new BigDecimal("50.00"), operador);

        assertThrows(ValidacaoException.class,
                () -> app.getCaixa().abrir(new BigDecimal("10.00"), operador));
        assertEquals(primeiro.getId(), app.getCaixa().caixaAberto().orElseThrow().getId());
    }

    @Test
    void naoPermiteFundoDeTrocoNegativo() {
        assertThrows(ValidacaoException.class,
                () -> app.getCaixa().abrir(new BigDecimal("-0.01"), operador));
        assertTrue(app.getCaixa().caixaAberto().isEmpty());
    }

    @Test
    void semAberturaNaoExisteCaixaAberto() {
        assertTrue(app.getCaixa().caixaAberto().isEmpty());
    }
}
