package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.model.TaxaCartao;
import com.portfolio.sistemaloja.repository.TaxaCartaoRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * Serviço para cálculo de taxas por bandeira/tipo/cartão.
 * <p>
 * Cálculo: {@code valorTaxa = (valorBruto * taxaPercentual/100) + taxaFixa}, {@code valorLiquido = valorBruto - valorTaxa}.
 * Configuração em {@code taxas_cartao}.
 */
public class TaxaCartaoService {

    private final TaxaCartaoRepository taxas;

    public TaxaCartaoService(TaxaCartaoRepository taxas) {
        this.taxas = taxas;
    }

    public List<TaxaCartao> listar(boolean apenasAtivos) {
        return taxas.listar(apenasAtivos);
    }

    public TaxaCartao salvar(TaxaCartao taxa) {
        validar(taxa);
        return taxas.salvar(taxa);
    }

    public CalculoTaxa calcular(BigDecimal valorBruto, String bandeira, String tipoCartao, int parcelas) {
        if (valorBruto == null || valorBruto.signum() <= 0) {
            return new CalculoTaxa(valorBruto == null ? BigDecimal.ZERO : valorBruto, BigDecimal.ZERO, valorBruto == null ? BigDecimal.ZERO : valorBruto, null);
        }
        Optional<TaxaCartao> taxa = taxas.buscar(bandeira, tipoCartao, parcelas);
        if (taxa.isEmpty()) {
            return new CalculoTaxa(valorBruto, BigDecimal.ZERO, valorBruto, null);
        }
        BigDecimal percentual = taxa.get().getTaxaPercentual().divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        BigDecimal valorTaxa = valorBruto.multiply(percentual).add(taxa.get().getTaxaFixa());
        if (valorTaxa.signum() < 0) {
            valorTaxa = BigDecimal.ZERO;
        }
        valorTaxa = valorTaxa.setScale(2, RoundingMode.HALF_UP);
        BigDecimal valorLiquido = valorBruto.subtract(valorTaxa).setScale(2, RoundingMode.HALF_UP);
        return new CalculoTaxa(valorBruto, valorTaxa, valorLiquido, taxa.get());
    }

    private void validar(TaxaCartao taxa) {
        if (taxa == null) {
            throw new ValidacaoException("Taxa inválida.");
        }
        if (taxa.getBandeira() == null || taxa.getBandeira().isBlank()) {
            throw new ValidacaoException("Bandeira é obrigatória.");
        }
        if (taxa.getTipoCartao() == null || taxa.getTipoCartao().isBlank()) {
            throw new ValidacaoException("Tipo do cartão é obrigatório.");
        }
        if (taxa.getParcelasMin() < 1 || taxa.getParcelasMax() < taxa.getParcelasMin()) {
            throw new ValidacaoException("Faixa de parcelas inválida.");
        }
        if (taxa.getTaxaPercentual() == null || taxa.getTaxaPercentual().signum() < 0) {
            throw new ValidacaoException("Taxa percentual inválida.");
        }
        if (taxa.getTaxaFixa() == null || taxa.getTaxaFixa().signum() < 0) {
            throw new ValidacaoException("Taxa fixa inválida.");
        }
    }

    public record CalculoTaxa(BigDecimal valorBruto, BigDecimal valorTaxa, BigDecimal valorLiquido, TaxaCartao taxa) {
    }
}
