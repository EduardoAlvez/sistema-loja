package com.portfolio.sistemaloja.model;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public class Produto {

    private Long id;
    private String codigoBarras;
    private String nome;
    private Long categoriaId;
    private String categoriaNome;
    private BigDecimal precoCusto;
    private BigDecimal precoVenda;
    private int estoque;
    private int estoqueMinimo;
    private boolean ativo;
    private BigDecimal precoPromocional;
    private LocalDate promoInicio;
    private LocalDate promoFim;
    private Set<DayOfWeek> promoDias = EnumSet.noneOf(DayOfWeek.class);
    private LocalTime promoHoraInicio;
    private LocalTime promoHoraFim;

    public Produto() {
    }

    public Produto(Long id, String codigoBarras, String nome, Long categoriaId,
                   BigDecimal precoCusto, BigDecimal precoVenda, int estoque, boolean ativo) {
        this.id = id;
        this.codigoBarras = codigoBarras;
        this.nome = nome;
        this.categoriaId = categoriaId;
        this.precoCusto = precoCusto;
        this.precoVenda = precoVenda;
        this.estoque = estoque;
        this.ativo = ativo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigoBarras() {
        return codigoBarras;
    }

    public void setCodigoBarras(String codigoBarras) {
        this.codigoBarras = codigoBarras;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Long getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Long categoriaId) {
        this.categoriaId = categoriaId;
    }

    public String getCategoriaNome() {
        return categoriaNome;
    }

    public void setCategoriaNome(String categoriaNome) {
        this.categoriaNome = categoriaNome;
    }

    public BigDecimal getPrecoCusto() {
        return precoCusto;
    }

    public void setPrecoCusto(BigDecimal precoCusto) {
        this.precoCusto = precoCusto;
    }

    public BigDecimal getPrecoVenda() {
        return precoVenda;
    }

    public void setPrecoVenda(BigDecimal precoVenda) {
        this.precoVenda = precoVenda;
    }

    public int getEstoque() {
        return estoque;
    }

    public void setEstoque(int estoque) {
        this.estoque = estoque;
    }

    public int getEstoqueMinimo() {
        return estoqueMinimo;
    }

    public void setEstoqueMinimo(int estoqueMinimo) {
        this.estoqueMinimo = estoqueMinimo;
    }

    public boolean estoqueBaixo() {
        return estoque <= estoqueMinimo;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public BigDecimal getPrecoPromocional() {
        return precoPromocional;
    }

    public void setPrecoPromocional(BigDecimal precoPromocional) {
        this.precoPromocional = precoPromocional;
    }

    public LocalDate getPromoInicio() {
        return promoInicio;
    }

    public void setPromoInicio(LocalDate promoInicio) {
        this.promoInicio = promoInicio;
    }

    public LocalDate getPromoFim() {
        return promoFim;
    }

    public void setPromoFim(LocalDate promoFim) {
        this.promoFim = promoFim;
    }

    public Set<DayOfWeek> getPromoDias() {
        return Collections.unmodifiableSet(promoDias);
    }

    public void setPromoDias(Set<DayOfWeek> promoDias) {
        this.promoDias = promoDias == null || promoDias.isEmpty()
                ? EnumSet.noneOf(DayOfWeek.class)
                : EnumSet.copyOf(promoDias);
    }

    public LocalTime getPromoHoraInicio() {
        return promoHoraInicio;
    }

    public void setPromoHoraInicio(LocalTime promoHoraInicio) {
        this.promoHoraInicio = promoHoraInicio;
    }

    public LocalTime getPromoHoraFim() {
        return promoHoraFim;
    }

    public void setPromoHoraFim(LocalTime promoHoraFim) {
        this.promoHoraFim = promoHoraFim;
    }

    public BigDecimal calcularLucro() {
        if (precoCusto == null || precoVenda == null) {
            return BigDecimal.ZERO;
        }
        return precoVenda.subtract(precoCusto);
    }

    public boolean promocaoVigente(LocalDateTime momento) {
        if (momento == null || precoPromocional == null || precoPromocional.signum() <= 0) {
            return false;
        }
        if (precoVenda != null && precoPromocional.compareTo(precoVenda) >= 0) {
            return false;
        }
        if (promoInicio != null && promoFim != null && promoFim.isBefore(promoInicio)) {
            return false;
        }
        LocalDate data = momento.toLocalDate();
        if (promoInicio != null && data.isBefore(promoInicio)) {
            return false;
        }
        if (promoFim != null && data.isAfter(promoFim)) {
            return false;
        }
        if (!promoDias.isEmpty() && !promoDias.contains(data.getDayOfWeek())) {
            return false;
        }
        boolean temInicio = promoHoraInicio != null;
        boolean temFim = promoHoraFim != null;
        if (temInicio != temFim) {
            return false;
        }
        if (temInicio && !promoHoraInicio.isBefore(promoHoraFim)) {
            return false;
        }
        if (temInicio) {
            LocalTime hora = momento.toLocalTime();
            if (hora.isBefore(promoHoraInicio) || hora.isAfter(promoHoraFim)) {
                return false;
            }
        }
        return true;
    }

    public BigDecimal precoVigente(LocalDateTime momento) {
        return promocaoVigente(momento) ? precoPromocional : precoVenda;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Produto produto)) {
            return false;
        }
        return Objects.equals(id, produto.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return nome;
    }
}
