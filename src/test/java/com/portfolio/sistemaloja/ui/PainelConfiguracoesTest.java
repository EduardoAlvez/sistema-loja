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

        assertEquals("1", painel.pontosPorRealTexto());
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

    @Test
    void desabilitaEdicaoParaNaoAdmin() {
        Aplicacao app = BancoTeste.criar("cfgpainelnaoadmin" + System.nanoTime());
        app.getAuth().autenticar("operador", "caixa123");

        PainelConfiguracoes painel = new PainelConfiguracoes(app);

        assertFalse(painel.pontosPorRealEditavel());
        assertFalse(painel.valorPorPontoEditavel());
        assertFalse(painel.salvarHabilitado());
        assertTrue(painel.textoAviso().contains("administradores"), painel.textoAviso());
    }

    @Test
    void gerenteNaoEditaPoisSoAdminAlteraConfiguracoes() {
        Aplicacao app = BancoTeste.criar("cfgpainelgerente" + System.nanoTime());
        app.getUsuarios().criar("Gerente Painel", "gerentepainel", "gerente123",
                com.portfolio.sistemaloja.model.PerfilUsuario.GERENTE);
        app.getAuth().autenticar("gerentepainel", "gerente123");

        PainelConfiguracoes painel = new PainelConfiguracoes(app);

        assertFalse(painel.salvarHabilitado(),
                "o servico e exigirAdmin (isAdmin estrito); a UI nao pode insinuar que gerente edita");
        assertFalse(painel.pontosPorRealEditavel());
    }

    @Test
    void abaConfiguracoesEhSoAdmin() {
        Aplicacao app = BancoTeste.criar("cfgvisibilidade" + System.nanoTime());
        com.portfolio.sistemaloja.model.Usuario admin = app.getAuth().autenticar("admin", "admin123");
        com.portfolio.sistemaloja.model.Usuario gerente = app.getUsuarios().criar("Gerente Aba",
                "gerenteaba", "gerente123", com.portfolio.sistemaloja.model.PerfilUsuario.GERENTE);
        com.portfolio.sistemaloja.model.Usuario operador = app.getAuth().autenticar("operador", "caixa123");

        assertTrue(TelaPrincipal.deveMostrarConfiguracoes(admin));
        assertFalse(TelaPrincipal.deveMostrarConfiguracoes(gerente));
        assertFalse(TelaPrincipal.deveMostrarConfiguracoes(operador));
        assertFalse(TelaPrincipal.deveMostrarConfiguracoes(null));
    }
}
