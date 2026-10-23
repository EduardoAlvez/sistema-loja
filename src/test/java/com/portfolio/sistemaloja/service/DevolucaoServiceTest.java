package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.Cliente;
import com.portfolio.sistemaloja.model.Devolucao;
import com.portfolio.sistemaloja.model.DevolucaoItem;
import com.portfolio.sistemaloja.model.FormaPagamento;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.Autorizacao;
import com.portfolio.sistemaloja.model.MotivoAutorizacao;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.model.VendaItem;
import com.portfolio.sistemaloja.repository.EventoAuditoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevolucaoServiceTest {

    private Aplicacao app;
    private Usuario operador;
    private Cliente maria;

    @BeforeEach
    void preparar() {
        app = BancoTeste.criar("devolucao" + System.nanoTime());
        operador = app.getAuth().autenticar("operador", "caixa123");
        maria = app.getClientes().salvar(new Cliente(null, "Maria Souza", "52998224725",
                "11999998888", "maria@exemplo.com", true));
    }

    @Test
    void devolveItensEstornaEstoqueECreditaOCliente() {
        int estoqueInicial = estoqueAgua();
        Venda venda = venda(6, maria.getId());
        Venda registrada = app.getVendas().registrar(venda);
        VendaItem item = registrada.getItens().get(0);

        Devolucao devolucao = app.getDevolucoes().devolver(registrada.getId(),
                List.of(new DevolucaoItem(item.getId(), null, null, 2, null)), true, "cliente reclamou", operador);

        assertEquals(0, new BigDecimal("5.00").compareTo(devolucao.getValor()));
        assertEquals(estoqueInicial - 6 + 2, estoqueAgua());
        assertEquals(2, app.getDevolucoes().jaDevolvido(item.getId()));
        assertEquals(0, new BigDecimal("5.00").compareTo(creditoDe(maria.getId())));
        assertEquals(2, devolucao.getQuantidadeItens());
        assertEquals("Água Mineral 500ml", devolucao.getItens().get(0).getProdutoNome());
        assertEquals(0, new BigDecimal("2.50").compareTo(devolucao.getItens().get(0).getValorUnitario()));
        assertTrue(contemAuditoria("DEVOLUCAO_VENDA"));
    }

    @Test
    void usaOprecoDaVendaMesmoSeOProdutoMudarDePreco() {
        Venda registrada = app.getVendas().registrar(venda(2, maria.getId()));
        VendaItem item = registrada.getItens().get(0);
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        agua.setPrecoVenda(new BigDecimal("9.99"));
        app.getProdutos().salvar(agua);

        Devolucao devolucao = app.getDevolucoes().devolver(registrada.getId(),
                List.of(new DevolucaoItem(item.getId(), null, null, 2, new BigDecimal("99.00"))), false, null, operador);

        assertEquals(0, new BigDecimal("5.00").compareTo(devolucao.getValor()));
    }

    @Test
    void recusaDevolverMaisDoQueVendido() {
        int estoqueInicial = estoqueAgua();
        Venda registrada = app.getVendas().registrar(venda(3, maria.getId()));
        VendaItem item = registrada.getItens().get(0);

        ValidacaoException erro = assertThrows(ValidacaoException.class, () -> app.getDevolucoes().devolver(
                registrada.getId(), List.of(new DevolucaoItem(item.getId(), null, null, 4, null)),
                true, null, operador));

        assertTrue(erro.getMessage().contains("restam 3"), erro.getMessage());
        assertEquals(estoqueInicial - 3, estoqueAgua());
        assertTrue(app.getDevolucoes().listarPorVenda(registrada.getId()).isEmpty());
    }

    @Test
    void consideraDevolucoesAnterioresDoMesmoItem() {
        Venda registrada = app.getVendas().registrar(venda(4, maria.getId()));
        VendaItem item = registrada.getItens().get(0);
        app.getDevolucoes().devolver(registrada.getId(),
                List.of(new DevolucaoItem(item.getId(), null, null, 3, null)), true, null, operador);

        ValidacaoException erro = assertThrows(ValidacaoException.class, () -> app.getDevolucoes().devolver(
                registrada.getId(), List.of(new DevolucaoItem(item.getId(), null, null, 2, null)),
                true, null, operador));

        assertTrue(erro.getMessage().contains("restam 1"), erro.getMessage());
        assertEquals(3, app.getDevolucoes().jaDevolvido(item.getId()));

        Devolucao ultima = app.getDevolucoes().devolver(registrada.getId(),
                List.of(new DevolucaoItem(item.getId(), null, null, 1, null)), true, null, operador);
        assertEquals(0, new BigDecimal("2.50").compareTo(ultima.getValor()));
        assertEquals(4, app.getDevolucoes().jaDevolvido(item.getId()));
    }

    @Test
    void creditoExigeClienteIdentificado() {
        Venda registrada = app.getVendas().registrar(venda(2, null));
        VendaItem item = registrada.getItens().get(0);

        ValidacaoException erro = assertThrows(ValidacaoException.class, () -> app.getDevolucoes().devolver(
                registrada.getId(), List.of(new DevolucaoItem(item.getId(), null, null, 1, null)),
                true, null, operador));

        assertTrue(erro.getMessage().contains("cliente"), erro.getMessage());
        assertTrue(app.getDevolucoes().listarPorVenda(registrada.getId()).isEmpty());
    }

    @Test
    void devolucaoSemCreditoNaoAlteraOCreditoEContaComoRegistrada() {
        Venda registrada = app.getVendas().registrar(venda(2, maria.getId()));
        VendaItem item = registrada.getItens().get(0);

        Devolucao devolucao = app.getDevolucoes().devolver(registrada.getId(),
                List.of(new DevolucaoItem(item.getId(), null, null, 1, null)), false, null, operador);

        assertEquals(0, BigDecimal.ZERO.compareTo(creditoDe(maria.getId())));
        assertEquals(0, new BigDecimal("2.50").compareTo(devolucao.getValor()));
    }

    @Test
    void recusaItemQueNaoPertenceAVenda() {
        Venda primeira = app.getVendas().registrar(venda(2, maria.getId()));
        Venda segunda = app.getVendas().registrar(venda(2, maria.getId()));
        VendaItem itemDeOutraVenda = segunda.getItens().get(0);

        ValidacaoException erro = assertThrows(ValidacaoException.class, () -> app.getDevolucoes().devolver(
                primeira.getId(), List.of(new DevolucaoItem(itemDeOutraVenda.getId(), null, null, 1, null)),
                true, null, operador));

        assertTrue(erro.getMessage().contains("não pertence"), erro.getMessage());
    }

    @Test
    void recusaDevolucaoDeVendaCancelada() {
        Venda registrada = app.getVendas().registrar(venda(2, maria.getId()));
        VendaItem item = registrada.getItens().get(0);
        app.getVendas().cancelar(registrada.getId(),
                BancoTeste.autorizarSupervisor(app, MotivoAutorizacao.CANCELAMENTO_VENDA));

        ValidacaoException erro = assertThrows(ValidacaoException.class, () -> app.getDevolucoes().devolver(
                registrada.getId(), List.of(new DevolucaoItem(item.getId(), null, null, 1, null)),
                true, null, operador));

        assertTrue(erro.getMessage().contains("concluídas"), erro.getMessage());
    }

    @Test
    void recusaDevolucaoSemItensEQuantidadeZerada() {
        Venda registrada = app.getVendas().registrar(venda(2, maria.getId()));
        VendaItem item = registrada.getItens().get(0);

        assertThrows(ValidacaoException.class, () -> app.getDevolucoes().devolver(registrada.getId(),
                List.of(), true, null, operador));
        assertThrows(ValidacaoException.class, () -> app.getDevolucoes().devolver(registrada.getId(),
                List.of(new DevolucaoItem(item.getId(), null, null, 0, null)), true, null, operador));
    }

    @Test
    void encontraVendaPeloNumeroDoCupom() {
        Venda registrada = app.getVendas().registrar(venda(1, maria.getId()));

        assertEquals(registrada.getId(),
                app.getDevolucoes().buscarVendaPorNumero(registrada.getNumero().toLowerCase()).orElseThrow().getId());
        assertTrue(app.getDevolucoes().buscarVendaPorNumero("V999999").isEmpty());
        assertTrue(app.getDevolucoes().buscarVendaPorNumero("  ").isEmpty());
    }

    private Venda venda(int quantidade, Long clienteId) {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        Venda venda = new Venda();
        venda.setUsuarioId(operador.getId());
        venda.setClienteId(clienteId);
        venda.setFormaPagamento(FormaPagamento.DINHEIRO);
        venda.setValorPago(agua.getPrecoVenda().multiply(BigDecimal.valueOf(quantidade)));
        venda.adicionarItem(new VendaItem(agua.getId(), agua.getNome(), quantidade, agua.getPrecoVenda()));
        return venda;
    }

    private int estoqueAgua() {
        return app.getProdutos().buscarPorCodigoBarras("7891000100104").getEstoque();
    }

    private BigDecimal creditoDe(Long clienteId) {
        return app.getClientes().listar("Maria").stream()
                .filter(c -> c.getId().equals(clienteId))
                .findFirst().orElseThrow().getCredito();
    }

    private boolean contemAuditoria(String acao) {
        List<EventoAuditoria> eventos = app.getAuditoria().listar(LocalDate.now(), LocalDate.now());
        return eventos.stream().anyMatch(evento -> acao.equals(evento.acao()));
    }
}
