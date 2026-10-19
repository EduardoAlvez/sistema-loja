package com.portfolio.sistemaloja.model;

import java.math.BigDecimal;

public class VendaItem {

    private Long id;
    private Long produtoId;
    private String produtoNome;
    private int quantidade;
    private BigDecimal precoUnitario;
    private BigDecimal desconto = BigDecimal.ZERO;
    private BigDecimal subtotal = BigDecimal.ZERO;

    public VendaItem() {
    }

    public VendaItem(Long produtoId, String produtoNome, int quantidade, BigDecimal precoUnitario) {
        this.produtoId = produtoId;
        this.produtoNome = produtoNome;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
        this.subtotal = precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProdutoId() {
        return produtoId;
    }

    public void setProdutoId(Long produtoId) {
        this.produtoId = produtoId;
    }

    public String getProdutoNome() {
        return produtoNome;
    }

    public void setProdutoNome(String produtoNome) {
        this.produtoNome = produtoNome;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
        recalcularSubtotal();
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = precoUnitario;
        recalcularSubtotal();
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getDesconto() {
        return desconto;
    }

    public void setDesconto(BigDecimal desconto) {
        this.desconto = desconto == null ? BigDecimal.ZERO : desconto;
        recalcularSubtotal();
    }

    public BigDecimal getValorBruto() {
        if (precoUnitario == null) {
            return BigDecimal.ZERO;
        }
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    private void recalcularSubtotal() {
        if (precoUnitario != null) {
            subtotal = getValorBruto().subtract(desconto == null ? BigDecimal.ZERO : desconto);
        }
    }
}
