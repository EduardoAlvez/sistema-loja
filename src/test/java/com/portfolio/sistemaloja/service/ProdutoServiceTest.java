package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.Produto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void guardaEstoqueMinimoEApontaEstoqueBaixo() {
        Produto justo = produtoNovo("Leite Integral 1L", "7891111111111", "3.00", "4.99", 4);
        justo.setEstoqueMinimo(4);
        app.getProdutos().salvar(justo);
        Produto folgado = produtoNovo("Queijo Minas 500g", "7891222222222", "18.00", "24.90", 30);
        folgado.setEstoqueMinimo(5);
        app.getProdutos().salvar(folgado);

        Produto lido = app.getProdutos().buscarPorCodigoBarras("7891111111111");
        assertEquals(4, lido.getEstoqueMinimo());
        assertTrue(lido.estoqueBaixo());
        assertFalse(app.getProdutos().buscarPorCodigoBarras("7891222222222").estoqueBaixo());

        List<String> baixos = app.getProdutos().listarComEstoqueBaixo().stream()
                .map(Produto::getNome).toList();
        assertTrue(baixos.contains("Leite Integral 1L"), baixos.toString());
        assertFalse(baixos.contains("Queijo Minas 500g"), baixos.toString());
    }

    @Test
    void filtraProdutosPorCategoria() {
        Long bebidas = app.getProdutos().salvarCategoria("Bebidas").getId();
        Produto suco = produtoNovo("Suco de Uva 1L", "7891333333333", "4.00", "7.50", 10);
        suco.setCategoriaId(bebidas);
        app.getProdutos().salvar(suco);
        app.getProdutos().salvar(produtoNovo("Biscoito Recheado", "7891444444444", "2.00", "3.50", 10));

        List<String> nomes = app.getProdutos().listar("", bebidas).stream()
                .map(Produto::getNome).toList();
        assertTrue(nomes.contains("Suco de Uva 1L"), nomes.toString());
        assertFalse(nomes.contains("Biscoito Recheado"), nomes.toString());

        assertEquals(1, app.getProdutos().listar("Suco", bebidas).size());
        assertEquals(0, app.getProdutos().listar("Biscoito", bebidas).size());
    }

    @Test
    void rejeitaEstoqueMinimoNegativo() {
        Produto produto = produtoNovo("Inválido", "7891555555555", "5.00", "10.00", 10);
        produto.setEstoqueMinimo(-1);
        assertThrows(ValidacaoException.class, () -> app.getProdutos().salvar(produto));
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
