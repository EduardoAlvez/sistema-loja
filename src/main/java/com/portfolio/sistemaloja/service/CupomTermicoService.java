package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.model.FormaPagamento;
import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.model.VendaItem;
import com.portfolio.sistemaloja.model.VendaPagamento;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class CupomTermicoService {

    public static final int LARGURA = 48;

    private static final DecimalFormat VALOR =
            new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.forLanguageTag("pt-BR")));

    private final ConfiguracaoPix pix;

    public CupomTermicoService(ConfiguracaoPix pix) {
        this.pix = pix;
    }

    public String formatar(Venda venda) {
        StringBuilder texto = new StringBuilder();
        texto.append("=".repeat(LARGURA)).append('\n');
        texto.append(centralizar(pix.nomeRecebedor())).append('\n');
        texto.append(centralizar("CUPOM NAO FISCAL - 80mm")).append('\n');
        texto.append("=".repeat(LARGURA)).append('\n');
        texto.append("Venda: ").append(nulo(venda.getNumero())).append('\n');
        texto.append("Data: ").append(nulo(venda.getDataHora())).append('\n');
        texto.append("Cliente: ").append(truncar(nulo(venda.getClienteNome()), 39)).append('\n');
        texto.append("Operador: ").append(truncar(nulo(venda.getUsuarioNome()), 38)).append('\n');
        texto.append("-".repeat(LARGURA)).append('\n');
        texto.append(String.format("%-22s%3s%10s%10s%n", "PRODUTO", "QTD", "UNIT", "TOTAL"));
        for (VendaItem item : venda.getItens()) {
            texto.append(String.format("%-22s%3d%10s%10s%n",
                    truncar(item.getProdutoNome(), 22), item.getQuantidade(),
                    VALOR.format(item.getPrecoUnitario()), VALOR.format(item.getSubtotal())));
        }
        texto.append("-".repeat(LARGURA)).append('\n');
        if (venda.getDesconto().compareTo(BigDecimal.ZERO) > 0) {
            texto.append(linhaValor("Desconto", venda.getDesconto()));
        }
        texto.append(linhaValor("TOTAL", venda.getTotal()));
        if (venda.isPagamentoDividido()) {
            texto.append("Pagamento dividido:\n");
            for (VendaPagamento parcela : venda.getPagamentos()) {
                texto.append(linhaValor("  " + parcela.getFormaPagamento().getDescricao(), parcela.getValor()));
            }
        } else {
            texto.append(linhaValor("  " + venda.getFormaPagamento().getDescricao(), venda.getTotal()));
        }
        if (venda.getTroco().compareTo(BigDecimal.ZERO) > 0) {
            texto.append(linhaValor("Troco", venda.getTroco()));
        }

        BigDecimal valorPix = valorPix(venda);
        if (valorPix.compareTo(BigDecimal.ZERO) > 0) {
            texto.append("-".repeat(LARGURA)).append('\n');
            texto.append(centralizar("PIX - QR CODE")).append('\n');
            String payload = PixPayload.gerar(pix, valorPix, venda.getNumero());
            for (String linha : quebrar(payload, LARGURA)) {
                texto.append(linha).append('\n');
            }
            texto.append(QrAscii.gerar(payload));
            texto.append(centralizar("Pague com o app do seu banco")).append('\n');
        }
        texto.append("=".repeat(LARGURA)).append('\n');
        return texto.toString();
    }

    public BigDecimal valorPix(Venda venda) {
        if (venda.isPagamentoDividido()) {
            BigDecimal total = BigDecimal.ZERO;
            for (VendaPagamento parcela : venda.getPagamentos()) {
                if (parcela.getFormaPagamento() == FormaPagamento.PIX) {
                    total = total.add(parcela.getValor());
                }
            }
            return total;
        }
        return venda.getFormaPagamento() == FormaPagamento.PIX ? venda.getTotal() : BigDecimal.ZERO;
    }

    static String linhaValor(String rotulo, BigDecimal valor) {
        String numero = VALOR.format(valor);
        int espacos = LARGURA - rotulo.length() - numero.length();
        if (espacos < 1) {
            espacos = 1;
        }
        return rotulo + " ".repeat(espacos) + numero + "\n";
    }

    static String centralizar(String texto) {
        String conteudo = truncar(texto, LARGURA);
        int espacos = (LARGURA - conteudo.length()) / 2;
        return " ".repeat(Math.max(espacos, 0)) + conteudo;
    }

    static String truncar(String texto, int limite) {
        return texto.length() <= limite ? texto : texto.substring(0, limite);
    }

    static java.util.List<String> quebrar(String texto, int largura) {
        java.util.List<String> linhas = new java.util.ArrayList<>();
        for (int inicio = 0; inicio < texto.length(); inicio += largura) {
            linhas.add(texto.substring(inicio, Math.min(inicio + largura, texto.length())));
        }
        return linhas;
    }

    private static String nulo(String texto) {
        return texto == null || texto.isBlank() ? "-" : texto;
    }
}
