package com.portfolio.sistemaloja.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CpfTest {

    @Test
    void aceitaCpfValidoComMascara() {
        assertTrue(Cpf.valido("529.982.247-25"));
    }

    @Test
    void aceitaCpfValidoSoComNumeros() {
        assertTrue(Cpf.valido("52998224725"));
    }

    @Test
    void rejeitaDigitoVerificadorErrado() {
        assertFalse(Cpf.valido("529.982.247-26"));
    }

    @Test
    void rejeitaSequenciaRepetida() {
        assertFalse(Cpf.valido("111.111.111-11"));
        assertFalse(Cpf.valido("00000000000"));
    }

    @Test
    void rejeitaTamanhoErrado() {
        assertFalse(Cpf.valido("1234567890"));
        assertFalse(Cpf.valido(""));
        assertFalse(Cpf.valido(null));
    }

    @Test
    void limpaEFormata() {
        assertEquals("52998224725", Cpf.limpar("529.982.247-25"));
        assertEquals("529.982.247-25", Cpf.formatar("52998224725"));
    }
}
