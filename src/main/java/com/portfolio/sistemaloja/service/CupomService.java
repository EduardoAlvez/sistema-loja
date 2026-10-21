package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.model.FormaPagamento;
import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.model.VendaItem;
import com.portfolio.sistemaloja.model.VendaPagamento;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class CupomService {

    private static final NumberFormat MOEDA = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));

    public String formatar(Venda venda) {
        StringBuilder texto = new StringBuilder();
        texto.append("       CUPOM NÃO FISCAL\n");
        texto.append("       ").append(venda.getNumero()).append("\n\n");
        if (venda.getDataHora() != null) {
            texto.append("Data: ").append(venda.getDataHora()).append('\n');
        }
        texto.append("Cliente: ")
                .append(venda.getClienteNome() == null ? "Consumidor" : venda.getClienteNome()).append('\n');
        texto.append("Operador: ")
                .append(venda.getUsuarioNome() == null ? "-" : venda.getUsuarioNome()).append('\n');
        texto.append("--------------------------------\n");
        for (VendaItem item : venda.getItens()) {
            texto.append(String.format("%s%n  %d x %s  =  %s%n",
                    item.getProdutoNome(), item.getQuantidade(),
                    moeda(item.getPrecoUnitario()), moeda(item.getSubtotal())));
            if (item.getDesconto().compareTo(BigDecimal.ZERO) > 0) {
                texto.append("  desc. item: -").append(moeda(item.getDesconto())).append('\n');
            }
        }
        texto.append("--------------------------------\n");
        BigDecimal descontoManual = venda.getDesconto();
        if (descontoManual.compareTo(BigDecimal.ZERO) > 0) {
            texto.append("Desconto: -").append(moeda(descontoManual)).append('\n');
        }
        texto.append("TOTAL: ").append(moeda(venda.getTotal())).append('\n');
        if (venda.getPontosResgatados() > 0) {
            texto.append("Pontos resgatados: ").append(venda.getPontosResgatados())
                    .append(" (").append(moeda(venda.getValorPontos())).append(")\n");
        }
        if (venda.getPontosCreditados() > 0) {
            texto.append("Pontos creditados: ").append(venda.getPontosCreditados()).append('\n');
        }
        if (venda.isPagamentoDividido()) {
            texto.append("Pagamento dividido:\n");
            for (VendaPagamento parcela : venda.getPagamentos()) {
                texto.append("  ").append(parcela.getFormaPagamento().getDescricao())
                        .append(": ").append(moeda(parcela.getValor())).append('\n');
            }
            if (venda.getTroco().compareTo(BigDecimal.ZERO) > 0) {
                texto.append("Troco: ").append(moeda(venda.getTroco())).append('\n');
            }
        } else {
            texto.append("Pagamento: ").append(venda.getFormaPagamento().getDescricao()).append('\n');
            if (venda.getFormaPagamento() == FormaPagamento.DINHEIRO) {
                texto.append("Pago: ").append(moeda(venda.getValorPago()))
                        .append("   Troco: ").append(moeda(venda.getTroco())).append('\n');
            }
        }
        return texto.toString();
    }

    private String moeda(BigDecimal valor) {
        return MOEDA.format(valor == null ? BigDecimal.ZERO : valor);
    }
}
