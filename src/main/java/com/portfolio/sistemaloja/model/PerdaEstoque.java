package com.portfolio.sistemaloja.model;

import java.time.LocalDateTime;

public class PerdaEstoque {
    private long id;
    private long produtoId;
    private int quantidade;
    private String motivo;
    private String observacao;
    private Long usuarioId;
    private LocalDateTime dataHora;

    public PerdaEstoque() {
    }

    public PerdaEstoque(long produtoId, int quantidade, String motivo, String observacao, Long usuarioId) {
        this.produtoId = produtoId;
        this.quantidade = quantidade;
        this.motivo = motivo;
        this.observacao = observacao;
        this.usuarioId = usuarioId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getProdutoId() {
        return produtoId;
    }

    public void setProdutoId(long produtoId) {
        this.produtoId = produtoId;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }
}
