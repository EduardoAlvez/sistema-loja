package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.model.FormaPagamento;
import com.portfolio.sistemaloja.model.VendaPagamento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PainelPagamentoDivididoTest {

    @Test
    void somaParcelasCalculaORestante() {
        PainelPagamentoDividido painel = new PainelPagamentoDividido(new BigDecimal("15.00"), FormaPagamento.PIX);

        painel.adicionarParcela(FormaPagamento.DINHEIRO, new BigDecimal("10.00"));

        assertEquals(0, new BigDecimal("10.00").compareTo(painel.somaParcelas()));
        assertEquals(0, new BigDecimal("5.00").compareTo(painel.restante()));
        assertFalse(painel.completo());
    }

    @Test
    void completoQuandoAsParcelasCobremOTotal() {
        PainelPagamentoDividido painel = new PainelPagamentoDividido(new BigDecimal("15.00"), FormaPagamento.PIX);
        painel.adicionarParcela(FormaPagamento.DINHEIRO, new BigDecimal("10.00"));
        painel.adicionarParcela(FormaPagamento.PIX, new BigDecimal("5.00"));

        assertTrue(painel.completo());
        assertEquals(0, BigDecimal.ZERO.compareTo(painel.restante()));

        List<VendaPagamento> parcelas = painel.parcelas();
        assertEquals(2, parcelas.size());
        assertEquals(FormaPagamento.DINHEIRO, parcelas.get(0).getFormaPagamento());
        assertEquals(FormaPagamento.PIX, parcelas.get(1).getFormaPagamento());
    }

    @Test
    void restanteNegativoIndicaTroco() {
        PainelPagamentoDividido painel = new PainelPagamentoDividido(new BigDecimal("15.00"), FormaPagamento.DINHEIRO);
        painel.adicionarParcela(FormaPagamento.DINHEIRO, new BigDecimal("20.00"));

        assertTrue(painel.completo());
        assertEquals(0, new BigDecimal("-5.00").compareTo(painel.restante()));
    }

    @Test
    void parcelaZeradaImpedeConfirmacao() {
        PainelPagamentoDividido painel = new PainelPagamentoDividido(new BigDecimal("10.00"), FormaPagamento.DINHEIRO);
        painel.adicionarParcela(FormaPagamento.DINHEIRO, new BigDecimal("10.00"));
        painel.adicionarParcela(FormaPagamento.PIX, BigDecimal.ZERO);

        assertFalse(painel.completo());
    }

    @Test
    void semParcelasNaoEstaCompleto() {
        PainelPagamentoDividido painel = new PainelPagamentoDividido(new BigDecimal("10.00"), FormaPagamento.PIX);

        assertFalse(painel.completo());
        assertEquals(0, new BigDecimal("10.00").compareTo(painel.restante()));
    }
}
