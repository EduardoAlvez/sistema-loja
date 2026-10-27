package com.portfolio.sistemaloja.tef;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RespostaTransacao {

    private StatusTransacao status;
    private boolean aprovado;
    private String nsu;
    private String codigoAutorizacao;
    private String mensagem;
    private String bandeira;
    private String adquirente;
    private BigDecimal valor;
    private LocalDateTime dataHora;

    public RespostaTransacao() {
    }

    public static RespostaTransacao aprovada(String nsu, String codigoAutorizacao, BigDecimal valor) {
        RespostaTransacao resposta = new RespostaTransacao();
        resposta.status = StatusTransacao.APROVADA;
        resposta.aprovado = true;
        resposta.nsu = nsu;
        resposta.codigoAutorizacao = codigoAutorizacao;
        resposta.valor = valor;
        resposta.mensagem = "Transação autorizada";
        resposta.dataHora = LocalDateTime.now();
        return resposta;
    }

    public static RespostaTransacao negada(String mensagem) {
        RespostaTransacao resposta = new RespostaTransacao();
        resposta.status = StatusTransacao.NEGADA;
        resposta.aprovado = false;
        resposta.mensagem = mensagem == null ? "Transação negada" : mensagem;
        resposta.dataHora = LocalDateTime.now();
        return resposta;
    }

    public static RespostaTransacao erro(String mensagem) {
        RespostaTransacao resposta = new RespostaTransacao();
        resposta.status = StatusTransacao.ERRO;
        resposta.aprovado = false;
        resposta.mensagem = mensagem == null ? "Erro na comunicação com a operadora" : mensagem;
        resposta.dataHora = LocalDateTime.now();
        return resposta;
    }

    public StatusTransacao getStatus() {
        return status;
    }

    public void setStatus(StatusTransacao status) {
        this.status = status;
    }

    public boolean isAprovado() {
        return aprovado;
    }

    public void setAprovado(boolean aprovado) {
        this.aprovado = aprovado;
    }

    public String getNsu() {
        return nsu;
    }

    public void setNsu(String nsu) {
        this.nsu = nsu;
    }

    public String getCodigoAutorizacao() {
        return codigoAutorizacao;
    }

    public void setCodigoAutorizacao(String codigoAutorizacao) {
        this.codigoAutorizacao = codigoAutorizacao;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getBandeira() {
        return bandeira;
    }

    public void setBandeira(String bandeira) {
        this.bandeira = bandeira;
    }

    public String getAdquirente() {
        return adquirente;
    }

    public void setAdquirente(String adquirente) {
        this.adquirente = adquirente;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }
}
