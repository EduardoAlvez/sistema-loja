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

class PainelContagemTest {

    private Aplicacao app;
    private Produto refrigerante;

    @BeforeEach
    void preparar() {
        app = BancoTeste.criar("painelcontagem" + System.nanoTime());
        refrigerante = app.getProdutos().buscarPorCodigoBarras("7891000100103");
        app.getUsuarios().criar("Gerente Loja", "gerente", "gerente123", PerfilUsuario.GERENTE);
        app.getAuth().autenticar("gerente", "gerente123");
    }

    @Test
    void exibeEstadoVazioQuandoNaoHaContagemAberta() {
        PainelContagem painel = new PainelContagem(app);

        assertEquals(0, painel.totalLinhas());
        assertTrue(painel.textoSituacao().contains("Nenhuma contagem em aberto"), painel.textoSituacao());
        assertEquals(0, painel.totalLinhasHistorico());
    }

    @Test
    void exibeOsItensDaContagemAbertaComEstoqueEResponsavel() {
        app.getInventario().abrirContagem(List.of(refrigerante.getId()));

        PainelContagem painel = new PainelContagem(app);

        assertEquals(1, painel.totalLinhas());
        assertTrue(painel.valor(0, 0).toString().contains("Refrigerante"), painel.valor(0, 0).toString());
        assertEquals(40, painel.valor(0, 1));
        assertEquals("-", painel.valor(0, 2));
        assertEquals("-", painel.valor(0, 3));
        assertEquals("Não contado", painel.valor(0, 4));
        assertTrue(painel.textoSituacao().contains("INV-"), painel.textoSituacao());
        assertTrue(painel.textoSituacao().contains("Gerente Loja"), painel.textoSituacao());
    }

    @Test
    void mostraQuantidadeContadaEPrevisaoDeDiferencaAposInformar() {
        long contagemId = app.getInventario().abrirContagem(List.of(refrigerante.getId()));
        app.getInventario().informarContagem(contagemId, refrigerante.getId(), 45);

        PainelContagem painel = new PainelContagem(app);

        assertEquals(45, painel.valor(0, 2));
        assertEquals("+5", painel.valor(0, 3));
        assertEquals("Contado", painel.valor(0, 4));
        assertTrue(painel.textoProgresso().contains("1 de 1"), painel.textoProgresso());
    }

    @Test
    void historicoMostraAContagemFechada() {
        long contagemId = app.getInventario().abrirContagem(List.of(refrigerante.getId()));
        app.getInventario().informarContagem(contagemId, refrigerante.getId(), 40);
        app.getInventario().fecharContagem(contagemId, "Fim do ciclo");

        PainelContagem painel = new PainelContagem(app);

        assertEquals(1, painel.totalLinhasHistorico());
        assertTrue(painel.textoSituacao().contains("Nenhuma contagem em aberto"), painel.textoSituacao());
    }

    @Test
    void camposDoFormularioTemNomeAcessivel() {
        PainelContagem painel = new PainelContagem(app);

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
    void tabelasUsamDesignSystemDoProjeto() {
        PainelContagem painel = new PainelContagem(app);

        List<JTable> tabelas = tabelasDe(painel);
        assertEquals(2, tabelas.size());
        for (JTable tabela : tabelas) {
            assertEquals(26, tabela.getRowHeight());
            assertFalse(tabela.getTableHeader().getReorderingAllowed());
        }
    }

    @Test
    void alertaDeSituacaoFicaVisivelComMensagem() {
        PainelContagem painel = new PainelContagem(app);

        JLabel situacao = painel.componenteSituacao();
        assertNotNull(situacao);
        assertFalse(situacao.getText().isBlank());
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

    private List<JTable> tabelasDe(Container raiz) {
        List<JTable> tabelas = new ArrayList<>();
        for (Component filho : raiz.getComponents()) {
            if (filho instanceof JTable tabela) {
                tabelas.add(tabela);
            }
            if (filho instanceof Container container) {
                tabelas.addAll(tabelasDe(container));
            }
        }
        return tabelas;
    }
}
