package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.Cliente;
import com.portfolio.sistemaloja.model.ExtratoFidelidade;
import com.portfolio.sistemaloja.model.FormaPagamento;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.TipoMovimentoFidelidade;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.model.VendaItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VendaFidelidadeTest {

    private Aplicacao app;
    private Usuario operador;
    private Usuario admin;
    private Cliente cliente;
    private Produto agua;

    @BeforeEach
    void preparar() {
        app = BancoTeste.criar("vendafidelidade" + System.nanoTime());
        operador = app.getAuth().autenticar("operador", "caixa123");
        admin = app.getAuth().autenticar("admin", "admin123");
        cliente = app.getClientes().salvar(new Cliente(null, "Maria Souza", "52998224725", "11999998888",
                "maria@exemplo.com", true));
        agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
    }

    @Test
    void vendaComClienteCreditaOsPontosPeloTotal() {
        Venda registrada = app.getVendas().registrar(venda(operador, 4, 0));

        assertEquals(10, registrada.getPontosCreditados());
        assertEquals(0, new BigDecimal("10.00").compareTo(registrada.getTotal()));
        assertEquals(10, app.getFidelidade().saldo(cliente.getId()));
    }

    @Test
    void vendaSemClienteNaoCreditaPontos() {
        Venda venda = new Venda();
        venda.setUsuarioId(operador.getId());
        venda.setFormaPagamento(FormaPagamento.PIX);
        venda.adicionarItem(new VendaItem(agua.getId(), agua.getNome(), 4, agua.getPrecoVenda()));

        Venda registrada = app.getVendas().registrar(venda);

        assertEquals(0, registrada.getPontosCreditados());
    }

    @Test
    void resgateDescontaDaVendaEDebitaDoSaldo() {
        creditar(100);

        Venda registrada = app.getVendas().registrar(venda(admin, 4, 20));

        assertEquals(20, registrada.getPontosResgatados());
        assertEquals(0, new BigDecimal("1.00").compareTo(registrada.getValorPontos()));
        assertEquals(0, new BigDecimal("0.00").compareTo(registrada.getDesconto()));
        assertEquals(0, new BigDecimal("9.00").compareTo(registrada.getTotal()));
        assertEquals(100 - 20 + 9, app.getFidelidade().saldo(cliente.getId()));
    }

    @Test
    void extratoMostraOCreditoEOResgateDaVenda() {
        creditar(100);

        Venda registrada = app.getVendas().registrar(venda(admin, 4, 20));

        List<ExtratoFidelidade> extrato = app.getFidelidade().extrato(cliente.getId());
        assertEquals(3, extrato.size());
        assertEquals(TipoMovimentoFidelidade.CREDITO, extrato.get(0).tipo());
        assertEquals(9, extrato.get(0).pontos());
        assertTrue(extrato.get(0).descricao().contains(registrada.getNumero()));
        assertEquals(TipoMovimentoFidelidade.DEBITO, extrato.get(1).tipo());
        assertEquals(20, extrato.get(1).pontos());
        assertEquals(registrada.getId(), extrato.get(1).vendaId());
    }

    @Test
    void resgateAcimaDoSaldoEhRecusado() {
        creditar(10);

        ValidacaoException erro = assertThrows(ValidacaoException.class,
                () -> app.getVendas().registrar(venda(admin, 4, 20)));

        assertTrue(erro.getMessage().contains("Saldo de pontos insuficiente"));
        assertEquals(10, app.getFidelidade().saldo(cliente.getId()));
    }

    @Test
    void resgateSemClienteEhRecusado() {
        Venda venda = new Venda();
        venda.setUsuarioId(admin.getId());
        venda.setFormaPagamento(FormaPagamento.PIX);
        venda.adicionarItem(new VendaItem(agua.getId(), agua.getNome(), 4, agua.getPrecoVenda()));
        venda.setPontosResgatados(10);

        assertThrows(ValidacaoException.class, () -> app.getVendas().registrar(venda));
    }

    @Test
    void resgateNaoPodeZerarOTotal() {
        creditar(100);

        assertThrows(ValidacaoException.class, () -> app.getVendas().registrar(venda(admin, 1, 50)));
    }

    @Test
    void resgateRespeitaOLimiteDeDescontoDoPerfil() {
        creditar(100);

        ValidacaoException erro = assertThrows(ValidacaoException.class,
                () -> app.getVendas().registrar(venda(operador, 4, 40)));

        assertTrue(erro.getMessage().contains("excede o limite"));
        assertEquals(100, app.getFidelidade().saldo(cliente.getId()));
    }

    @Test
    void resgateQueNaoGeraDescontoEhRecusado() {
        System.setProperty("fidelidade.valorPorPonto", "0.001");
        try {
            Aplicacao local = BancoTeste.criar("pontosinficientes" + System.nanoTime());
            local.getConfiguracoes().invalidar();
            local.getConfiguracoes().atual();
            Usuario adminLocal = local.getAuth().autenticar("admin", "admin123");
            Cliente clienteLocal = local.getClientes().salvar(new Cliente(null, "Joao Lima", "52998224725",
                    "11988887777", "joao@exemplo.com", true));
            local.getFidelidadeRepository().aplicarMovimentos(List.of(
                    local.getFidelidade().credito(clienteLocal.getId(), null, 10, "Credito de teste")));
            Venda venda = new Venda();
            venda.setUsuarioId(adminLocal.getId());
            venda.setClienteId(clienteLocal.getId());
            venda.setFormaPagamento(FormaPagamento.PIX);
            venda.setPontosResgatados(1);
            venda.adicionarItem(new VendaItem(agua.getId(), agua.getNome(), 1, agua.getPrecoVenda()));

            ValidacaoException erro = assertThrows(ValidacaoException.class,
                    () -> local.getVendas().registrar(venda));

            assertTrue(erro.getMessage().contains("não gera desconto"), erro.getMessage());
            assertEquals(10, local.getFidelidade().saldo(clienteLocal.getId()));
        } finally {
            System.clearProperty("fidelidade.valorPorPonto");
        }
    }

    private void creditar(int pontos) {
        app.getFidelidadeRepository().aplicarMovimentos(List.of(
                app.getFidelidade().credito(cliente.getId(), null, pontos, "Credito de teste")));
    }

    private Venda venda(Usuario usuario, int quantidade, int pontosResgatados) {
        Venda venda = new Venda();
        venda.setUsuarioId(usuario.getId());
        venda.setClienteId(cliente.getId());
        venda.setFormaPagamento(FormaPagamento.PIX);
        venda.setPontosResgatados(pontosResgatados);
        venda.adicionarItem(new VendaItem(agua.getId(), agua.getNome(), quantidade, agua.getPrecoVenda()));
        return venda;
    }
}
