package com.portfolio.sistemaloja.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VendaItemDescontoTest {

    private static void assertMoeda(String esperada, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperada).compareTo(real));
    }

    @Test
    void promocaoViraDescontoMantendoSubtotalCobrado() {
        VendaItem item = new VendaItem(1L, "Refrigerante Cola 2L", 2, new BigDecimal("8.99"));
        item.recalcularPrecos(new BigDecimal("8.99"), new BigDecimal("7.99"));

        assertMoeda("8.99", item.getPrecoUnitario());
        assertMoeda("2.00", item.getDesconto());
        assertMoeda("2.00", item.getDescontoPromocao());
        assertMoeda("0.00", item.getDescontoManual());
        assertMoeda("15.98", item.getSubtotal());
    }

    @Test
    void descontoEditadoComoTotalPreservaPromocao() {
        VendaItem item = new VendaItem(1L, "Refrigerante Cola 2L", 2, new BigDecimal("8.99"));
        item.recalcularPrecos(new BigDecimal("8.99"), new BigDecimal("7.99"));
        item.setDesconto(new BigDecimal("2.00").add(new BigDecimal("3.00")));

        assertMoeda("3.00", item.getDescontoManual());
        assertMoeda("12.98", item.getSubtotal());

        item.setDesconto(new BigDecimal("1.00").max(item.getDescontoPromocao()));

        assertMoeda("2.00", item.getDesconto());
        assertMoeda("0.00", item.getDescontoManual());
        assertMoeda("15.98", item.getSubtotal());
    }

    @Test
    void mudancaDeVigentePreservaParteManual() {
        VendaItem item = new VendaItem(1L, "Refrigerante Cola 2L", 2, new BigDecimal("8.99"));
        item.recalcularPrecos(new BigDecimal("8.99"), new BigDecimal("7.99"));
        item.setDesconto(new BigDecimal("5.00"));
        item.recalcularPrecos(new BigDecimal("8.99"), new BigDecimal("6.99"));

        assertMoeda("4.00", item.getDescontoPromocao());
        assertMoeda("3.00", item.getDescontoManual());
        assertMoeda("7.00", item.getDesconto());
        assertMoeda("10.98", item.getSubtotal());
    }

    @Test
    void aumentoDeQuantidadeRecalculaResidualDaPromocao() {
        VendaItem item = new VendaItem(1L, "Refrigerante Cola 2L", 2, new BigDecimal("8.99"));
        item.recalcularPrecos(new BigDecimal("8.99"), new BigDecimal("7.99"));
        item.setDesconto(new BigDecimal("5.00"));
        item.setQuantidade(3);
        item.recalcularPrecos(new BigDecimal("8.99"), new BigDecimal("7.99"));

        assertMoeda("3.00", item.getDescontoPromocao());
        assertMoeda("3.00", item.getDescontoManual());
        assertMoeda("26.97", item.getValorBruto());
        assertMoeda("20.97", item.getSubtotal());
    }

    @Test
    void semPromocaoNadaMuda() {
        VendaItem item = new VendaItem(2L, "Arroz Tipo 1 5kg", 1, new BigDecimal("29.90"));
        item.setDesconto(new BigDecimal("4.00"));
        item.recalcularPrecos(new BigDecimal("29.90"), new BigDecimal("29.90"));

        assertMoeda("29.90", item.getPrecoUnitario());
        assertMoeda("4.00", item.getDesconto());
        assertMoeda("0.00", item.getDescontoPromocao());
        assertMoeda("25.90", item.getSubtotal());
    }

    @Test
    void itemLegadoNaoReprecificadoMantemValores() {
        VendaItem legado = new VendaItem(1L, "Refrigerante Cola 2L", 2, new BigDecimal("7.99"));
        legado.setDesconto(new BigDecimal("1.98"));

        assertMoeda("14.00", legado.getSubtotal());
        assertMoeda("7.99", legado.getPrecoUnitario());
        assertMoeda("0.00", legado.getDescontoPromocao());
    }

    @Test
    void unitarioPagoRateiaSubtotal() {
        VendaItem item = new VendaItem(1L, "Refrigerante Cola 2L", 3, new BigDecimal("8.99"));
        item.recalcularPrecos(new BigDecimal("8.99"), new BigDecimal("7.99"));

        assertMoeda("7.99", item.getUnitarioPago());

        VendaItem impar = new VendaItem(1L, "Item", 3, new BigDecimal("10.00"));
        impar.setDesconto(new BigDecimal("0.10"));
        assertMoeda("9.97", impar.getUnitarioPago());
    }
}
