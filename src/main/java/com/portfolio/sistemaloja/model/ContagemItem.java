package com.portfolio.sistemaloja.model;

/**
 * Item de uma contagem de inventário.
 *
 * <p>Três noções de estoque convivem aqui e é importante não confundi-las:
 * <ul>
 *   <li>{@code estoqueSistema} — fotografia do estoque no momento em que a contagem foi
 *       aberta. Serve de referência para o operador e não é mais usada depois disso.</li>
 *   <li>{@code estoqueAtual} — leitura do cadastro feita pela consulta que carregou este
 *       item. É uma foto do instante da leitura, útil para exibir a prévia da diferença.
 *       <strong>Não</strong> é a base do ajuste.</li>
 *   <li>a base real do ajuste é a leitura <em>travada</em> que o fechamento faz de cada
 *       produto dentro da sua transação
 *       ({@code ProdutoRepository.estoqueParaAjuste}). Por isso {@code deltaAplicado} pode
 *       diferir de {@code quantidadeContada - estoqueSistema} quando houve movimento
 *       durante a contagem.</li>
 * </ul>
 *
 * <p>{@code quantidadeContada} fica {@code null} enquanto o item não foi contado.
 * {@code deltaAplicado} fica {@code null} até o fechamento, quando passa a guardar o
 * ajuste efetivamente aplicado ao estoque.
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
    private Integer deltaAplicado;
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

    public Integer getDeltaAplicado() {
        return deltaAplicado;
    }

    public void setDeltaAplicado(Integer deltaAplicado) {
        this.deltaAplicado = deltaAplicado;
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
     * Prévia da diferença contra o estoque lido no momento da consulta.
     *
     * <p>Retorna {@code null} enquanto o item não foi contado — assim "não contado" não se
     * confunde com "sem diferença". Só faz sentido enquanto a contagem está aberta: depois
     * do fechamento o estoque passa a ser exatamente a quantidade contada, e a prévia
     * tenderá a zero mesmo que {@link #getDeltaAplicado()} registre um ajuste real.
     */
    public Integer previaDeDiferenca() {
        return quantidadeContada == null ? null : quantidadeContada - estoqueAtual;
    }
}
