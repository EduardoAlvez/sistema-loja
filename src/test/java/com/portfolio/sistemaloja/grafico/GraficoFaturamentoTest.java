package com.portfolio.sistemaloja.grafico;

import com.portfolio.sistemaloja.grafico.GraficoFaturamento.Desenho;
import com.portfolio.sistemaloja.repository.VendaDoDia;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraficoFaturamentoTest {

    private final GraficoFaturamento grafico = new GraficoFaturamento();

    @Test
    void renderComListaVaziaNaoFalha() {
        Desenho desenho = grafico.render(600, 240, List.of());

        assertEquals(600, desenho.imagem().getWidth());
        assertEquals(240, desenho.imagem().getHeight());
        assertTrue(desenho.semVendas());
        assertTrue(desenho.barras().isEmpty());
    }

    @Test
    void renderComUmDiaDesenhaUmaBarraPintada() {
        List<VendaDoDia> dados = List.of(
                new VendaDoDia(LocalDate.now(), 3, new BigDecimal("150.00")));

        Desenho desenho = grafico.render(600, 240, dados);

        assertFalse(desenho.semVendas());
        assertEquals(1, desenho.barras().size());
        assertTrue(desenho.barras().get(0).altura() > 0);
        assertTrue(possuiPixelDaBarra(desenho.imagem()), "a barra deve ser pintada na imagem");
    }

    @Test
    void renderComNoventaDiasDesenhaSoDiasComVenda() {
        LocalDate inicio = LocalDate.now().minusDays(89);
        List<VendaDoDia> dados = new ArrayList<>();
        for (int i = 0; i < 90; i++) {
            boolean comVenda = i % 3 == 0;
            dados.add(new VendaDoDia(inicio.plusDays(i),
                    comVenda ? 2 : 0,
                    comVenda ? BigDecimal.valueOf(i * 10 + 10) : BigDecimal.ZERO));
        }

        Desenho desenho = grafico.render(900, 300, dados);

        assertEquals(30, desenho.barras().size());
        assertFalse(desenho.semVendas());
    }

    @Test
    void renderComTodosZerosMarcaEstadoVazio() {
        List<VendaDoDia> dados = List.of(
                new VendaDoDia(LocalDate.now(), 0, BigDecimal.ZERO),
                new VendaDoDia(LocalDate.now().plusDays(1), 0, BigDecimal.ZERO));

        Desenho desenho = grafico.render(600, 240, dados);

        assertTrue(desenho.semVendas());
        assertTrue(desenho.barras().isEmpty());
    }

    @Test
    void renderComEscalaDobraOsPixelsSemMudarAsBarras() {
        List<VendaDoDia> dados = List.of(
                new VendaDoDia(LocalDate.now(), 3, new BigDecimal("150.00")));

        Desenho normal = grafico.render(600, 240, dados);
        Desenho alto = grafico.render(600, 240, dados, 2.0);

        assertEquals(1200, alto.imagem().getWidth(), "pixels dobram na horizontal");
        assertEquals(480, alto.imagem().getHeight(), "pixels dobram na vertical");
        assertEquals(normal.barras(), alto.barras(),
                "barras continuam em unidades do componente para os tooltips");
        assertTrue(possuiPixelDaBarra(alto.imagem()), "a barra deve continuar pintada");
    }

    private boolean possuiPixelDaBarra(BufferedImage imagem) {
        for (int y = 0; y < imagem.getHeight(); y++) {
            for (int x = 0; x < imagem.getWidth(); x++) {
                if (new Color(imagem.getRGB(x, y)).equals(GraficoFaturamento.COR_BARRA)) {
                    return true;
                }
            }
        }
        return false;
    }
}
