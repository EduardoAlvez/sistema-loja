package com.portfolio.sistemaloja.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public record RecebimentoFiado(Long id, Long fiadoId, LocalDateTime dataPagamento,
                               BigDecimal valor, String formaPagamento, Long usuarioId,
                               String observacao) {

    public RecebimentoFiado {
        Objects.requireNonNull(fiadoId, "fiadoId");
        Objects.requireNonNull(dataPagamento, "dataPagamento");
        Objects.requireNonNull(valor, "valor");
        Objects.requireNonNull(formaPagamento, "formaPagamento");
        Objects.requireNonNull(usuarioId, "usuarioId");
    }
}
