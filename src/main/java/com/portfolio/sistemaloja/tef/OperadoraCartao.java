package com.portfolio.sistemaloja.tef;

public interface OperadoraCartao {

    RespostaTransacao autorizar(TransacaoCartao transacao);

    RespostaTransacao cancelar(String nsu, String numeroVenda);

    boolean disponivel();
}
