package com.portfolio.sistemaloja.model;

import java.time.LocalDateTime;

public record ExtratoFidelidade(
        LocalDateTime dataHora,
        TipoMovimentoFidelidade tipo,
        int pontos,
        String descricao,
        Long vendaId) {
}
