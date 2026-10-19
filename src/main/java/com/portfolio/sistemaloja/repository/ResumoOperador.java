package com.portfolio.sistemaloja.repository;

import java.math.BigDecimal;

public record ResumoOperador(long usuarioId, String nome, BigDecimal comissaoPercentual, int vendas,
                             BigDecimal totalVendido, BigDecimal comissao) {
}
