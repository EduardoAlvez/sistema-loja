package com.portfolio.sistemaloja.tef;

/**
 * Contrato para integração com operadoras de cartão (TEF).
 * <p>
 * A aplicação chama este contrato para autorizar/cancelar transações sem acoplar-se
 * a um SDK específico. Em desenvolvimento e testes utiliza-se
 * {@link com.portfolio.sistemaloja.tef.simulado.OperadoraCartaoSimulada}.
 * Para integrar um SDK real (SiTef/PayGo/GetNet), basta criar uma nova implementação
 * e configurar via {@link OperadoraCartaoFactory}.
 * </p>
 * 
 * <p><strong>Fluxo:</strong> {@code PDV -> VendaService -> OperadoraCartao -> RespostaTransacao}</p>
 */
public interface OperadoraCartao {

    /**
     * Solicita autorização da transação junto à operadora.
     * 
     * @param transacao dados da transação (valor, bandeira, tipo, parcelas)
     * @return resposta com status, NSU, código de autorização e mensagem
     */
    RespostaTransacao autorizar(TransacaoCartao transacao);

    /**
     * Solicita cancelamento de uma transação já autorizada.
     * 
     * @param nsu NSU da transação original
     * @param numeroVenda número da venda associada
     * @return resposta com status do cancelamento
     */
    RespostaTransacao cancelar(String nsu, String numeroVenda);

    /**
     * Verifica se a operadora está disponível para autorização.
     * 
     * @return {@code true} se disponível
     */
    boolean disponivel();

}
