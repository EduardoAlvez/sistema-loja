package com.portfolio.sistemaloja.impressao;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImpressoraMemoriaTest {

    @Test
    void guardaCadaTrabalhoImpresso() {
        ImpressoraMemoria impressora = new ImpressoraMemoria("Teste");
        impressora.imprimir(new byte[] {1, 2});
        impressora.imprimir(new byte[] {3});

        assertEquals("Teste", impressora.nome());
        assertEquals(2, impressora.total());
        assertArrayEquals(new byte[] {3}, impressora.ultimoTrabalho());
    }

    @Test
    void naoDeixaAlterarOTrabalhoGuardado() {
        ImpressoraMemoria impressora = new ImpressoraMemoria();
        byte[] dados = {1, 2};
        impressora.imprimir(dados);
        dados[0] = 9;

        assertArrayEquals(new byte[] {1, 2}, impressora.ultimoTrabalho());
    }

    @Test
    void falhaAoConsultarSemTrabalho() {
        assertThrows(IllegalStateException.class, () -> new ImpressoraMemoria().ultimoTrabalho());
    }
}
