package com.portfolio.sistemaloja.grafico;

import com.portfolio.sistemaloja.repository.VendaDoDia;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class GraficoFaturamento {

    public static final Color COR_BARRA = new Color(33, 97, 140);
    public static final Color COR_GRID = new Color(226, 230, 234);
    public static final Color COR_TEXTO = new Color(110, 118, 126);
    public static final Color COR_FUNDO = Color.WHITE;

    private static final DateTimeFormatter ROTULO_DIA = DateTimeFormatter.ofPattern("dd/MM");
    private static final NumberFormat MOEDA = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));
    private static final NumberFormat MOEDA_SEM_CENTAVOS =
            NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    static {
        MOEDA_SEM_CENTAVOS.setMinimumFractionDigits(0);
        MOEDA_SEM_CENTAVOS.setMaximumFractionDigits(0);
    }

    public record Barra(int x, int y, int largura, int altura, VendaDoDia dado) {
    }

    public record Desenho(BufferedImage imagem, List<Barra> barras, boolean semVendas) {
    }

    public Desenho render(int largura, int altura, List<VendaDoDia> dados) {
        BufferedImage imagem = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
        Graphics2D graficos = imagem.createGraphics();
        List<Barra> barras = new ArrayList<>();
        boolean semVendas = dados.stream().allMatch(d -> d.total().signum() == 0);
        try {
            graficos.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graficos.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            desenhar(graficos, largura, altura, dados, barras, semVendas);
        } finally {
            graficos.dispose();
        }
        return new Desenho(imagem, List.copyOf(barras), semVendas);
    }

    private void desenhar(Graphics2D g, int largura, int altura, List<VendaDoDia> dados,
                          List<Barra> barras, boolean semVendas) {
        g.setColor(COR_FUNDO);
        g.fillRect(0, 0, largura, altura);

        int margemEsq = 72;
        int margemDir = 16;
        int margemTopo = 44;
        int margemBase = 30;
        int areaX = margemEsq;
        int areaY = margemTopo;
        int areaL = Math.max(1, largura - margemEsq - margemDir);
        int areaA = Math.max(1, altura - margemTopo - margemBase);

        g.setColor(new Color(40, 46, 54));
        g.setFont(g.getFont().deriveFont(Font.BOLD, 13f));
        g.drawString("Faturamento por dia", margemEsq, 24);

        if (semVendas) {
            g.setColor(COR_TEXTO);
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 13f));
            FontMetrics fonte = g.getFontMetrics();
            String texto = "Sem vendas no período";
            g.drawString(texto, areaX + (areaL - fonte.stringWidth(texto)) / 2, areaY + areaA / 2);
            return;
        }

        double maximo = dados.stream()
                .map(VendaDoDia::total)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO)
                .doubleValue();
        maximo = Math.max(maximo, 1.0);

        int linhas = 4;
        g.setFont(g.getFont().deriveFont(Font.PLAIN, 10f));
        FontMetrics fonte = g.getFontMetrics();
        for (int i = 0; i <= linhas; i++) {
            int y = areaY + areaA - (int) Math.round(areaA * (double) i / linhas);
            g.setColor(COR_GRID);
            g.drawLine(areaX, y, areaX + areaL, y);
            if (i > 0) {
                String rotulo = rotuloCompacto(maximo * i / linhas);
                g.setColor(COR_TEXTO);
                g.drawString(rotulo, areaX - 8 - fonte.stringWidth(rotulo), y + fonte.getAscent() / 2 - 1);
            }
        }

        int quantidade = dados.size();
        double espaco = (double) areaL / Math.max(quantidade, 1);
        int larguraBarra = Math.max(2, (int) Math.min(espaco * 0.65, 48));
        boolean rotulosValor = quantidade <= 15;
        int passoDia = Math.max(1, (int) Math.ceil(quantidade / 10.0));

        for (int i = 0; i < quantidade; i++) {
            VendaDoDia dado = dados.get(i);
            int centro = areaX + (int) Math.round(espaco * (i + 0.5));
            if (i % passoDia == 0) {
                String dia = ROTULO_DIA.format(dado.dia());
                g.setColor(COR_TEXTO);
                g.drawString(dia, centro - fonte.stringWidth(dia) / 2, areaY + areaA + 18);
            }
            int alturaBarra = (int) Math.round(areaA * (dado.total().doubleValue() / maximo));
            if (alturaBarra <= 0) {
                continue;
            }
            int x = centro - larguraBarra / 2;
            int y = areaY + areaA - alturaBarra;
            g.setColor(COR_BARRA);
            g.fillRect(x, y, larguraBarra, alturaBarra);
            barras.add(new Barra(x, y, larguraBarra, alturaBarra, dado));

            if (rotulosValor && larguraBarra >= 18 && y - fonte.getAscent() > margemTopo + 4) {
                String valor = MOEDA_SEM_CENTAVOS.format(dado.total());
                g.setColor(COR_TEXTO);
                g.drawString(valor, centro - fonte.stringWidth(valor) / 2, y - 4);
            }
        }

        g.setColor(COR_GRID);
        g.drawLine(areaX, areaY + areaA, areaX + areaL, areaY + areaA);
    }

    private String rotuloCompacto(double valor) {
        if (valor >= 1_000_000) {
            return String.format(PT_BR, "R$ %.1f mi", valor / 1_000_000);
        }
        if (valor >= 1_000) {
            return String.format(PT_BR, "R$ %.1f mil", valor / 1_000);
        }
        return MOEDA.format(valor);
    }
}
