package com.portfolio.sistemaloja.model;

import java.math.BigDecimal;

public class Caixa {

    private Long id;
    private String dataAbertura;
    private String dataFechamento;
    private Long usuarioAberturaId;
    private String usuarioAberturaNome;
    private Long usuarioFechamentoId;
    private String usuarioFechamentoNome;
    private BigDecimal valorAbertura = BigDecimal.ZERO;
    private BigDecimal valorFechamento;
    private StatusCaixa status = StatusCaixa.ABERTO;
    private String observacao;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDataAbertura() {
        return dataAbertura;
    }

    public void setDataAbertura(String dataAbertura) {
        this.dataAbertura = dataAbertura;
    }

    public String getDataFechamento() {
        return dataFechamento;
    }

    public void setDataFechamento(String dataFechamento) {
        this.dataFechamento = dataFechamento;
    }

    public Long getUsuarioAberturaId() {
        return usuarioAberturaId;
    }

    public void setUsuarioAberturaId(Long usuarioAberturaId) {
        this.usuarioAberturaId = usuarioAberturaId;
    }

    public String getUsuarioAberturaNome() {
        return usuarioAberturaNome;
    }

    public void setUsuarioAberturaNome(String usuarioAberturaNome) {
        this.usuarioAberturaNome = usuarioAberturaNome;
    }

    public Long getUsuarioFechamentoId() {
        return usuarioFechamentoId;
    }

    public void setUsuarioFechamentoId(Long usuarioFechamentoId) {
        this.usuarioFechamentoId = usuarioFechamentoId;
    }

    public String getUsuarioFechamentoNome() {
        return usuarioFechamentoNome;
    }

    public void setUsuarioFechamentoNome(String usuarioFechamentoNome) {
        this.usuarioFechamentoNome = usuarioFechamentoNome;
    }

    public BigDecimal getValorAbertura() {
        return valorAbertura;
    }

    public void setValorAbertura(BigDecimal valorAbertura) {
        this.valorAbertura = valorAbertura == null ? BigDecimal.ZERO : valorAbertura;
    }

    public BigDecimal getValorFechamento() {
        return valorFechamento;
    }

    public void setValorFechamento(BigDecimal valorFechamento) {
        this.valorFechamento = valorFechamento;
    }

    public StatusCaixa getStatus() {
        return status;
    }

    public void setStatus(StatusCaixa status) {
        this.status = status;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public boolean isAberto() {
        return status == StatusCaixa.ABERTO;
    }
}
