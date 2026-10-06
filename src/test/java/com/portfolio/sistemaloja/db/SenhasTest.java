package com.portfolio.sistemaloja.db;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SenhasTest {

    @Test
    void geraSalDiferenteParaCadaUsuario() {
        assertNotEquals(Senhas.gerarSal(), Senhas.gerarSal());
    }

    @Test
    void hashEhDeterministicoParaMesmoSal() {
        String sal = Senhas.gerarSal();
        assertEqualsHash(Senhas.hash("senha123", sal), Senhas.hash("senha123", sal));
    }

    @Test
    void confereSenhaCorretaERejeitaErrada() {
        String sal = Senhas.gerarSal();
        String hash = Senhas.hash("admin123", sal);
        assertTrue(Senhas.confere("admin123", sal, hash));
        assertFalse(Senhas.confere("errada", sal, hash));
        assertFalse(Senhas.confere(null, sal, hash));
        assertFalse(Senhas.confere("admin123", null, hash));
    }

    private void assertEqualsHash(String esperado, String obtido) {
        org.junit.jupiter.api.Assertions.assertEquals(esperado, obtido);
    }
}
