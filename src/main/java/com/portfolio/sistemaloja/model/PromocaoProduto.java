package com.portfolio.sistemaloja.model;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public class PromocaoProduto {

    private Long id;
    private Long produtoId;
    private String nome;
    private String produtoNome;
    private Integer quantidadeMinima;
    private BigDecimal precoPromocional;
    private boolean ativa;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private Set<DayOfWeek> diasSemana = EnumSet.noneOf(DayOfWeek.class);
    private LocalTime horarioInicio;
    private LocalTime horarioFim;

    public PromocaoProduto() {
    }

    public PromocaoProduto(Long id, Long produtoId, String nome, Integer quantidadeMinima,
                           BigDecimal precoPromocional, boolean ativa, LocalDate dataInicio,
                           LocalDate dataFim, Set<DayOfWeek> diasSemana, LocalTime horarioInicio,
                           LocalTime horarioFim) {
        this.id = id;
        this.produtoId = produtoId;
        this.nome = nome;
        this.quantidadeMinima = quantidadeMinima;
        this.precoPromocional = precoPromocional;
        this.ativa = ativa;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        setDiasSemana(diasSemana);
        this.horarioInicio = horarioInicio;
        this.horarioFim = horarioFim;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProdutoId() {
        return produtoId;
    }

    public void setProdutoId(Long produtoId) {
        this.produtoId = produtoId;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getProdutoNome() {
        return produtoNome;
    }

    public void setProdutoNome(String produtoNome) {
        this.produtoNome = produtoNome;
    }

    public Integer getQuantidadeMinima() {
        return quantidadeMinima;
    }

    public void setQuantidadeMinima(Integer quantidadeMinima) {
        this.quantidadeMinima = quantidadeMinima;
    }

    public BigDecimal getPrecoPromocional() {
        return precoPromocional;
    }

    public void setPrecoPromocional(BigDecimal precoPromocional) {
        this.precoPromocional = precoPromocional;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public void setAtiva(boolean ativa) {
        this.ativa = ativa;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDate dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }

    public void setDataFim(LocalDate dataFim) {
        this.dataFim = dataFim;
    }

    public Set<DayOfWeek> getDiasSemana() {
        return Collections.unmodifiableSet(diasSemana);
    }

    public void setDiasSemana(Set<DayOfWeek> diasSemana) {
        this.diasSemana = diasSemana == null || diasSemana.isEmpty()
                ? EnumSet.noneOf(DayOfWeek.class)
                : EnumSet.copyOf(diasSemana);
    }

    public LocalTime getHorarioInicio() {
        return horarioInicio;
    }

    public void setHorarioInicio(LocalTime horarioInicio) {
        this.horarioInicio = horarioInicio;
    }

    public LocalTime getHorarioFim() {
        return horarioFim;
    }

    public void setHorarioFim(LocalTime horarioFim) {
        this.horarioFim = horarioFim;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PromocaoProduto)) return false;
        PromocaoProduto that = (PromocaoProduto) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PromocaoProduto{" +
                "id=" + id +
                ", produtoId=" + produtoId +
                ", nome='" + nome + '\'' +
                ", quantidadeMinima=" + quantidadeMinima +
                ", precoPromocional=" + precoPromocional +
                ", ativa=" + ativa +
                '}';
    }
}
