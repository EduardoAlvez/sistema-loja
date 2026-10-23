package com.portfolio.sistemaloja.model;

import java.math.BigDecimal;

public record VendaParaEstorno(int pontosCreditados, BigDecimal somaItensBrutos, int pontosEstornados) {
}
