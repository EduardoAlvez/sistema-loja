package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.Produto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProdutoServiceTest {

    private Aplicacao app;

    @BeforeEach
    void preparar() {
        app = BancoTeste.criar("produto" + System.nanoTime());
    }

    @Test
    void salvaEBuscaProdutoPorCodigoDeBarras() {
        Produto produto = produtoNovo("Café Torrado 500g", "7899999999999", "12.50", "18.90", 20);
        Produto salvo = app.getProdutos().salvar(produto);

        assertNotNull(salvo.getId());
        assertEquals("7899999999999",
                app.getProdutos().buscarPorCodigoBarras("7899999999999").getCodigoBarras());
        assertEquals(0, new BigDecimal("6.40").compareTo(salvo.calcularLucro()));
    }

    @Test
    void rejeitaPrecoDeVendaZerado() {
        Produto produto = produtoNovo("Inválido", "7898888888888", "5.00", "0", 10);
        assertThrows(ValidacaoException.class, () -> app.getProdutos().salvar(produto));
    }

    @Test
    void rejeitaEstoqueNegativo() {
        Produto produto = produtoNovo("Inválido", "7897777777777", "5.00", "10.00", -1);
        assertThrows(ValidacaoException.class, () -> app.getProdutos().salvar(produto));
    }

    @Test
    void rejeitaNomeVazioECodigoVazio() {
        assertThrows(ValidacaoException.class,
                () -> app.getProdutos().salvar(produtoNovo("", "7896666666666", "5.00", "10.00", 1)));
        assertThrows(ValidacaoException.class,
                () -> app.getProdutos().salvar(produtoNovo("Sem código", " ", "5.00", "10.00", 1)));
    }

    @Test
    void naoPermiteCodigoDeBarrasDuplicado() {
        app.getProdutos().salvar(produtoNovo("Primeiro", "7895555555555", "5.00", "10.00", 1));
        Produto segundo = produtoNovo("Segundo", "7895555555555", "5.00", "10.00", 1);
        assertThrows(ValidacaoException.class, () -> app.getProdutos().salvar(segundo));
    }

    @Test
    void atualizaProdutoSemPerderIdentidade() {
        Produto salvo = app.getProdutos().salvar(produtoNovo("Original", "7894444444444", "5.00", "10.00", 1));
        salvo.setNome("Atualizado");
        salvo.setEstoque(55);
        app.getProdutos().salvar(salvo);

        Produto lido = app.getProdutos().buscarPorCodigoBarras("7894444444444");
        assertEquals("Atualizado", lido.getNome());
        assertEquals(55, lido.getEstoque());
    }

    @Test
    void filtraProdutosPorNome() {
        app.getProdutos().salvar(produtoNovo("Arroz Integral", "7893333333333", "5.00", "10.00", 1));
        app.getProdutos().salvar(produtoNovo("Feijão Preto", "7892222222222", "5.00", "10.00", 1));

        assertEquals(1, app.getProdutos().listar("Integral").size());
        assertEquals(0, app.getProdutos().listar("naoexiste").size());
    }

    private Produto produtoNovo(String nome, String codigo, String custo, String venda, int estoque) {
        Produto produto = new Produto();
        produto.setNome(nome);
        produto.setCodigoBarras(codigo);
        produto.setPrecoCusto(new BigDecimal(custo));
        produto.setPrecoVenda(new BigDecimal(venda));
        produto.setEstoque(estoque);
        produto.setAtivo(true);
        return produto;
    }
}
