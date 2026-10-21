package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.model.ExtratoFidelidade;
import com.portfolio.sistemaloja.model.FidelidadeMovimento;
import com.portfolio.sistemaloja.model.TipoMovimentoFidelidade;
import com.portfolio.sistemaloja.repository.FidelidadeRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class FidelidadeService {

    private static final int LIMITE_EXTRATO = 20;

    private final FidelidadeRepository fidelidade;
    private final FidelidadeConfig config;

    public FidelidadeService(FidelidadeRepository fidelidade, FidelidadeConfig config) {
        this.fidelidade = fidelidade;
        this.config = config;
    }

    public int saldo(long clienteId) {
        return fidelidade.saldo(clienteId);
    }

    public List<ExtratoFidelidade> extrato(long clienteId) {
        return fidelidade.extrato(clienteId, LIMITE_EXTRATO);
    }

    public FidelidadeConfig getConfig() {
        return config;
    }

    public BigDecimal valorDosPontos(int pontos) {
        return config.valorDosPontos(pontos);
    }
    public int calcularCredito(BigDecimal totalVenda) {
        return config.calcularPontos(totalVenda);
    }

    public int calcularEstorno(int pontosCreditados, BigDecimal baseVenda, BigDecimal baseDevolucao) {
        if (pontosCreditados <= 0 || baseVenda == null || baseVenda.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        BigDecimal devolvido = baseDevolucao == null ? BigDecimal.ZERO : baseDevolucao;
        if (devolvido.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        BigDecimal fracao = devolvido.min(baseVenda).divide(baseVenda, 6, RoundingMode.HALF_UP);
        int estorno = BigDecimal.valueOf(pontosCreditados).multiply(fracao)
                .setScale(0, RoundingMode.FLOOR).intValue();
        return Math.max(estorno, 0);
    }

    public FidelidadeMovimento credito(long clienteId, Long vendaId, int pontos, String descricao) {
        return new FidelidadeMovimento(clienteId, vendaId, null, TipoMovimentoFidelidade.CREDITO, pontos, descricao);
    }

    public FidelidadeMovimento debito(long clienteId, Long vendaId, int pontos, String descricao) {
        return new FidelidadeMovimento(clienteId, vendaId, null, TipoMovimentoFidelidade.DEBITO, pontos, descricao);
    }

    public FidelidadeMovimento estornoCredito(long clienteId, Long vendaId, int pontos, String descricao) {
        return new FidelidadeMovimento(clienteId, vendaId, null, TipoMovimentoFidelidade.ESTORNO_CREDITO, pontos,
                descricao);
    }

    public FidelidadeMovimento estornoDebito(long clienteId, Long vendaId, int pontos, String descricao) {
        return new FidelidadeMovimento(clienteId, vendaId, null, TipoMovimentoFidelidade.ESTORNO_DEBITO, pontos,
                descricao);
    }
}
