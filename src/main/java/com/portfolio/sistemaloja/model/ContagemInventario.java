package com.portfolio.sistemaloja.model;

import java.time.LocalDateTime;

public class ContagemInventario {
    private long id;
    private String numero;
    private StatusContagem status = StatusContagem.ABERTA;
    private Long usuarioId;
    private String usuarioNome;
    private String observacao;
    private LocalDateTime dataHora;
    private LocalDateTime dataFechamento;

    public ContagemInventario() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public StatusContagem getStatus() {
        return status;
    }

    public void setStatus(StatusContagem status) {
        this.status = status;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getUsuarioNome() {
        return usuarioNome;
    }

    public void setUsuarioNome(String usuarioNome) {
        this.usuarioNome = usuarioNome;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public LocalDateTime getDataFechamento() {
        return dataFechamento;
    }

    public void setDataFechamento(LocalDateTime dataFechamento) {
        this.dataFechamento = dataFechamento;
    }

    public boolean estaAberta() {
        return status == StatusContagem.ABERTA;
    }
}
