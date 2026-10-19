package com.portfolio.sistemaloja.model;

import java.math.BigDecimal;

public class DevolucaoItem {

    private Long id;
    private Long vendaItemId;
    private Long produtoId;
    private String produtoNome;
    private int quantidade;
    private BigDecimal valorUnitario = BigDecimal.ZERO;

    public DevolucaoItem() {
    }

    public DevolucaoItem(Long vendaItemId, Long produtoId, String produtoNome, int quantidade,
                         BigDecimal valorUnitario) {
        this.vendaItemId = vendaItemId;
        this.produtoId = produtoId;
        this.produtoNome = produtoNome;
        this.quantidade = quantidade;
        this.valorUnitario = valorUnitario;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVendaItemId() {
        return vendaItemId;
    }

    public void setVendaItemId(Long vendaItemId) {
        this.vendaItemId = vendaItemId;
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
    }

    public BigDecimal getValorUnitario() {
        return valorUnitario;
    }

    public void setValorUnitario(BigDecimal valorUnitario) {
        this.valorUnitario = valorUnitario;
    }

    public BigDecimal getSubtotal() {
        return valorUnitario.multiply(BigDecimal.valueOf(quantidade));
    }
}
