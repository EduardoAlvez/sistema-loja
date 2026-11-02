package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.model.TaxaCartao;
import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.repository.ComparativoPeriodo;
import com.portfolio.sistemaloja.repository.CurvaABC;
import com.portfolio.sistemaloja.repository.GiroEstoque;
import com.portfolio.sistemaloja.repository.ProdutoEstoque;
import com.portfolio.sistemaloja.repository.ProdutoSemMovimento;
import com.portfolio.sistemaloja.repository.ProdutoVendado;
import com.portfolio.sistemaloja.repository.SugestaoCompra;
import com.portfolio.sistemaloja.repository.RelatorioRepository;
import com.portfolio.sistemaloja.repository.ResumoOperador;
import com.portfolio.sistemaloja.repository.TaxaCartaoRepository;
import com.portfolio.sistemaloja.repository.TotaisFormaPagamento;
import com.portfolio.sistemaloja.repository.VendaDoDia;
import com.portfolio.sistemaloja.repository.VendaHora;
import com.portfolio.sistemaloja.repository.VendaRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class RelatorioService {

    private final VendaRepository vendas;
    private final RelatorioRepository relatorios;
    private final TaxaCartaoRepository taxas;

    public RelatorioService(VendaRepository vendas, RelatorioRepository relatorios,
                            TaxaCartaoRepository taxas) {
        this.vendas = vendas;
        this.relatorios = relatorios;
        this.taxas = taxas;
    }

    public ResumoDashboard resumoDoDia() {
        LocalDate hoje = LocalDate.now();
        int quantidade = vendas.quantidadeVendas(hoje, hoje);
        BigDecimal faturamento = vendas.totalFaturado(hoje, hoje);
        BigDecimal ticketMedio = quantidade == 0
                ? BigDecimal.ZERO
                : faturamento.divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.HALF_UP);
        return new ResumoDashboard(quantidade, faturamento, ticketMedio,
                relatorios.contarClientesAtivos(), relatorios.contarProdutosAtivos(),
                estoqueBaixo().size());
    }

    public ResumoDashboard resumoPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio.isAfter(fim)) {
            throw new ValidacaoException("A data inicial deve ser anterior à final.");
        }
        int quantidade = vendas.quantidadeVendas(inicio, fim);
        BigDecimal faturamento = vendas.totalFaturado(inicio, fim);
        BigDecimal ticketMedio = quantidade == 0
                ? BigDecimal.ZERO
                : faturamento.divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.HALF_UP);
        return new ResumoDashboard(quantidade, faturamento, ticketMedio,
                relatorios.contarClientesAtivos(), relatorios.contarProdutosAtivos(),
                estoqueBaixo().size());
    }

    public List<Venda> vendasDoPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio.isAfter(fim)) {
            throw new ValidacaoException("A data inicial deve ser anterior à final.");
        }
        return vendas.listar(inicio, fim);
    }

    public List<ProdutoVendado> maisVendidos(LocalDate inicio, LocalDate fim) {
        return relatorios.maisVendidos(inicio, fim, 10);
    }

    public List<VendaDoDia> faturamentoPorDia(LocalDate inicio, LocalDate fim) {
        if (inicio.isAfter(fim)) {
            throw new ValidacaoException("A data inicial deve ser anterior à final.");
        }
        List<VendaDoDia> doBanco = relatorios.faturamentoPorDia(inicio, fim);
        List<VendaDoDia> porDia = new ArrayList<>();
        int posicao = 0;
        for (LocalDate dia = inicio; !dia.isAfter(fim); dia = dia.plusDays(1)) {
            if (posicao < doBanco.size() && doBanco.get(posicao).dia().equals(dia)) {
                porDia.add(doBanco.get(posicao++));
            } else {
                porDia.add(new VendaDoDia(dia, 0, BigDecimal.ZERO));
            }
        }
        return porDia;
    }

    public List<ProdutoEstoque> estoqueBaixo() {
        return relatorios.estoqueBaixo();
    }

    public List<ResumoOperador> resumoPorOperador(LocalDate inicio, LocalDate fim) {
        if (inicio.isAfter(fim)) {
            throw new ValidacaoException("A data inicial deve ser anterior à final.");
        }
        return relatorios.resumoPorOperador(inicio, fim);
    }

    public BigDecimal comissaoTotal(LocalDate inicio, LocalDate fim) {
        BigDecimal total = BigDecimal.ZERO;
        for (ResumoOperador operador : resumoPorOperador(inicio, fim)) {
            total = total.add(operador.comissao());
        }
        return total;
    }

    public List<VendaHora> vendasPorHora(LocalDate inicio, LocalDate fim) {
        if (inicio.isAfter(fim)) {
            throw new ValidacaoException("A data inicial deve ser anterior à final.");
        }
        return relatorios.vendasPorHora(inicio, fim);
    }

    public List<CurvaABC> curvaABCProdutos(LocalDate inicio, LocalDate fim) {
        if (inicio.isAfter(fim)) {
            throw new ValidacaoException("A data inicial deve ser anterior à final.");
        }
        return relatorios.curvaABCProdutos(inicio, fim);
    }

    public List<CurvaABC> curvaABCCategorias(LocalDate inicio, LocalDate fim) {
        if (inicio.isAfter(fim)) {
            throw new ValidacaoException("A data inicial deve ser anterior à final.");
        }
        return relatorios.curvaABCCategorias(inicio, fim);
    }

    public List<GiroEstoque> giroEstoque(LocalDate inicio, LocalDate fim) {
        if (inicio.isAfter(fim)) {
            throw new ValidacaoException("A data inicial deve ser anterior à final.");
        }
        return relatorios.giroEstoque(inicio, fim);
    }

    public List<ProdutoSemMovimento> produtosSemMovimento(LocalDate inicio, LocalDate fim) {
        if (inicio.isAfter(fim)) {
            throw new ValidacaoException("A data inicial deve ser anterior à final.");
        }
        return relatorios.produtosSemMovimento(inicio, fim);
    }

    public List<SugestaoCompra> sugestaoCompra(LocalDate inicio, LocalDate fim) {
        if (inicio != null && fim != null && inicio.isAfter(fim)) {
            throw new ValidacaoException("A data inicial deve ser anterior à final.");
        }
        return relatorios.sugestaoCompra(inicio, fim);
    }

    public ComparativoPeriodo comparativoPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio.isAfter(fim)) {
            throw new ValidacaoException("A data inicial deve ser anterior à final.");
        }
        return relatorios.comparativoPeriodo(inicio, fim);
    }

    /**
     * Projecao de custo de taxa de cartao pela configuracao vigente. Cada linha assume
     * que TODOS os lancamentos do tipo no periodo passaram por aquela taxa: as linhas de
     * um mesmo tipo se sobrepoe sobre o mesmo total, e a soma entre linhas nao e grandeza
     * real. A taxa fixa incide por lancamento; o percentual, sobre o bruto do tipo.
     */
    public List<ProjecaoTaxaCartao> taxasCartao(LocalDate inicio, LocalDate fim) {
        if (inicio.isAfter(fim)) {
            throw new ValidacaoException("A data inicial deve ser anterior à final.");
        }
        Map<String, TotaisFormaPagamento> porForma = relatorios.totaisPorFormaEntre(inicio, fim).stream()
                .collect(Collectors.toMap(t -> t.forma().toUpperCase(Locale.ROOT), t -> t, (a, b) -> a));
        List<ProjecaoTaxaCartao> projecoes = new ArrayList<>();
        for (TaxaCartao taxa : taxas.listar(true)) {
            TotaisFormaPagamento totais = porForma.get(
                    taxa.getTipoCartao().toUpperCase(Locale.ROOT));
            int quantidade = totais == null ? 0 : totais.quantidade();
            BigDecimal bruto = totais == null ? BigDecimal.ZERO : totais.total();
            BigDecimal percentual = taxa.getTaxaPercentual()
                    .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
            BigDecimal valorTaxa = bruto.multiply(percentual)
                    .add(taxa.getTaxaFixa().multiply(BigDecimal.valueOf(quantidade)))
                    .setScale(2, RoundingMode.HALF_UP);
            if (valorTaxa.signum() < 0) {
                valorTaxa = BigDecimal.ZERO;
            }
            BigDecimal valorLiquido = bruto.subtract(valorTaxa).setScale(2, RoundingMode.HALF_UP);
            projecoes.add(new ProjecaoTaxaCartao(taxa.getBandeira(), taxa.getAdquirente(),
                    taxa.getTipoCartao(), taxa.getParcelasMin(), taxa.getParcelasMax(),
                    quantidade, bruto, valorTaxa, valorLiquido));
        }
        return projecoes;
    }

    public record ProjecaoTaxaCartao(String bandeira, String adquirente, String tipoCartao,
                                     int parcelasMin, int parcelasMax, int quantidadeLancamentos,
                                     BigDecimal valorBruto, BigDecimal valorTaxa,
                                     BigDecimal valorLiquido) {

        public String faixaParcelas() {
            return parcelasMin == parcelasMax
                    ? String.valueOf(parcelasMin) + "x"
                    : parcelasMin + "-" + parcelasMax + "x";
        }

        public String adquirenteOuPadrao() {
            return adquirente == null || adquirente.isBlank() ? "-" : adquirente;
        }
    }

    public record ResumoDashboard(int vendas, BigDecimal faturamento, BigDecimal ticketMedio,
                                  int clientes, int produtos, int itensEstoqueBaixo) {
    }
}
