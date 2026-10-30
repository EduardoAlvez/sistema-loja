package com.portfolio.sistemaloja.model;

import java.util.Objects;

public class ContagemItem {

    private Long id;
    private Long contagemId;
    private Long produtoId;
    private int estoqueSistema;
    private int quantidadeContada;
    private int diferenca;
    private boolean ajustado;

    public ContagemItem() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getContagemId() {
        return contagemId;
    }

    public void setContagemId(Long contagemId) {
        this.contagemId = contagemId;
    }

    public Long getProdutoId() {
        return produtoId;
    }

    public void setProdutoId(Long produtoId) {
        this.produtoId = produtoId;
    }

    public int getEstoqueSistema() {
        return estoqueSistema;
    }

    public void setEstoqueSistema(int estoqueSistema) {
        this.estoqueSistema = estoqueSistema;
    }

    public int getQuantidadeContada() {
        return quantidadeContada;
    }

    public void setQuantidadeContada(int quantidadeContada) {
        this.quantidadeContada = quantidadeContada;
    }

    public int getDiferenca() {
        return diferenca;
    }

    public void setDiferenca(int diferenca) {
        this.diferenca = diferenca;
    }

    public boolean isAjustado() {
        return ajustado;
    }

    public void setAjustado(boolean ajustado) {
        this.ajustado = ajustado;
    }

    public void calcularDiferenca() {
        this.diferenca = this.quantidadeContada - this.estoqueSistema;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContagemItem that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
