package com.portfolio.sistemaloja.tef;

import java.math.BigDecimal;

/**
 * Dados da transação enviada à operadora de cartão.
 */
public class TransacaoCartao {

    private String numeroVenda;
    private BigDecimal valor;
    private String bandeira;
    private TipoCartao tipoCartao;
    private int parcelas = 1;
    private String adquirente;

    public TransacaoCartao() {
    }

    public TransacaoCartao(String numeroVenda, BigDecimal valor, String bandeira, TipoCartao tipoCartao,
                           int parcelas, String adquirente) {
        this.numeroVenda = numeroVenda;
        this.valor = valor;
        this.bandeira = bandeira;
        this.tipoCartao = tipoCartao;
        this.parcelas = parcelas;
        this.adquirente = adquirente;
    }

    public String getNumeroVenda() {
        return numeroVenda;
    }

    public void setNumeroVenda(String numeroVenda) {
        this.numeroVenda = numeroVenda;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getBandeira() {
        return bandeira;
    }

    public void setBandeira(String bandeira) {
        this.bandeira = bandeira;
    }

    public TipoCartao getTipoCartao() {
        return tipoCartao;
    }

    public void setTipoCartao(TipoCartao tipoCartao) {
        this.tipoCartao = tipoCartao;
    }

    public int getParcelas() {
        return parcelas;
    }

    public void setParcelas(int parcelas) {
        this.parcelas = parcelas;
    }

    public String getAdquirente() {
        return adquirente;
    }

    public void setAdquirente(String adquirente) {
        this.adquirente = adquirente;
    }

    public enum TipoCartao {
        DEBITO,
        CREDITO
    }
}
