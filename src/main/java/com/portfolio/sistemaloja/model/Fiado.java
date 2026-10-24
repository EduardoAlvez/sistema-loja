package com.portfolio.sistemaloja.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record Fiado(Long id, Long vendaId, Long clienteId, BigDecimal valorTotal,
                    BigDecimal valorPago, BigDecimal valorDevedor, BigDecimal jurosAplicados,
                    LocalDate dataVencimento, String status, String observacao) {

    public Fiado {
        Objects.requireNonNull(vendaId, "vendaId");
        Objects.requireNonNull(clienteId, "clienteId");
        Objects.requireNonNull(valorTotal, "valorTotal");
        Objects.requireNonNull(valorPago, "valorPago");
        Objects.requireNonNull(valorDevedor, "valorDevedor");
        Objects.requireNonNull(jurosAplicados, "jurosAplicados");
        Objects.requireNonNull(dataVencimento, "dataVencimento");
        Objects.requireNonNull(status, "status");
    }

    public boolean isAberto() {
        return "ABERTO".equals(status);
    }

    public boolean isParcial() {
        return "PARCIAL".equals(status);
    }

    public boolean isQuitado() {
        return "QUITADO".equals(status);
    }

    public boolean isInadimplente() {
        return "INADIMPLENTE".equals(status);
    }
}
