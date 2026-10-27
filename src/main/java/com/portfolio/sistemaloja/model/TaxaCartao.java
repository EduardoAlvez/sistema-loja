package com.portfolio.sistemaloja.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class TaxaCartao {

    private Long id;
    private String bandeira;
    private String tipoCartao;
    private int parcelasMin = 1;
    private int parcelasMax = 1;
    private BigDecimal taxaPercentual = BigDecimal.ZERO;
    private BigDecimal taxaFixa = BigDecimal.ZERO;
    private String adquirente;
    private boolean ativo = true;
    private LocalDateTime criadoEm;

    public TaxaCartao() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBandeira() {
        return bandeira;
    }

    public void setBandeira(String bandeira) {
        this.bandeira = bandeira;
    }

    public String getTipoCartao() {
        return tipoCartao;
    }

    public void setTipoCartao(String tipoCartao) {
        this.tipoCartao = tipoCartao;
    }

    public int getParcelasMin() {
        return parcelasMin;
    }

    public void setParcelasMin(int parcelasMin) {
        this.parcelasMin = parcelasMin;
    }

    public int getParcelasMax() {
        return parcelasMax;
    }

    public void setParcelasMax(int parcelasMax) {
        this.parcelasMax = parcelasMax;
    }

    public BigDecimal getTaxaPercentual() {
        return taxaPercentual == null ? BigDecimal.ZERO : taxaPercentual;
    }

    public void setTaxaPercentual(BigDecimal taxaPercentual) {
        this.taxaPercentual = taxaPercentual;
    }

    public BigDecimal getTaxaFixa() {
        return taxaFixa == null ? BigDecimal.ZERO : taxaFixa;
    }

    public void setTaxaFixa(BigDecimal taxaFixa) {
        this.taxaFixa = taxaFixa;
    }

    public String getAdquirente() {
        return adquirente;
    }

    public void setAdquirente(String adquirente) {
        this.adquirente = adquirente;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TaxaCartao taxaCartao)) {
            return false;
        }
        return Objects.equals(id, taxaCartao.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
