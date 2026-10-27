package com.portfolio.sistemaloja.model;

import java.math.BigDecimal;
import java.util.Objects;

public class EntradaItem {

    private Long id;
    private Long entradaId;
    private Long produtoId;
    private String produtoNome;
    private int quantidade;
    private BigDecimal valorUnitario;
    private BigDecimal subtotal;

    public EntradaItem() {
    }

    public EntradaItem(Long id, Long entradaId, Long produtoId, String produtoNome, int quantidade,
                       BigDecimal valorUnitario, BigDecimal subtotal) {
        this.id = id;
        this.entradaId = entradaId;
        this.produtoId = produtoId;
        this.produtoNome = produtoNome;
        this.quantidade = quantidade;
        this.valorUnitario = valorUnitario;
        this.subtotal = subtotal;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEntradaId() {
        return entradaId;
    }

    public void setEntradaId(Long entradaId) {
        this.entradaId = entradaId;
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
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public void recalcularSubtotal() {
        if (valorUnitario == null) {
            valorUnitario = BigDecimal.ZERO;
        }
        subtotal = valorUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EntradaItem entradaItem)) {
            return false;
        }
        return Objects.equals(id, entradaItem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
