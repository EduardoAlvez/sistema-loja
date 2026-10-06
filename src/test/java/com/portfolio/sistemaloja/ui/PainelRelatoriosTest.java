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

class PainelRelatoriosTest {

    @Test
    void exibeAbaAuditoriaSomenteParaAdmin() {
        Aplicacao adminApp = BancoTeste.criar("reladmin" + System.nanoTime());
        adminApp.getAuth().autenticar("admin", "admin123");
        assertEquals(5, abas(new PainelRelatorios(adminApp)).getTabCount(),
                "admin deve ver a aba Auditoria");

        Aplicacao operadorApp = BancoTeste.criar("relop" + System.nanoTime());
        operadorApp.getAuth().autenticar("operador", "caixa123");
        assertEquals(4, abas(new PainelRelatorios(operadorApp)).getTabCount(),
                "operador nao deve ver a aba Auditoria");
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
