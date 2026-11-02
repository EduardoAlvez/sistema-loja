package com.portfolio.sistemaloja.tef;

import com.portfolio.sistemaloja.tef.TransacaoCartao.TipoCartao;
import com.portfolio.sistemaloja.tef.simulado.OperadoraCartaoSimulada;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contrato da operadora simulada -- o unico TEF do projeto hoje. Os testes de formato
 * (NSU/auth) travam o formato prometido, e o tearDown devolve a fabrica ao padrao
 * porque o estado e global de processo.
 */
class OperadoraCartaoSimuladaTest {

    private final OperadoraCartaoSimulada simulada = new OperadoraCartaoSimulada();

    @AfterEach
    void restaurarPadraoDaFabrica() {
        OperadoraCartaoFactory.getInstance().configurar(new OperadoraCartaoSimulada());
    }

    @Test
    void aprovaValorPositivoComFormatosPrometidos() {
        TransacaoCartao transacao = new TransacaoCartao("V1", new BigDecimal("87.30"),
                "VISA", TipoCartao.CREDITO, 3, null);

        RespostaTransacao resposta = simulada.autorizar(transacao);

        assertTrue(resposta.isAprovado());
        assertEquals(StatusTransacao.APROVADA, resposta.getStatus());
        assertNotNull(resposta.getNsu());
        assertTrue(resposta.getNsu().matches("\\d{12}"), resposta.getNsu());
        assertNotNull(resposta.getCodigoAutorizacao());
        assertTrue(resposta.getCodigoAutorizacao().matches("\\d{6}"), resposta.getCodigoAutorizacao());
        assertTrue(Integer.parseInt(resposta.getCodigoAutorizacao()) >= 100000);
        assertEquals(0, new BigDecimal("87.30").compareTo(resposta.getValor()));
        assertEquals("VISA", resposta.getBandeira());
        assertEquals("SIMULADA", resposta.getAdquirente());
        assertNotNull(resposta.getDataHora());
    }

    @Test
    void adquirenteInformadoEcoado() {
        TransacaoCartao transacao = new TransacaoCartao("V2", new BigDecimal("10.00"),
                "MASTER", TipoCartao.DEBITO, 1, "CIELO");

        assertEquals("CIELO", simulada.autorizar(transacao).getAdquirente());
    }

    @Test
    void negacaoForcadaPeloConstrutorESetter() {
        TransacaoCartao transacao = new TransacaoCartao("V3", new BigDecimal("50.00"),
                "VISA", TipoCartao.CREDITO, 1, null);

        RespostaTransacao peloConstrutor = new OperadoraCartaoSimulada(true).autorizar(transacao);
        assertFalse(peloConstrutor.isAprovado());
        assertEquals(StatusTransacao.NEGADA, peloConstrutor.getStatus());
        assertEquals("Transação não autorizada pela operadora", peloConstrutor.getMensagem());

        simulada.setForcarNegacao(true);
        simulada.setMensagemNegacao("saldo insuficiente");
        RespostaTransacao peloSetter = simulada.autorizar(transacao);
        assertFalse(peloSetter.isAprovado());
        assertEquals("saldo insuficiente", peloSetter.getMensagem());
    }

    @Test
    void valorNaoPositivoENegado() {
        TransacaoCartao zero = new TransacaoCartao("V4", BigDecimal.ZERO,
                "VISA", TipoCartao.DEBITO, 1, null);
        TransacaoCartao negativo = new TransacaoCartao("V5", new BigDecimal("-1.00"),
                "VISA", TipoCartao.DEBITO, 1, null);

        RespostaTransacao respostaZero = simulada.autorizar(zero);
        assertEquals(StatusTransacao.NEGADA, respostaZero.getStatus());
        assertEquals("Valor inválido para transação TEF", respostaZero.getMensagem());
        assertEquals(StatusTransacao.NEGADA, simulada.autorizar(negativo).getStatus());
    }

    @Test
    void transacaoNulaOuSemValorDevolveErro() {
        RespostaTransacao nula = simulada.autorizar(null);
        assertEquals(StatusTransacao.ERRO, nula.getStatus());
        assertEquals("Dados da transação inválidos", nula.getMensagem());

        TransacaoCartao semValor = new TransacaoCartao("V6", null,
                "VISA", TipoCartao.DEBITO, 1, null);
        assertEquals(StatusTransacao.ERRO, simulada.autorizar(semValor).getStatus());
    }

    @Test
    void cancelamentoExigeNsuValido() {
        RespostaTransacao cancelado = simulada.cancelar("000000001234", "V1");
        assertTrue(cancelado.isAprovado());
        assertEquals(StatusTransacao.CANCELADA, cancelado.getStatus());

        assertEquals(StatusTransacao.ERRO, simulada.cancelar(null, "V1").getStatus());
        assertEquals(StatusTransacao.ERRO, simulada.cancelar("   ", "V1").getStatus());
    }

    @Test
    void disponivelSempreTrue() {
        assertTrue(simulada.disponivel());
    }

    @Test
    void fabricaNasceComSimuladaEConfigurarNullIgnora() {
        OperadoraCartaoFactory fabrica = OperadoraCartaoFactory.getInstance();
        assertInstanceOf(OperadoraCartaoSimulada.class, fabrica.getOperadora());

        OperadoraCartao original = fabrica.getOperadora();
        fabrica.configurar(null);
        assertSame(original, fabrica.getOperadora());

        OperadoraCartao custom = new OperadoraCartaoSimulada(true);
        fabrica.configurar(custom);
        assertSame(custom, fabrica.getOperadora());
    }
}
