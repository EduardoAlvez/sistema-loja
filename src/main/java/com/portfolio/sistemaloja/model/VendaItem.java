package com.portfolio.sistemaloja.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class VendaItem {

    private Long id;
    private Long produtoId;
    private String produtoNome;
    private int quantidade;
    private BigDecimal precoUnitario;
    private BigDecimal desconto = BigDecimal.ZERO;
    private BigDecimal descontoPromocao = BigDecimal.ZERO;
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

    /** Parcela do desconto que vem de promoção vigente; não é persistida. */
    public BigDecimal getDescontoPromocao() {
        return descontoPromocao;
    }

    public void setDescontoPromocao(BigDecimal descontoPromocao) {
        this.descontoPromocao = descontoPromocao == null ? BigDecimal.ZERO : descontoPromocao;
    }

    /** Parte do desconto digitada pelo operador, sem a promoção (pode ser negativa na entrada bruta). */
    public BigDecimal getDescontoManual() {
        return desconto.subtract(descontoPromocao);
    }

    /**
     * Aplica preço de tabela e promove o ganho da promoção para o desconto do item
     * (issue #46), preservando a parcela manual já digitada. O subtotal cobrado
     * permanece o preço vigente menos o desconto manual.
     */
    public void recalcularPrecos(BigDecimal precoTabela, BigDecimal precoVigente) {
        BigDecimal tabela = precoTabela == null ? BigDecimal.ZERO : precoTabela;
        BigDecimal vigente = precoVigente == null ? tabela : precoVigente;
        BigDecimal bruto = tabela.multiply(BigDecimal.valueOf(quantidade));
        BigDecimal pago = vigente.multiply(BigDecimal.valueOf(quantidade));
        BigDecimal residual = bruto.subtract(pago);
        if (residual.compareTo(BigDecimal.ZERO) < 0) {
            residual = BigDecimal.ZERO;
        }
        BigDecimal manual = getDescontoManual();
        this.precoUnitario = tabela;
        this.descontoPromocao = residual;
        this.desconto = residual.add(manual);
        recalcularSubtotal();
    }

    /** Valor efetivamente pago por unidade (subtotal rateado), para devoluções. */
    public BigDecimal getUnitarioPago() {
        if (quantidade <= 0) {
            return subtotal == null ? BigDecimal.ZERO : subtotal;
        }
        return subtotal.divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.HALF_UP);
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
