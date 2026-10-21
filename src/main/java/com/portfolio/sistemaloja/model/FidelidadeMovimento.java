package com.portfolio.sistemaloja.model;

public record FidelidadeMovimento(
        long clienteId,
        Long vendaId,
        Long devolucaoId,
        TipoMovimentoFidelidade tipo,
        int pontos,
        String descricao) {

    public FidelidadeMovimento {
        if (clienteId <= 0) {
            throw new IllegalArgumentException("Movimento de fidelidade exige cliente.");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("Movimento de fidelidade exige tipo.");
        }
        if (pontos <= 0) {
            throw new IllegalArgumentException("Movimento de fidelidade exige pontos positivos.");
        }
    }
}
