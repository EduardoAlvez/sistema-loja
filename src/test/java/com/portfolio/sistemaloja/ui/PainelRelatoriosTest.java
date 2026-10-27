package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import org.junit.jupiter.api.Test;

import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.Component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PainelRelatoriosTest {

    @Test
    void exibeAbaAuditoriaSomenteParaAdmin() {
        Aplicacao adminApp = BancoTeste.criar("reladmin" + System.nanoTime());
        adminApp.getAuth().autenticar("admin", "admin123");
        JTabbedPane abasAdmin = abas(new PainelRelatorios(adminApp));
        assertEquals(8, abasAdmin.getTabCount(), "admin deve ver a aba Auditoria");
        assertEquals("Auditoria", abasAdmin.getTitleAt(abasAdmin.getTabCount() - 1));

        Aplicacao operadorApp = BancoTeste.criar("relop" + System.nanoTime());
        operadorApp.getAuth().autenticar("operador", "caixa123");
        JTabbedPane abasOperador = abas(new PainelRelatorios(operadorApp));
        assertEquals(7, abasOperador.getTabCount(), "operador nao deve ver a aba Auditoria");
        assertEquals("Indicadores", abasOperador.getTitleAt(abasOperador.getTabCount() - 1));
    }

    @Test
    void preencheAbaPorOperadorVaziaQuandoNaoHaVendasNoPeriodo() {
        Aplicacao app = BancoTeste.criar("relopcom" + System.nanoTime());
        var operador = app.getAuth().autenticar("operador", "caixa123");
        app.getUsuarios().definirComissao(operador.getId(), new java.math.BigDecimal("10"));

        PainelRelatorios painel = new PainelRelatorios(app);
        painel.gerar();

        JTabbedPane abas = abas(painel);
        // Encontra a aba "Por operador" (última para operador, antes de Indicadores)
        JTable tabela = null;
        for (int i = 0; i < abas.getTabCount(); i++) {
            if ("Por operador".equals(abas.getTitleAt(i))) {
                tabela = (JTable) ((JScrollPane) abas.getComponentAt(i)).getViewport().getView();
                break;
            }
        }
        assertNotNull(tabela, "aba Por operador deve existir");
        assertEquals(0, tabela.getRowCount(), "operador sem vendas no periodo nao aparece");
    }

    @Test
    void preencheAuditoriaComOsEventosDoPeriodo() {
        Aplicacao app = BancoTeste.criar("relaudit" + System.nanoTime());
        app.getAuth().autenticar("admin", "admin123");
        app.getAuditoria().registrar("PRODUTO_SALVO", "PRODUTO", 7L, "Refrigerante Cola 2L");

        PainelRelatorios painel = new PainelRelatorios(app);
        JTabbedPane abas = abas(painel);
        assertEquals("Auditoria", abas.getTitleAt(abas.getTabCount() - 1));

        JTable tabela = tabelaDaAbaAuditoria(abas);
        assertEquals(2, tabela.getRowCount());
        assertEquals("PRODUTO_SALVO", tabela.getValueAt(0, 2));
        assertEquals("admin", tabela.getValueAt(0, 1));
        assertEquals(7L, tabela.getValueAt(0, 4));
        assertEquals("LOGIN_OK", tabela.getValueAt(1, 2));
    }

    private JTabbedPane abas(PainelRelatorios painel) {
        Component centro = ((BorderLayout) painel.getLayout())
                .getLayoutComponent(BorderLayout.CENTER);
        return (JTabbedPane) centro;
    }

    private JTable tabelaDaAbaAuditoria(JTabbedPane abas) {
        JScrollPane rolagem = (JScrollPane) abas.getComponentAt(abas.getTabCount() - 1);
        return (JTable) rolagem.getViewport().getView();
    }
}
