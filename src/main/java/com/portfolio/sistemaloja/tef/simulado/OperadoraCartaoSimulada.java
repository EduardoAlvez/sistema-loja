package com.portfolio.sistemaloja.tef.simulado;

import com.portfolio.sistemaloja.tef.OperadoraCartao;
import com.portfolio.sistemaloja.tef.RespostaTransacao;
import com.portfolio.sistemaloja.tef.TransacaoCartao;

import java.util.concurrent.ThreadLocalRandom;

public class OperadoraCartaoSimulada implements OperadoraCartao {

    private boolean forcarNegacao = false;
    private String mensagemNegacao = "Transação não autorizada pela operadora";

    public OperadoraCartaoSimulada() {
    }

    public OperadoraCartaoSimulada(boolean forcarNegacao) {
        this.forcarNegacao = forcarNegacao;
    }

    @Override
    public RespostaTransacao autorizar(TransacaoCartao transacao) {
        if (transacao == null || transacao.getValor() == null) {
            return RespostaTransacao.erro("Dados da transação inválidos");
        }
        if (forcarNegacao) {
            return RespostaTransacao.negada(mensagemNegacao);
        }
        if (transacao.getValor().signum() <= 0) {
            return RespostaTransacao.negada("Valor inválido para transação TEF");
        }
        String nsu = String.format("%012d", ThreadLocalRandom.current().nextLong(1_000_000_000_000L));
        String autorizacao = String.format("%06d", ThreadLocalRandom.current().nextInt(100000, 999999));
        RespostaTransacao resposta = RespostaTransacao.aprovada(nsu, autorizacao, transacao.getValor());
        resposta.setBandeira(transacao.getBandeira());
        resposta.setAdquirente(transacao.getAdquirente() == null ? "SIMULADA" : transacao.getAdquirente());
        return resposta;
    }

    @Override
    public RespostaTransacao cancelar(String nsu, String numeroVenda) {
        if (nsu == null || nsu.isBlank()) {
            return RespostaTransacao.erro("NSU inválido para cancelamento");
        }
        RespostaTransacao resposta = new RespostaTransacao();
        resposta.setStatus(com.portfolio.sistemaloja.tef.StatusTransacao.CANCELADA);
        resposta.setAprovado(true);
        resposta.setNsu(nsu);
        resposta.setMensagem("Cancelamento realizado com sucesso");
        resposta.setDataHora(java.time.LocalDateTime.now());
        return resposta;
    }

    @Override
    public boolean disponivel() {
        return true;
    }

    public void setForcarNegacao(boolean forcarNegacao) {
        this.forcarNegacao = forcarNegacao;
    }

    public void setMensagemNegacao(String mensagemNegacao) {
        this.mensagemNegacao = mensagemNegacao;
    }
}
