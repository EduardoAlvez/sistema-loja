package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.Cliente;
import com.portfolio.sistemaloja.model.DevolucaoItem;
import com.portfolio.sistemaloja.model.ExtratoFidelidade;
import com.portfolio.sistemaloja.model.FormaPagamento;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.TipoMovimentoFidelidade;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.model.VendaItem;
import com.portfolio.sistemaloja.model.VendaPausada;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FidelidadeEstornoTest {

    private Aplicacao app;
    private Usuario operador;
    private Usuario admin;
    private Cliente cliente;
    private Produto agua;

    @BeforeEach
    void preparar() {
        app = BancoTeste.criar("estorno" + System.nanoTime());
        operador = app.getAuth().autenticar("operador", "caixa123");
        admin = app.getAuth().autenticar("admin", "admin123");
        cliente = app.getClientes().salvar(new Cliente(null, "Maria Souza", "52998224725", "11999998888",
                "maria@exemplo.com", true));
        agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
    }

    @Test
    void devolucaoParcialEstornaOsPontosProporcionalmente() {
        Venda registrada = app.getVendas().registrar(venda(admin, 4, BigDecimal.ZERO, 0));
        assertEquals(10, app.getFidelidade().saldo(cliente.getId()));

        app.getDevolucoes().devolver(registrada.getId(), List.of(itemDevolvido(registrada, 1)), false, null, operador);

        assertEquals(8, app.getFidelidade().saldo(cliente.getId()));
    }

    @Test
    void devolucaoNaoEstornaMaisQueOsPontosCreditados() {
        Venda registrada = app.getVendas().registrar(venda(admin, 4, new BigDecimal("5.00"), 0));
        assertEquals(5, app.getFidelidade().saldo(cliente.getId()));

        app.getDevolucoes().devolver(registrada.getId(), List.of(itemDevolvido(registrada, 1)), false, null, operador);
        assertEquals(3, app.getFidelidade().saldo(cliente.getId()));

        app.getDevolucoes().devolver(registrada.getId(), List.of(itemDevolvido(registrada, 1)), false, null, operador);
        assertEquals(1, app.getFidelidade().saldo(cliente.getId()));

        app.getDevolucoes().devolver(registrada.getId(), List.of(itemDevolvido(registrada, 1)), false, null, operador);
        assertEquals(0, app.getFidelidade().saldo(cliente.getId()));
    }

    @Test
    void cancelamentoEstornaOCreditoEDevolveOsPontosResgatados() {
        creditar(100);
        Venda registrada = app.getVendas().registrar(venda(admin, 4, BigDecimal.ZERO, 20));
        assertEquals(100 - 20 + 9, app.getFidelidade().saldo(cliente.getId()));

        app.getVendas().cancelar(registrada.getId());

        assertEquals(100, app.getFidelidade().saldo(cliente.getId()));
        List<ExtratoFidelidade> extrato = app.getFidelidade().extrato(cliente.getId());
        assertEquals(TipoMovimentoFidelidade.ESTORNO_DEBITO, extrato.get(0).tipo());
        assertEquals(20, extrato.get(0).pontos());
        assertEquals(TipoMovimentoFidelidade.ESTORNO_CREDITO, extrato.get(1).tipo());
        assertEquals(9, extrato.get(1).pontos());
    }

    @Test
    void cancelamentoRepetidoNaoEstornaEmDobro() {
        Venda registrada = app.getVendas().registrar(venda(admin, 4, BigDecimal.ZERO, 0));
        assertEquals(10, app.getFidelidade().saldo(cliente.getId()));

        app.getVendas().cancelar(registrada.getId());
        app.getVendas().cancelar(registrada.getId());

        assertEquals(0, app.getFidelidade().saldo(cliente.getId()));
    }

    @Test
    void pausaPreservaOsPontosResgatados() {
        Venda venda = venda(admin, 4, BigDecimal.ZERO, 20);
        venda.setDesconto(new BigDecimal("1.00"));
        venda.setValorPontos(new BigDecimal("1.00"));

        VendaPausada pausada = app.getPausas().pausar(venda, "volto depois", admin);

        assertEquals(20, pausada.getPontosResgatados());
        assertEquals(0, new BigDecimal("1.00").compareTo(pausada.getValorPontos()));
        assertEquals(20, pausada.paraVenda().getPontosResgatados());
    }

    private DevolucaoItem itemDevolvido(Venda venda, int quantidade) {
        VendaItem item = venda.getItens().get(0);
        return new DevolucaoItem(item.getId(), null, null, quantidade, null);
    }

    private void creditar(int pontos) {
        app.getFidelidadeRepository().aplicarMovimentos(List.of(
                app.getFidelidade().credito(cliente.getId(), null, pontos, "Credito de teste")));
    }

    private Venda venda(Usuario usuario, int quantidade, BigDecimal desconto, int pontosResgatados) {
        Venda venda = new Venda();
        venda.setUsuarioId(usuario.getId());
        venda.setClienteId(cliente.getId());
        venda.setFormaPagamento(FormaPagamento.PIX);
        venda.setDesconto(desconto);
        venda.setPontosResgatados(pontosResgatados);
        venda.adicionarItem(new VendaItem(agua.getId(), agua.getNome(), quantidade, agua.getPrecoVenda()));
        return venda;
    }
}
