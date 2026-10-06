package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.FormaPagamento;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.model.VendaItem;
import com.portfolio.sistemaloja.repository.ProdutoEstoque;
import com.portfolio.sistemaloja.repository.ProdutoVendado;
import com.portfolio.sistemaloja.repository.VendaDoDia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelatorioServiceTest {

    private Aplicacao app;
    private Usuario operador;

    @BeforeEach
    void preparar() {
        app = BancoTeste.criar("relatorio" + System.nanoTime());
        operador = app.getAuth().autenticar("operador", "caixa123");
    }

    @Test
    void resumoDoDiaConsideraVendasConcluidas() {
        RelatorioService.ResumoDashboard antes = app.getRelatorios().resumoDoDia();
        assertEquals(0, antes.vendas());

        registrarVenda("7891000100103", 2, FormaPagamento.PIX);

        RelatorioService.ResumoDashboard depois = app.getRelatorios().resumoDoDia();
        assertEquals(1, depois.vendas());
        assertEquals(0, new BigDecimal("17.98").compareTo(depois.faturamento()));
        assertEquals(0, new BigDecimal("17.98").compareTo(depois.ticketMedio()));
    }

    @Test
    void vendaCanceladaNaoEntraNoFaturamento() {
        Venda venda = registrarVenda("7891000100103", 2, FormaPagamento.PIX);
        app.getVendas().cancelar(venda.getId());

        RelatorioService.ResumoDashboard resumo = app.getRelatorios().resumoDoDia();
        assertEquals(0, resumo.vendas());
        assertEquals(0, BigDecimal.ZERO.compareTo(resumo.faturamento()));
    }

    @Test
    void listaMaisVendidosDoPeriodo() {
        registrarVenda("7891000100104", 4, FormaPagamento.DINHEIRO);
        registrarVenda("7891000100104", 1, FormaPagamento.DINHEIRO);

        LocalDate hoje = LocalDate.now();
        List<ProdutoVendado> maisVendidos = app.getRelatorios().maisVendidos(hoje, hoje);

        assertFalse(maisVendidos.isEmpty());
        assertEquals("Água Mineral 500ml", maisVendidos.get(0).nome());
        assertEquals(5, maisVendidos.get(0).quantidade());
    }

    @Test
    void listaItensComEstoqueCritico() {
        Produto produtoBaixo = new Produto();
        produtoBaixo.setNome("Item Quase Esgotado");
        produtoBaixo.setCodigoBarras("7891111111111");
        produtoBaixo.setPrecoCusto(BigDecimal.ONE);
        produtoBaixo.setPrecoVenda(new BigDecimal("3.00"));
        produtoBaixo.setEstoque(2);
        produtoBaixo.setAtivo(true);
        app.getProdutos().salvar(produtoBaixo);

        List<ProdutoEstoque> baixo = app.getRelatorios().estoqueBaixo();
        assertTrue(baixo.stream().anyMatch(p -> p.nome().equals("Item Quase Esgotado")));
        assertTrue(baixo.stream().noneMatch(p -> p.nome().equals("Refrigerante Cola 2L")));
    }

    @Test
    void faturamentoPorDiaPreencheDiasSemVendaComZero() {
        registrarVenda("7891000100103", 2, FormaPagamento.PIX);

        LocalDate hoje = LocalDate.now();
        List<VendaDoDia> porDia = app.getRelatorios().faturamentoPorDia(hoje.minusDays(2), hoje);

        assertEquals(3, porDia.size(), "um registro por dia do período, mesmo sem venda");
        assertEquals(hoje.minusDays(2), porDia.get(0).dia());
        assertEquals(0, porDia.get(0).quantidade());
        assertEquals(0, BigDecimal.ZERO.compareTo(porDia.get(0).total()));
        assertEquals(hoje, porDia.get(2).dia());
        assertEquals(1, porDia.get(2).quantidade());
        assertEquals(0, new BigDecimal("17.98").compareTo(porDia.get(2).total()));
    }

    @Test
    void faturamentoPorDiaIgnoraVendaCancelada() {
        Venda venda = registrarVenda("7891000100103", 2, FormaPagamento.PIX);
        app.getVendas().cancelar(venda.getId());

        LocalDate hoje = LocalDate.now();
        List<VendaDoDia> porDia = app.getRelatorios().faturamentoPorDia(hoje, hoje);

        assertEquals(1, porDia.size());
        assertEquals(0, porDia.get(0).quantidade());
        assertEquals(0, BigDecimal.ZERO.compareTo(porDia.get(0).total()));
    }

    @Test
    void faturamentoPorDiaSomaComOResumoDoPeriodo() {
        registrarVenda("7891000100103", 2, FormaPagamento.PIX);
        registrarVenda("7891000100104", 5, FormaPagamento.DINHEIRO);

        LocalDate hoje = LocalDate.now();
        List<VendaDoDia> porDia = app.getRelatorios().faturamentoPorDia(hoje.minusDays(1), hoje);
        RelatorioService.ResumoDashboard resumo =
                app.getRelatorios().resumoPeriodo(hoje.minusDays(1), hoje);

        BigDecimal soma = porDia.stream()
                .map(VendaDoDia::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, soma.compareTo(resumo.faturamento()));
        assertEquals(2, porDia.stream().mapToInt(VendaDoDia::quantidade).sum());
    }

    @Test
    void periodoComDataInicialInvalidaEhRejeitado() {
        LocalDate hoje = LocalDate.now();
        assertThrows(ValidacaoException.class,
                () -> app.getRelatorios().resumoPeriodo(hoje, hoje.minusDays(3)));
        assertThrows(ValidacaoException.class,
                () -> app.getRelatorios().vendasDoPeriodo(hoje, hoje.minusDays(1)));
        assertThrows(ValidacaoException.class,
                () -> app.getRelatorios().faturamentoPorDia(hoje, hoje.minusDays(1)));
    }

    private Venda registrarVenda(String codigo, int quantidade, FormaPagamento pagamento) {
        Produto produto = app.getProdutos().buscarPorCodigoBarras(codigo);
        Venda venda = new Venda();
        venda.setUsuarioId(operador.getId());
        venda.setFormaPagamento(pagamento);
        venda.adicionarItem(new VendaItem(produto.getId(), produto.getNome(), quantidade, produto.getPrecoVenda()));
        return app.getVendas().registrar(venda);
    }
}
