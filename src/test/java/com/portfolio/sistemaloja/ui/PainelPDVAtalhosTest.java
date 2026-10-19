package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.Usuario;
import org.junit.jupiter.api.Test;

import java.awt.event.KeyEvent;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.KeyStroke;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PainelPDVAtalhosTest {

    @Test
    void atalhosDeF3AF12EstaoInstaladosNoPainel() {
        Aplicacao app = BancoTeste.criar("pdvatalhos" + System.nanoTime());
        Usuario operador = app.getAuth().autenticar("operador", "caixa123");

        PainelPDV painel = new PainelPDV(app, operador);

        Map<Integer, String> atalhos = painel.atalhos();
        assertEquals(10, atalhos.size());
        for (int tecla = KeyEvent.VK_F3; tecla <= KeyEvent.VK_F12; tecla++) {
            assertTrue(atalhos.containsKey(tecla),
                    "Falta atalho para a tecla " + KeyEvent.getKeyText(tecla));
            Object nome = painel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .get(KeyStroke.getKeyStroke(tecla, 0));
            assertNotNull(nome, "Tecla sem vinculo registrado: " + KeyEvent.getKeyText(tecla));
            assertNotNull(painel.getActionMap().get(nome), "Tecla sem acao: " + KeyEvent.getKeyText(tecla));
        }
        assertTrue(atalhos.get(KeyEvent.VK_F12).contains("Finalizar"));
        assertTrue(atalhos.get(KeyEvent.VK_F5).contains("Pausar"));
    }
}
