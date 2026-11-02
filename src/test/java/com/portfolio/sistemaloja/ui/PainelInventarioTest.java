package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.PerfilUsuario;
import com.portfolio.sistemaloja.model.Produto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.JTable;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PainelInventarioTest {

    private Aplicacao app;
    private Produto refrigerante;

    @BeforeEach
    void preparar() {
        app = BancoTeste.criar("painelinventario" + System.nanoTime());
        refrigerante = app.getProdutos().buscarPorCodigoBarras("7891000100103");
        app.getUsuarios().criar("Gerente Loja", "gerente", "gerente123", PerfilUsuario.GERENTE);
        app.getAuth().autenticar("gerente", "gerente123");
    }

    @Test
    void exibeEstadoVazioQuandoProdutoNaoTemPerdas() {
        PainelInventario painel = new PainelInventario(app);

        assertEquals(0, painel.totalLinhas());
        assertTrue(painel.textoAlerta().contains("Nenhuma perda registrada"), painel.textoAlerta());
    }

    @Test
    void exibeHistoricoComMotivoQuantidadeEResponsavel() {
        app.getInventario().registrarPerda(refrigerante.getId(), 2, "AVARIA", "Embalagem violada");

        PainelInventario painel = new PainelInventario(app);
        painel.selecionarProduto(refrigerante.getId());

        assertEquals(1, painel.totalLinhas());
        assertEquals("AVARIA", painel.valor(0, 1));
        assertEquals(2, painel.valor(0, 2));
        assertEquals("Gerente Loja", painel.valor(0, 3));
        assertEquals("Embalagem violada", painel.valor(0, 4));
        assertTrue(painel.textoAlerta().contains("1 perda"), painel.textoAlerta());
    }

    @Test
    void camposDoFormularioTemNomeAcessivel() {
        PainelInventario painel = new PainelInventario(app);

        List<Component> campos = componentesComRotulo(painel);
        assertFalse(campos.isEmpty(), "deveria haver campos com rótulo associado");

        for (Component campo : campos) {
            javax.accessibility.AccessibleContext contexto = campo.getAccessibleContext();
            assertNotNull(contexto.getAccessibleName(),
                    "campo sem nome acessível: " + campo.getClass().getSimpleName());
            assertFalse(contexto.getAccessibleName().isBlank(),
                    "campo com nome acessível vazio: " + campo.getClass().getSimpleName());
        }
    }

    @Test
    void tabelaUsaDesignSystemDoProjeto() {
        PainelInventario painel = new PainelInventario(app);

        JTable tabela = tabelaDe(painel);
        assertNotNull(tabela);
        assertEquals(26, tabela.getRowHeight());
        assertTrue(tabela.getTableHeader().getReorderingAllowed() == false);
    }

    @Test
    void alertaFicaVisivelComMensagemDeEstadoVazio() {
        PainelInventario painel = new PainelInventario(app);

        JLabel alerta = painel.componenteAlerta();
        assertNotNull(alerta);
        assertFalse(alerta.getText().isBlank());
    }

    @Test
    void painelTemDuasAbasComTitulos() {
        PainelInventario painel = new PainelInventario(app);

        javax.swing.JTabbedPane abas = abaDe(painel);
        assertNotNull(abas, "o painel do inventário deve ter abas");
        assertEquals(2, abas.getTabCount());
        assertEquals("Perdas de estoque", abas.getTitleAt(0));
        assertEquals("Contagem cíclica", abas.getTitleAt(1));
    }

    @Test
    void tituloDasPerdasContinuaAposMontarOTab() {
        PainelInventario painel = new PainelInventario(app);

        JLabel titulo = painel.componenteTituloPerdas();
        assertNotNull(titulo, "título não deve ter sido descartado do layout");
        assertNotNull(titulo.getParent(), "o título precisa estar num container para ser desenhado");
    }

    @Test
    void tituloDasPerdasEDesenhadoComLarguraReal() {
        org.junit.jupiter.api.Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(),
                "largura real exige um frame decorado");
        PainelInventario painel = new PainelInventario(app);

        javax.swing.JFrame quadro = new javax.swing.JFrame();
        try {
            quadro.setUndecorated(true);
            quadro.add(painel);
            quadro.pack();
            quadro.validate();
            assertTrue(painel.componenteTituloPerdas().getWidth() > 0,
                    "o título das perdas deve ter largura maior que zero");
        } finally {
            quadro.dispose();
        }
    }

    private javax.swing.JTabbedPane abaDe(Container raiz) {
        for (Component filho : raiz.getComponents()) {
            if (filho instanceof javax.swing.JTabbedPane abas) {
                return abas;
            }
            if (filho instanceof Container container) {
                javax.swing.JTabbedPane achada = abaDe(container);
                if (achada != null) {
                    return achada;
                }
            }
        }
        return null;
    }

    private List<Component> componentesComRotulo(Container raiz) {
        List<Component> encontrados = new ArrayList<>();
        for (Component filho : raiz.getComponents()) {
            if (filho instanceof JLabel rotulo && rotulo.getLabelFor() != null) {
                encontrados.add(rotulo.getLabelFor());
            }
            if (filho instanceof Container container) {
                encontrados.addAll(componentesComRotulo(container));
            }
        }
        return encontrados;
    }

    private JTable tabelaDe(Container raiz) {
        for (Component filho : raiz.getComponents()) {
            if (filho instanceof JTable tabela) {
                return tabela;
            }
            if (filho instanceof Container container) {
                JTable achada = tabelaDe(container);
                if (achada != null) {
                    return achada;
                }
            }
        }
        return null;
    }
}
