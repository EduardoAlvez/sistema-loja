package com.portfolio.sistemaloja.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class ContagemInventario {

    public enum Status {
        ABERTA,
        FECHADA,
        CANCELADA
    }

    private Long id;
    private String numero;
    private Status status = Status.ABERTA;
    private Long usuarioId;
    private LocalDateTime iniciadoEm;
    private LocalDateTime fechadoEm;
    private String observacao;

    public ContagemInventario() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public LocalDateTime getIniciadoEm() {
        return iniciadoEm;
    }

    public void setIniciadoEm(LocalDateTime iniciadoEm) {
        this.iniciadoEm = iniciadoEm;
    }

    public LocalDateTime getFechadoEm() {
        return fechadoEm;
    }

    public void setFechadoEm(LocalDateTime fechadoEm) {
        this.fechadoEm = fechadoEm;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContagemInventario that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
