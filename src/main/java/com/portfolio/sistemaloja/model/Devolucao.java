package com.portfolio.sistemaloja.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Devolucao {

    private Long id;
    private Long vendaId;
    private String vendaNumero;
    private Long clienteId;
    private String clienteNome;
    private Long usuarioId;
    private String usuarioNome;
    private String dataHora;
    private BigDecimal valor = BigDecimal.ZERO;
    private boolean creditoCliente;
    private int pontosEstornados;
    private String observacao;
    private final List<DevolucaoItem> itens = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVendaId() {
        return vendaId;
    }

    public void setVendaId(Long vendaId) {
        this.vendaId = vendaId;
    }

    public String getVendaNumero() {
        return vendaNumero;
    }

    public void setVendaNumero(String vendaNumero) {
        this.vendaNumero = vendaNumero;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public String getClienteNome() {
        return clienteNome;
    }

    public void setClienteNome(String clienteNome) {
        this.clienteNome = clienteNome;
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

    public String getDataHora() {
        return dataHora;
    }

    public void setDataHora(String dataHora) {
        this.dataHora = dataHora;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public boolean isCreditoCliente() {
        return creditoCliente;
    }

    public void setCreditoCliente(boolean creditoCliente) {
        this.creditoCliente = creditoCliente;
    }

    public int getPontosEstornados() {
        return pontosEstornados;
    }

    public void setPontosEstornados(int pontosEstornados) {
        this.pontosEstornados = pontosEstornados;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public List<DevolucaoItem> getItens() {
        return itens;
    }

    public void adicionarItem(DevolucaoItem item) {
        itens.add(item);
    }

    public int getQuantidadeItens() {
        int total = 0;
        for (DevolucaoItem item : itens) {
            total += item.getQuantidade();
        }
        return total;
    }

    public void recalcularValor() {
        BigDecimal soma = BigDecimal.ZERO;
        for (DevolucaoItem item : itens) {
            soma = soma.add(item.getSubtotal());
        }
        this.valor = soma;
    }
}
