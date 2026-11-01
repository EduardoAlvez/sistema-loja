package com.portfolio.sistemaloja.model;

/**
 * Item de uma contagem de inventário.
 *
 * <p>{@code estoqueSistema} é a fotografia do estoque no momento em que a contagem foi
 * aberta e serve de referência para o operador. {@code estoqueAtual} é o estoque vivo,
 * relido do cadastro de produtos, e é contra ele que o fechamento calcula o ajuste.
 *
 * <p>{@code quantidadeContada} fica {@code null} enquanto o item não foi contado.
 * {@code diferenca} fica {@code null} até o fechamento, quando passa a guardar o
 * ajuste efetivamente aplicado.
 */
public class ContagemItem {
    private long id;
    private long contagemId;
    private long produtoId;
    private String produtoNome;
    private String produtoCodigoBarras;
    private int estoqueSistema;
    private int estoqueAtual;
    private Integer quantidadeContada;
    private Integer diferenca;
    private boolean ajustado;

    public ContagemItem() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getContagemId() {
        return contagemId;
    }

    public void setContagemId(long contagemId) {
        this.contagemId = contagemId;
    }

    public long getProdutoId() {
        return produtoId;
    }

    public void setProdutoId(long produtoId) {
        this.produtoId = produtoId;
    }

    public String getProdutoNome() {
        return produtoNome;
    }

    public void setProdutoNome(String produtoNome) {
        this.produtoNome = produtoNome;
    }

    public String getProdutoCodigoBarras() {
        return produtoCodigoBarras;
    }

    public void setProdutoCodigoBarras(String produtoCodigoBarras) {
        this.produtoCodigoBarras = produtoCodigoBarras;
    }

    public int getEstoqueSistema() {
        return estoqueSistema;
    }

    public void setEstoqueSistema(int estoqueSistema) {
        this.estoqueSistema = estoqueSistema;
    }

    public int getEstoqueAtual() {
        return estoqueAtual;
    }

    public void setEstoqueAtual(int estoqueAtual) {
        this.estoqueAtual = estoqueAtual;
    }

    public Integer getQuantidadeContada() {
        return quantidadeContada;
    }

    public void setQuantidadeContada(Integer quantidadeContada) {
        this.quantidadeContada = quantidadeContada;
    }

    public Integer getDiferenca() {
        return diferenca;
    }

    public void setDiferenca(Integer diferenca) {
        this.diferenca = diferenca;
    }

    public boolean isAjustado() {
        return ajustado;
    }

    public void setAjustado(boolean ajustado) {
        this.ajustado = ajustado;
    }

    public boolean foiContado() {
        return quantidadeContada != null;
    }

    /**
     * Diferença entre o que foi contado e o estoque atual do cadastro.
     * Vale zero enquanto o item não foi contado.
     */
    public int diferencaAtual() {
        return quantidadeContada == null ? 0 : quantidadeContada - estoqueAtual;
    }
}
