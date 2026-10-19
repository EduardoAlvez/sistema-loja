package com.portfolio.sistemaloja.model;

import java.math.BigDecimal;

public class VendaPagamento {

    private Long id;
    private Long vendaId;
    private FormaPagamento formaPagamento;
    private BigDecimal valor = BigDecimal.ZERO;

    public VendaPagamento() {
    }

    public VendaPagamento(FormaPagamento formaPagamento, BigDecimal valor) {
        this.formaPagamento = formaPagamento;
        this.valor = valor == null ? BigDecimal.ZERO : valor;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVendaId() {
        return vendaId;
    }

    public void setVendaId(Long vendaId) {
        this.vendaId = vendaId;
    }

    public FormaPagamento getFormaPagamento() {
        return formaPagamento;
    }

    public void setFormaPagamento(FormaPagamento formaPagamento) {
        this.formaPagamento = formaPagamento;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor == null ? BigDecimal.ZERO : valor;
    }
}
