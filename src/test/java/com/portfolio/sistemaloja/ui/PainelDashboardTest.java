package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.repository.VendaDoDia;
import org.junit.jupiter.api.Test;

import java.awt.Component;
import java.awt.Container;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PainelDashboardTest {

    @Test
    void exibeGraficoComOsUltimos14DiasAbaixoDasTabelas() {
        Aplicacao app = BancoTeste.criar("dash" + System.nanoTime());

        PainelDashboard painel = new PainelDashboard(app);
        PainelGrafico grafico = encontrar(painel, PainelGrafico.class);

        assertNotNull(grafico, "dashboard deve conter o grafico de faturamento");
        List<VendaDoDia> dados = grafico.getDados();
        assertEquals(14, dados.size(), "grafico deve cobrir 14 dias");
        assertEquals(LocalDate.now(), dados.get(dados.size() - 1).dia(),
                "o ultimo dia do grafico deve ser hoje");
    }

    private <T> T encontrar(Component raiz, Class<T> tipo) {
        if (tipo.isInstance(raiz)) {
            return tipo.cast(raiz);
        }
        if (raiz instanceof Container container) {
            for (Component filho : container.getComponents()) {
                T achado = encontrar(filho, tipo);
                if (achado != null) {
                    return achado;
                }
            }
        }
        return null;
    }
}
