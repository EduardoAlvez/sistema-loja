package com.portfolio.sistemaloja.model;

import java.math.BigDecimal;

public class CaixaLancamento {

    private Long id;
    private Long caixaId;
    private String dataHora;
    private TipoLancamentoCaixa tipo;
    private BigDecimal valor = BigDecimal.ZERO;
    private Long usuarioId;
    private String usuarioNome;
    private String observacao;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCaixaId() {
        return caixaId;
    }

    public void setCaixaId(Long caixaId) {
        this.caixaId = caixaId;
    }

    public String getDataHora() {
        return dataHora;
    }

    public void setDataHora(String dataHora) {
        this.dataHora = dataHora;
    }

    public TipoLancamentoCaixa getTipo() {
        return tipo;
    }

    public void setTipo(TipoLancamentoCaixa tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor == null ? BigDecimal.ZERO : valor;
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

    public BigDecimal getEfeitoNoSaldo() {
        return tipo == TipoLancamentoCaixa.SANGRIA ? valor.negate() : valor;
    }
}
