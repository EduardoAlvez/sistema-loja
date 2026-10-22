package com.portfolio.sistemaloja.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Venda {

    private Long id;
    private String numero;
    private String dataHora;
    private Long clienteId;
    private String clienteNome;
    private Long usuarioId;
    private String usuarioNome;
    private FormaPagamento formaPagamento;
    private BigDecimal desconto = BigDecimal.ZERO;
    private BigDecimal total = BigDecimal.ZERO;
    private BigDecimal valorPago = BigDecimal.ZERO;
    private BigDecimal troco = BigDecimal.ZERO;
    private StatusVenda status = StatusVenda.CONCLUIDA;
    private int pontosResgatados;
    private int pontosCreditados;
    private int pontosEstornados;
    private BigDecimal valorPontos = BigDecimal.ZERO;
    private BigDecimal somaItensBrutos;
    private final List<VendaItem> itens = new ArrayList<>();
    private final List<VendaPagamento> pagamentos = new ArrayList<>();

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

    public String getDataHora() {
        return dataHora;
    }

    public void setDataHora(String dataHora) {
        this.dataHora = dataHora;
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

    public FormaPagamento getFormaPagamento() {
        return formaPagamento;
    }

    public void setFormaPagamento(FormaPagamento formaPagamento) {
        this.formaPagamento = formaPagamento;
    }

    public BigDecimal getDesconto() {
        return desconto;
    }

    public void setDesconto(BigDecimal desconto) {
        this.desconto = desconto == null ? BigDecimal.ZERO : desconto;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public BigDecimal getValorPago() {
        return valorPago;
    }

    public void setValorPago(BigDecimal valorPago) {
        this.valorPago = valorPago;
    }

    public BigDecimal getTroco() {
        return troco;
    }

    public void setTroco(BigDecimal troco) {
        this.troco = troco;
    }

    public StatusVenda getStatus() {
        return status;
    }

    public void setStatus(StatusVenda status) {
        this.status = status;
    }

    public int getPontosResgatados() {
        return pontosResgatados;
    }

    public void setPontosResgatados(int pontosResgatados) {
        this.pontosResgatados = pontosResgatados;
    }

    public int getPontosCreditados() {
        return pontosCreditados;
    }
    public void setPontosCreditados(int pontosCreditados) {
        this.pontosCreditados = pontosCreditados;
    }

    public int getPontosEstornados() {
        return pontosEstornados;
    }

    public void setPontosEstornados(int pontosEstornados) {
        this.pontosEstornados = pontosEstornados;
    }

    public BigDecimal getValorPontos() {
        return valorPontos;
    }

    public void setValorPontos(BigDecimal valorPontos) {
        this.valorPontos = valorPontos == null ? BigDecimal.ZERO : valorPontos;
    }

    public List<VendaItem> getItens() {
        return itens;
    }

    public void adicionarItem(VendaItem item) {
        itens.add(item);
        recalcularTotal();
    }

    public List<VendaPagamento> getPagamentos() {
        return pagamentos;
    }

    public void adicionarPagamento(VendaPagamento pagamento) {
        pagamentos.add(pagamento);
    }

    public boolean isPagamentoDividido() {
        return pagamentos.size() > 1;
    }

    public void recalcularTotal() {
        total = somaItens()
                .subtract(desconto == null ? BigDecimal.ZERO : desconto)
                .subtract(valorPontos == null ? BigDecimal.ZERO : valorPontos);
    }

    public BigDecimal somaItens() {
        return itens.stream()
                .map(VendaItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal somaItensBrutos() {
        if (somaItensBrutos != null) {
            return somaItensBrutos;
        }
        return itens.stream()
                .map(VendaItem::getValorBruto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void setSomaItensBrutos(BigDecimal somaItensBrutos) {
        this.somaItensBrutos = somaItensBrutos;
    }
}
