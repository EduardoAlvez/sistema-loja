package com.portfolio.sistemaloja.model;

import java.math.BigDecimal;
import java.util.Objects;

public class Produto {

    private Long id;
    private String codigoBarras;
    private String nome;
    private Long categoriaId;
    private String categoriaNome;
    private BigDecimal precoCusto;
    private BigDecimal precoVenda;
    private int estoque;
    private boolean ativo;

    public Produto() {
    }

    public Produto(Long id, String codigoBarras, String nome, Long categoriaId,
                   BigDecimal precoCusto, BigDecimal precoVenda, int estoque, boolean ativo) {
        this.id = id;
        this.codigoBarras = codigoBarras;
        this.nome = nome;
        this.categoriaId = categoriaId;
        this.precoCusto = precoCusto;
        this.precoVenda = precoVenda;
        this.estoque = estoque;
        this.ativo = ativo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigoBarras() {
        return codigoBarras;
    }

    public void setCodigoBarras(String codigoBarras) {
        this.codigoBarras = codigoBarras;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Long getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Long categoriaId) {
        this.categoriaId = categoriaId;
    }

    public String getCategoriaNome() {
        return categoriaNome;
    }

    public void setCategoriaNome(String categoriaNome) {
        this.categoriaNome = categoriaNome;
    }

    public BigDecimal getPrecoCusto() {
        return precoCusto;
    }

    public void setPrecoCusto(BigDecimal precoCusto) {
        this.precoCusto = precoCusto;
    }

    public BigDecimal getPrecoVenda() {
        return precoVenda;
    }

    public void setPrecoVenda(BigDecimal precoVenda) {
        this.precoVenda = precoVenda;
    }

    public int getEstoque() {
        return estoque;
    }

    public void setEstoque(int estoque) {
        this.estoque = estoque;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public BigDecimal calcularLucro() {
        if (precoCusto == null || precoVenda == null) {
            return BigDecimal.ZERO;
        }
        return precoVenda.subtract(precoCusto);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Produto produto)) {
            return false;
        }
        return Objects.equals(id, produto.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return nome;
    }
}
