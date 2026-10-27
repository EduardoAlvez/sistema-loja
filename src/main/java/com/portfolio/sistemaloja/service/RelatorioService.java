package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.repository.ComparativoPeriodo;
import com.portfolio.sistemaloja.repository.CurvaABC;
import com.portfolio.sistemaloja.repository.GiroEstoque;
import com.portfolio.sistemaloja.repository.ProdutoEstoque;
import com.portfolio.sistemaloja.repository.ProdutoVendado;
import com.portfolio.sistemaloja.repository.ProdutoSemMovimento;
import com.portfolio.sistemaloja.repository.RelatorioRepository;
import com.portfolio.sistemaloja.repository.ResumoOperador;
import com.portfolio.sistemaloja.repository.VendaDoDia;
import com.portfolio.sistemaloja.repository.VendaHora;
import com.portfolio.sistemaloja.repository.VendaRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RelatorioService {

    private final VendaRepository vendas;
    private final RelatorioRepository relatorios;

    public RelatorioService(VendaRepository vendas, RelatorioRepository relatorios) {
        this.vendas = vendas;
        this.relatorios = relatorios;
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

    public ComparativoPeriodo comparativoPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio.isAfter(fim)) {
            throw new ValidacaoException("A data inicial deve ser anterior à final.");
        }
        return relatorios.comparativoPeriodo(inicio, fim);
    }

    public record ResumoDashboard(int vendas, BigDecimal faturamento, BigDecimal ticketMedio,
                                  int clientes, int produtos, int itensEstoqueBaixo) {
    }
}
