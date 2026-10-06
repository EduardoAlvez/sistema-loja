package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.model.Categoria;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.repository.CategoriaRepository;
import com.portfolio.sistemaloja.repository.ProdutoRepository;

import java.math.BigDecimal;
import java.util.List;

public class ProdutoService {

    private final ProdutoRepository produtos;
    private final CategoriaRepository categorias;

    public ProdutoService(ProdutoRepository produtos, CategoriaRepository categorias) {
        this.produtos = produtos;
        this.categorias = categorias;
    }

    public List<Produto> listar(String termo) {
        return produtos.listar(termo);
    }

    public List<Produto> listarAtivos() {
        return produtos.listarAtivos();
    }

    public List<Categoria> listarCategorias() {
        return categorias.listar();
    }

    public Produto buscarPorCodigoBarras(String codigo) {
        return produtos.buscarPorCodigoBarras(codigo)
                .orElseThrow(() -> new ValidacaoException("Produto não encontrado para o código " + codigo + "."));
    }

    public Produto salvar(Produto produto) {
        validar(produto);
        if (produtos.codigoExiste(produto.getCodigoBarras(), produto.getId())) {
            throw new ValidacaoException("Já existe um produto com esse código de barras.");
        }
        return produtos.salvar(produto);
    }

    public void remover(long id) {
        produtos.remover(id);
    }

    public Categoria salvarCategoria(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new ValidacaoException("Nome da categoria é obrigatório.");
        }
        String normalizado = nome.strip();
        return categorias.buscarPorNome(normalizado)
                .orElseGet(() -> categorias.salvar(new Categoria(null, normalizado)));
    }

    private void validar(Produto produto) {
        if (produto.getNome() == null || produto.getNome().isBlank()) {
            throw new ValidacaoException("Nome do produto é obrigatório.");
        }
        if (produto.getCodigoBarras() == null || produto.getCodigoBarras().isBlank()) {
            throw new ValidacaoException("Código de barras é obrigatório.");
        }
        if (produto.getPrecoVenda() == null || produto.getPrecoVenda().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidacaoException("Preço de venda deve ser maior que zero.");
        }
        if (produto.getPrecoCusto() == null || produto.getPrecoCusto().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidacaoException("Preço de custo não pode ser negativo.");
        }
        if (produto.getEstoque() < 0) {
            throw new ValidacaoException("Estoque não pode ser negativo.");
        }
    }
}
