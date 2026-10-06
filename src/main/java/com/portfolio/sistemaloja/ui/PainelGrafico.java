package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.grafico.GraficoFaturamento;
import com.portfolio.sistemaloja.repository.VendaDoDia;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.util.List;

public class PainelGrafico extends JPanel {

    private static final long serialVersionUID = 1L;

    private final transient GraficoFaturamento grafico = new GraficoFaturamento();
    private List<VendaDoDia> dados = List.of();
    private transient GraficoFaturamento.Desenho desenho;

    public PainelGrafico() {
        setBackground(GraficoFaturamento.COR_FUNDO);
        setBorder(BorderFactory.createLineBorder(new Color(226, 230, 234)));
        setToolTipText("");
        getAccessibleContext().setAccessibleDescription(
                "Gráfico de barras com o faturamento por dia do período selecionado. "
                        + "Os mesmos valores estão na tabela da aba Vendas.");
    }

    public void setDados(List<VendaDoDia> dados) {
        this.dados = List.copyOf(dados);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graficos) {
        super.paintComponent(graficos);
        desenho = grafico.render(getWidth(), getHeight(), dados);
        graficos.drawImage(desenho.imagem(), 0, 0, null);
    }

    @Override
    public String getToolTipText(MouseEvent evento) {
        if (desenho == null) {
            return null;
        }
        for (GraficoFaturamento.Barra barra : desenho.barras()) {
            boolean dentroX = evento.getX() >= barra.x() && evento.getX() <= barra.x() + barra.largura();
            boolean dentroY = evento.getY() >= barra.y() && evento.getY() <= barra.y() + barra.altura();
            if (dentroX && dentroY) {
                VendaDoDia dado = barra.dado();
                String vendas = dado.quantidade() == 1 ? "1 venda" : dado.quantidade() + " vendas";
                return Ui.data(dado.dia()) + " — " + Ui.moeda(dado.total()) + " (" + vendas + ")";
            }
        }
        return null;
    }
}
