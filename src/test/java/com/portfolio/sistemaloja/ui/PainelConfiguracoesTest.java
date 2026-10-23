package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PainelConfiguracoesTest {

    @AfterEach
    void limpar() {
        System.clearProperty("fidelidade.pontosPorReal");
        System.clearProperty("fidelidade.valorPorPonto");
        System.clearProperty("pix.chave");
        System.clearProperty("pix.nome");
        System.clearProperty("pix.cidade");
    }

    @Test
    void carregaOsValoresDoBanco() {
        Aplicacao app = BancoTeste.criar("cfgpanel" + System.nanoTime());
        app.getAuth().autenticar("admin", "admin123");

        PainelConfiguracoes painel = new PainelConfiguracoes(app);

        assertEquals("1.000000", painel.pontosPorRealTexto());
        assertEquals(0, new BigDecimal("0.05").compareTo(new BigDecimal(painel.valorPorPontoTexto())));
        assertTrue(painel.pontosPorRealEditavel());
    }

    @Test
    void desabilitaCampoSobrescritoPorPropriedade() {
        Aplicacao app = BancoTeste.criar("cfgpanelenv" + System.nanoTime());
        app.getAuth().autenticar("admin", "admin123");
        System.setProperty("fidelidade.valorPorPonto", "0.20");

        PainelConfiguracoes painel = new PainelConfiguracoes(app);

        assertFalse(painel.valorPorPontoEditavel());
        assertTrue(painel.pontosPorRealEditavel());
    }
}
