package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.Caixa;
import com.portfolio.sistemaloja.model.CaixaLancamento;
import com.portfolio.sistemaloja.model.FormaPagamento;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.StatusCaixa;
import com.portfolio.sistemaloja.model.Autorizacao;
import com.portfolio.sistemaloja.model.MotivoAutorizacao;
import com.portfolio.sistemaloja.model.TipoLancamentoCaixa;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.model.VendaItem;
import com.portfolio.sistemaloja.model.VendaPagamento;
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

        Autorizacao token = BancoTeste.autorizarSupervisor(app, MotivoAutorizacao.SANGRIA);
        app.getCaixa().lancar(TipoLancamentoCaixa.SANGRIA, new BigDecimal("40.00"), "depósito", operador, token);

        assertEquals(0, new BigDecimal("60.00").compareTo(app.getCaixa().saldoEspecie(caixa)));
    }

    @Test
    void sangriaNaoPodeExcederOSaldoEmEspecie() {
        Caixa caixa = app.getCaixa().abrir(new BigDecimal("100.00"), operador);

        Autorizacao token = BancoTeste.autorizarSupervisor(app, MotivoAutorizacao.SANGRIA);
        ValidacaoException erro = assertThrows(ValidacaoException.class, () -> app.getCaixa()
                .lancar(TipoLancamentoCaixa.SANGRIA, new BigDecimal("100.01"), null, operador, token));
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

        Autorizacao token110 = BancoTeste.autorizarSupervisor(app, MotivoAutorizacao.SANGRIA);
        app.getCaixa().lancar(TipoLancamentoCaixa.SANGRIA, new BigDecimal("110.00"), null, operador, token110);
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
        Autorizacao token30 = BancoTeste.autorizarSupervisor(app, MotivoAutorizacao.SANGRIA);
        app.getCaixa().lancar(TipoLancamentoCaixa.SANGRIA, new BigDecimal("30.00"), "cofre", operador, token30);

        List<CaixaLancamento> lancamentos = app.getCaixa().lancamentos(caixa);

        assertEquals(2, lancamentos.size());
        assertEquals(TipoLancamentoCaixa.SUPRIMENTO, lancamentos.get(0).getTipo());
        assertEquals(TipoLancamentoCaixa.SANGRIA, lancamentos.get(1).getTipo());
        assertEquals("Operador Caixa", lancamentos.get(0).getUsuarioNome());
        assertEquals("troco", lancamentos.get(0).getObservacao());
    }

    @Test
    void resumoDoTurnoDetalhaVendasPorFormaDePagamento() {
        Caixa caixa = app.getCaixa().abrir(new BigDecimal("100.00"), operador);
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        Venda dinheiro = new Venda();
        dinheiro.setUsuarioId(operador.getId());
        dinheiro.setFormaPagamento(FormaPagamento.DINHEIRO);
        dinheiro.setValorPago(new BigDecimal("20.00"));
        dinheiro.adicionarItem(new VendaItem(agua.getId(), agua.getNome(), 4, agua.getPrecoVenda()));
        app.getVendas().registrar(dinheiro);
        Venda pix = new Venda();
        pix.setUsuarioId(operador.getId());
        pix.setFormaPagamento(FormaPagamento.PIX);
        pix.adicionarItem(new VendaItem(agua.getId(), agua.getNome(), 2, agua.getPrecoVenda()));
        app.getVendas().registrar(pix);

        CaixaService.Resumo resumo = app.getCaixa().resumo(caixa);

        assertEquals(2, resumo.quantidadeVendas());
        assertEquals(0, new BigDecimal("15.00").compareTo(resumo.totalVendas()));
        assertEquals(0, new BigDecimal("10.00").compareTo(resumo.porForma().get(FormaPagamento.DINHEIRO)));
        assertEquals(0, new BigDecimal("5.00").compareTo(resumo.porForma().get(FormaPagamento.PIX)));
        assertEquals(0, new BigDecimal("110.00").compareTo(resumo.saldoEsperado()));
    }

    @Test
    void fechamentoRegistraValorStatusEDiferenca() {
        Caixa caixa = app.getCaixa().abrir(new BigDecimal("100.00"), operador);
        Autorizacao token30 = BancoTeste.autorizarSupervisor(app, MotivoAutorizacao.SANGRIA);
        app.getCaixa().lancar(TipoLancamentoCaixa.SANGRIA, new BigDecimal("30.00"), "cofre", operador, token30);

        CaixaService.Resumo fechado = app.getCaixa().fechar(new BigDecimal("65.00"), "conferido", operador);

        assertEquals(0, new BigDecimal("70.00").compareTo(fechado.saldoEsperado()));
        assertEquals(0, new BigDecimal("65.00").compareTo(fechado.valorInformado()));
        assertEquals(0, new BigDecimal("-5.00").compareTo(fechado.diferenca()));
        assertTrue(app.getCaixa().caixaAberto().isEmpty());
        Caixa fechadoBanco = app.getCaixa().buscarPorId(caixa.getId());
        assertEquals(StatusCaixa.FECHADO, fechadoBanco.getStatus());
        assertEquals("conferido", fechadoBanco.getObservacao());
        assertEquals(0, new BigDecimal("65.00").compareTo(fechadoBanco.getValorConferido()));
        assertEquals(0, new BigDecimal("-5.00").compareTo(fechadoBanco.getDiferenca()));
    }

    @Test
    void fechamentoConferidoIgualAoEsperadoSemDiferenca() {
        Caixa caixa = app.getCaixa().abrir(new BigDecimal("100.00"), operador);

        CaixaService.Resumo fechado = app.getCaixa().fechar(new BigDecimal("100.00"), null, operador);

        assertEquals(0, new BigDecimal("100.00").compareTo(fechado.saldoEsperado()));
        assertEquals(0, new BigDecimal("100.00").compareTo(fechado.valorInformado()));
        assertEquals(0, BigDecimal.ZERO.compareTo(fechado.diferenca()));
        Caixa fechadoBanco = app.getCaixa().buscarPorId(caixa.getId());
        assertEquals(StatusCaixa.FECHADO, fechadoBanco.getStatus());
        assertEquals(0, new BigDecimal("100.00").compareTo(fechadoBanco.getValorConferido()));
        assertEquals(0, BigDecimal.ZERO.compareTo(fechadoBanco.getDiferenca()));
    }

    @Test
    void fechamentoSemInformarValorUsaOSaldoEsperado() {
        Caixa caixa = app.getCaixa().abrir(new BigDecimal("100.00"), operador);

        CaixaService.Resumo fechado = app.getCaixa().fechar(null, null, operador);

        assertEquals(0, BigDecimal.ZERO.compareTo(fechado.diferenca()));
        assertEquals(0, new BigDecimal("100.00").compareTo(fechado.valorInformado()));
        assertEquals(StatusCaixa.FECHADO, app.getCaixa().buscarPorId(caixa.getId()).getStatus());
    }

    @Test
    void naoPermiteFecharSemCaixaAberto() {
        ValidacaoException erro = assertThrows(ValidacaoException.class,
                () -> app.getCaixa().fechar(new BigDecimal("10.00"), null, operador));
        assertTrue(erro.getMessage().contains("Não há caixa aberto"));
    }

    @Test
    void naoPermiteFecharComValorNegativo() {
        app.getCaixa().abrir(new BigDecimal("100.00"), operador);
        assertThrows(ValidacaoException.class,
                () -> app.getCaixa().fechar(new BigDecimal("-1.00"), null, operador));
        assertTrue(app.getCaixa().caixaAberto().isPresent());
    }

    @Test
    void naoPermiteFecharDuasVezes() {
        app.getCaixa().abrir(new BigDecimal("100.00"), operador);
        app.getCaixa().fechar(null, null, operador);

        assertThrows(ValidacaoException.class, () -> app.getCaixa().fechar(null, null, operador));
    }

    @Test
    void pagamentoDivididoEntraNoSaldoPelaParteEmDinheiro() {
        Caixa caixa = app.getCaixa().abrir(new BigDecimal("100.00"), operador);
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        Venda venda = new Venda();
        venda.setUsuarioId(operador.getId());
        venda.setFormaPagamento(FormaPagamento.PIX);
        venda.adicionarItem(new VendaItem(agua.getId(), agua.getNome(), 4, agua.getPrecoVenda()));
        venda.adicionarPagamento(new VendaPagamento(FormaPagamento.PIX, new BigDecimal("4.00")));
        venda.adicionarPagamento(new VendaPagamento(FormaPagamento.DINHEIRO, new BigDecimal("6.00")));
        app.getVendas().registrar(venda);

        CaixaService.Resumo resumo = app.getCaixa().resumo(caixa);

        assertEquals(0, new BigDecimal("6.00").compareTo(resumo.porForma().get(FormaPagamento.DINHEIRO)));
        assertEquals(0, new BigDecimal("4.00").compareTo(resumo.porForma().get(FormaPagamento.PIX)));
        assertEquals(0, new BigDecimal("106.00").compareTo(resumo.saldoEsperado()));
    }
}
