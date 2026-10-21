package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.model.FormaPagamento;
import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.model.VendaItem;
import com.portfolio.sistemaloja.model.VendaPagamento;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
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
        for (String linha : linhas(venda)) {
            texto.append(linha).append('\n');
        }
        String payload = payloadPix(venda);
        if (payload != null) {
            texto.append("-".repeat(LARGURA)).append('\n');
            texto.append(centralizar("PIX - QR CODE")).append('\n');
            for (String linha : quebrar(payload, LARGURA)) {
                texto.append(linha).append('\n');
            }
            texto.append(QrAscii.gerar(payload));
            texto.append(centralizar("Pague com o app do seu banco")).append('\n');
        }
        texto.append("=".repeat(LARGURA)).append('\n');
        return texto.toString();
    }

    public List<String> linhas(Venda venda) {
        List<String> linhas = new ArrayList<>();
        linhas.add("=".repeat(LARGURA));
        linhas.add(centralizar(pix.nomeRecebedor()));
        linhas.add(centralizar("CUPOM NAO FISCAL - 80mm"));
        linhas.add("=".repeat(LARGURA));
        linhas.add("Venda: " + nulo(venda.getNumero()));
        linhas.add("Data: " + nulo(venda.getDataHora()));
        linhas.add("Cliente: " + truncar(nulo(venda.getClienteNome()), 39));
        linhas.add("Operador: " + truncar(nulo(venda.getUsuarioNome()), 38));
        linhas.add("-".repeat(LARGURA));
        linhas.add(String.format("%-22s%3s%10s%10s", "PRODUTO", "QTD", "UNIT", "TOTAL"));
        for (VendaItem item : venda.getItens()) {
            linhas.add(String.format("%-22s%3d%10s%10s",
                    truncar(item.getProdutoNome(), 22), item.getQuantidade(),
                    VALOR.format(item.getPrecoUnitario()), VALOR.format(item.getSubtotal())));
        }
        linhas.add("-".repeat(LARGURA));
        if (venda.getDesconto().compareTo(BigDecimal.ZERO) > 0) {
            linhas.add(linhaValor("Desconto", venda.getDesconto()));
        }
        if (venda.getPontosResgatados() > 0) {
            linhas.add("Pontos resgatados: " + venda.getPontosResgatados()
                    + " (" + VALOR.format(venda.getValorPontos()) + ")");
        }
        if (venda.getPontosCreditados() > 0) {
            linhas.add("Pontos creditados: " + venda.getPontosCreditados());
        }
        linhas.add(linhaValor("TOTAL", venda.getTotal()));
        if (venda.isPagamentoDividido()) {
            linhas.add("Pagamento dividido:");
            for (VendaPagamento parcela : venda.getPagamentos()) {
                linhas.add(linhaValor("  " + parcela.getFormaPagamento().getDescricao(), parcela.getValor()));
            }
        } else {
            linhas.add(linhaValor("  " + venda.getFormaPagamento().getDescricao(), venda.getTotal()));
        }
        if (venda.getTroco().compareTo(BigDecimal.ZERO) > 0) {
            linhas.add(linhaValor("Troco", venda.getTroco()));
        }
        return linhas;
    }

    public String payloadPix(Venda venda) {
        BigDecimal valor = valorPix(venda);
        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return PixPayload.gerar(pix, valor, venda.getNumero());
    }

    public boolean pagamentoEmDinheiro(Venda venda) {
        if (venda.isPagamentoDividido()) {
            for (VendaPagamento parcela : venda.getPagamentos()) {
                if (parcela.getFormaPagamento() == FormaPagamento.DINHEIRO) {
                    return true;
                }
            }
            return false;
        }
        return venda.getFormaPagamento() == FormaPagamento.DINHEIRO;
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
        return rotulo + " ".repeat(espacos) + numero;
    }

    static String centralizar(String texto) {
        String conteudo = truncar(texto, LARGURA);
        int espacos = (LARGURA - conteudo.length()) / 2;
        return " ".repeat(Math.max(espacos, 0)) + conteudo;
    }

    static String truncar(String texto, int limite) {
        return texto.length() <= limite ? texto : texto.substring(0, limite);
    }

    public static List<String> quebrar(String texto, int largura) {
        List<String> linhas = new ArrayList<>();
        for (int inicio = 0; inicio < texto.length(); inicio += largura) {
            linhas.add(texto.substring(inicio, Math.min(inicio + largura, texto.length())));
        }
        return linhas;
    }

    private static String nulo(String texto) {
        return texto == null || texto.isBlank() ? "-" : texto;
    }
}
