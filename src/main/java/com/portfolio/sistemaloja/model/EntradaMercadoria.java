package com.portfolio.sistemaloja.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EntradaMercadoria {

    private Long id;
    private Long fornecedorId;
    private String fornecedorNome;
    private String numeroNota;
    private LocalDateTime dataEntrada;
    private Long usuarioId;
    private String usuarioNome;
    private BigDecimal valorTotal = BigDecimal.ZERO;
    private String observacao;
    private LocalDateTime criadoEm;
    private final List<EntradaItem> itens = new ArrayList<>();

    public EntradaMercadoria() {
    }

    public EntradaMercadoria(Long id, Long fornecedorId, String fornecedorNome, String numeroNota,
                             LocalDateTime dataEntrada, Long usuarioId, String usuarioNome,
                             BigDecimal valorTotal, String observacao, LocalDateTime criadoEm) {
        this.id = id;
        this.fornecedorId = fornecedorId;
        this.fornecedorNome = fornecedorNome;
        this.numeroNota = numeroNota;
        this.dataEntrada = dataEntrada;
        this.usuarioId = usuarioId;
        this.usuarioNome = usuarioNome;
        this.valorTotal = valorTotal == null ? BigDecimal.ZERO : valorTotal;
        this.observacao = observacao;
        this.criadoEm = criadoEm;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFornecedorId() {
        return fornecedorId;
    }

    public void setFornecedorId(Long fornecedorId) {
        this.fornecedorId = fornecedorId;
    }

    public String getFornecedorNome() {
        return fornecedorNome;
    }

    public void setFornecedorNome(String fornecedorNome) {
        this.fornecedorNome = fornecedorNome;
    }

    public String getNumeroNota() {
        return numeroNota;
    }

    public void setNumeroNota(String numeroNota) {
        this.numeroNota = numeroNota;
    }

    public LocalDateTime getDataEntrada() {
        return dataEntrada;
    }

    public void setDataEntrada(LocalDateTime dataEntrada) {
        this.dataEntrada = dataEntrada;
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

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal == null ? BigDecimal.ZERO : valorTotal;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public List<EntradaItem> getItens() {
        return itens;
    }

    public void adicionarItem(EntradaItem item) {
        if (item == null) {
            return;
        }
        item.recalcularSubtotal();
        itens.add(item);
        valorTotal = valorTotal.add(item.getSubtotal());
    }

    public void recalcularTotal() {
        valorTotal = BigDecimal.ZERO;
        for (EntradaItem item : itens) {
            item.recalcularSubtotal();
            valorTotal = valorTotal.add(item.getSubtotal());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EntradaMercadoria entrada)) {
            return false;
        }
        return Objects.equals(id, entrada.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
