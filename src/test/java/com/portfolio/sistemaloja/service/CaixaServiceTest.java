package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.Caixa;
import com.portfolio.sistemaloja.model.CaixaLancamento;
import com.portfolio.sistemaloja.model.FormaPagamento;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.StatusCaixa;
import com.portfolio.sistemaloja.model.TipoLancamentoCaixa;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.model.VendaItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CaixaServiceTest {

    private Aplicacao app;
    private Usuario operador;

    @BeforeEach
    void preparar() {
        app = BancoTeste.criar("caixa" + System.nanoTime());
        operador = app.getAuth().autenticar("operador", "caixa123");
    }

    @Test
    void abreCaixaComFundoDeTroco() {
        Caixa caixa = app.getCaixa().abrir(new BigDecimal("100.00"), operador);

        assertNotNull(caixa.getId());
        assertEquals(StatusCaixa.ABERTO, caixa.getStatus());
        assertEquals(0, new BigDecimal("100.00").compareTo(caixa.getValorAbertura()));

        Caixa aberto = app.getCaixa().caixaAberto().orElseThrow();
        assertEquals(caixa.getId(), aberto.getId());
        assertEquals("Operador Caixa", aberto.getUsuarioAberturaNome());
    }

    @Test
    void naoPermiteAbrirCaixaComOutroAberto() {
        Caixa primeiro = app.getCaixa().abrir(new BigDecimal("50.00"), operador);

        assertThrows(ValidacaoException.class,
                () -> app.getCaixa().abrir(new BigDecimal("10.00"), operador));
        assertEquals(primeiro.getId(), app.getCaixa().caixaAberto().orElseThrow().getId());
    }

    @Test
    void naoPermiteFundoDeTrocoNegativo() {
        assertThrows(ValidacaoException.class,
                () -> app.getCaixa().abrir(new BigDecimal("-0.01"), operador));
        assertTrue(app.getCaixa().caixaAberto().isEmpty());
    }

    @Test
    void semAberturaNaoExisteCaixaAberto() {
        assertTrue(app.getCaixa().caixaAberto().isEmpty());
    }

    @Test
    void supramentoAumentaOSaldoEmEspecie() {
        Caixa caixa = app.getCaixa().abrir(new BigDecimal("100.00"), operador);

        app.getCaixa().lancar(TipoLancamentoCaixa.SUPRIMENTO, new BigDecimal("50.00"), "troco extra", operador);

        assertEquals(0, new BigDecimal("150.00").compareTo(app.getCaixa().saldoEspecie(caixa)));
    }

    @Test
    void sangriaAbateDoSaldoEmEspecie() {
        Caixa caixa = app.getCaixa().abrir(new BigDecimal("100.00"), operador);

        app.getCaixa().lancar(TipoLancamentoCaixa.SANGRIA, new BigDecimal("40.00"), "depósito", operador);

        assertEquals(0, new BigDecimal("60.00").compareTo(app.getCaixa().saldoEspecie(caixa)));
    }

    @Test
    void sangriaNaoPodeExcederOSaldoEmEspecie() {
        Caixa caixa = app.getCaixa().abrir(new BigDecimal("100.00"), operador);

        ValidacaoException erro = assertThrows(ValidacaoException.class, () -> app.getCaixa()
                .lancar(TipoLancamentoCaixa.SANGRIA, new BigDecimal("100.01"), null, operador));
        assertTrue(erro.getMessage().contains("saldo em espécie"));
        assertEquals(0, new BigDecimal("100.00").compareTo(app.getCaixa().saldoEspecie(caixa)));
    }

    @Test
    void vendasEmDinheiroDoTurnoEntramNoSaldo() {
        Caixa caixa = app.getCaixa().abrir(new BigDecimal("100.00"), operador);
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        Venda venda = new Venda();
        venda.setUsuarioId(operador.getId());
        venda.setFormaPagamento(FormaPagamento.DINHEIRO);
        venda.setValorPago(new BigDecimal("20.00"));
        venda.adicionarItem(new VendaItem(agua.getId(), agua.getNome(), 4, agua.getPrecoVenda()));
        app.getVendas().registrar(venda);

        assertEquals(0, new BigDecimal("110.00").compareTo(app.getCaixa().saldoEspecie(caixa)));

        app.getCaixa().lancar(TipoLancamentoCaixa.SANGRIA, new BigDecimal("110.00"), null, operador);
        assertEquals(0, BigDecimal.ZERO.compareTo(app.getCaixa().saldoEspecie(caixa)));
    }

    @Test
    void naoPermiteLancamentoSemCaixaAberto() {
        ValidacaoException erro = assertThrows(ValidacaoException.class, () -> app.getCaixa()
                .lancar(TipoLancamentoCaixa.SUPRIMENTO, new BigDecimal("10.00"), null, operador));
        assertTrue(erro.getMessage().contains("Abra o caixa"));
    }

    @Test
    void naoPermiteLancamentoComValorZerado() {
        app.getCaixa().abrir(new BigDecimal("100.00"), operador);
        assertThrows(ValidacaoException.class, () -> app.getCaixa()
                .lancar(TipoLancamentoCaixa.SUPRIMENTO, BigDecimal.ZERO, null, operador));
    }

    @Test
    void listaOsLancamentosDoCaixaComUsuarioETipo() {
        Caixa caixa = app.getCaixa().abrir(new BigDecimal("100.00"), operador);
        app.getCaixa().lancar(TipoLancamentoCaixa.SUPRIMENTO, new BigDecimal("20.00"), "troco", operador);
        app.getCaixa().lancar(TipoLancamentoCaixa.SANGRIA, new BigDecimal("30.00"), "cofre", operador);

        List<CaixaLancamento> lancamentos = app.getCaixa().lancamentos(caixa);

        assertEquals(2, lancamentos.size());
        assertEquals(TipoLancamentoCaixa.SUPRIMENTO, lancamentos.get(0).getTipo());
        assertEquals(TipoLancamentoCaixa.SANGRIA, lancamentos.get(1).getTipo());
        assertEquals("Operador Caixa", lancamentos.get(0).getUsuarioNome());
        assertEquals("troco", lancamentos.get(0).getObservacao());
    }
}
