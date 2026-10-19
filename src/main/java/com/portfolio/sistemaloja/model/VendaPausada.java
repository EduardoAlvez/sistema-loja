package com.portfolio.sistemaloja.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class VendaPausada {

    private Long id;
    private String dataHora;
    private Long clienteId;
    private String clienteNome;
    private Long usuarioId;
    private String usuarioNome;
    private FormaPagamento formaPagamento;
    private BigDecimal desconto = BigDecimal.ZERO;
    private BigDecimal total = BigDecimal.ZERO;
    private String observacao;
    private final List<VendaItem> itens = new ArrayList<>();
    private final List<VendaPagamento> pagamentos = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
        this.total = total == null ? BigDecimal.ZERO : total;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public List<VendaItem> getItens() {
        return itens;
    }

    public List<VendaPagamento> getPagamentos() {
        return pagamentos;
    }

    public int getQuantidadeItens() {
        return itens.stream().mapToInt(VendaItem::getQuantidade).sum();
    }

    public Venda paraVenda() {
        Venda venda = new Venda();
        venda.setClienteId(clienteId);
        venda.setUsuarioId(usuarioId);
        venda.setFormaPagamento(formaPagamento);
        venda.setDesconto(desconto);
        for (VendaItem item : itens) {
            venda.adicionarItem(item);
        }
        for (VendaPagamento pagamento : pagamentos) {
            venda.adicionarPagamento(pagamento);
        }
        if (!pagamentos.isEmpty() && venda.getFormaPagamento() == null) {
            venda.setFormaPagamento(pagamentos.get(0).getFormaPagamento());
        }
        return venda;
    }
}
