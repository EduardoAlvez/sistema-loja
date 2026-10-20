package com.portfolio.sistemaloja.impressao;

import org.junit.jupiter.api.Test;

import javax.print.PrintService;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImpressorasSpoolTest {

    @Test
    void escolheAPrimeiraQuandoNaoHaPreferida() {
        assertEquals("A", ImpressorasSpool.escolher(List.of("A", "B"), null));
        assertEquals("A", ImpressorasSpool.escolher(List.of("A", "B"), "  "));
    }

    @Test
    void escolheIgnorandoCaixaEEspacos() {
        assertEquals("POS-80", ImpressorasSpool.escolher(List.of("POS-80", "B"), " pos-80 "));
    }

    @Test
    void falhaQuandoNaoEncontra() {
        ImpressaoException erro = assertThrows(ImpressaoException.class,
                () -> ImpressorasSpool.escolher(List.of("A", "B"), "C"));

        assertTrue(erro.getMessage().contains("A, B"), erro.getMessage());
    }

    @Test
    void falhaQuandoNaoHaImpressora() {
        assertThrows(ImpressaoException.class, () -> ImpressorasSpool.escolher(List.of(), null));
        assertTrue(new ImpressorasSpool(() -> new PrintService[0]).nomes().isEmpty());
        assertThrows(ImpressaoException.class, () -> new ImpressorasSpool(() -> new PrintService[0])
                .selecionar(null));
    }
}
