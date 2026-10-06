package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.repository.EventoAuditoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditoriaServiceTest {

    private Aplicacao app;

    @BeforeEach
    void preparar() {
        app = BancoTeste.criar("auditoria" + System.nanoTime());
    }

    @Test
    void gravaComOUsuarioDaSessaoQuandoAutenticado() {
        app.getAuth().autenticar("admin", "admin123");

        app.getAuditoria().registrar("PRODUTO_SALVO", "PRODUTO", 7L, "Refrigerante Cola 2L");

        List<EventoAuditoria> eventos = hoje();
        assertEquals(1, eventos.size());
        assertEquals("admin", eventos.get(0).usuarioLogin());
        assertEquals("PRODUTO_SALVO", eventos.get(0).acao());
        assertEquals("PRODUTO", eventos.get(0).entidade());
        assertEquals(7L, eventos.get(0).entidadeId());
        assertEquals("Refrigerante Cola 2L", eventos.get(0).detalhe());
    }

    @Test
    void gravaComoSistemaQuandoNaoHaSessao() {
        app.getAuditoria().registrar("ACAO_AUTOMATICA", null, null, null);

        List<EventoAuditoria> eventos = hoje();
        assertEquals(1, eventos.size());
        assertEquals("sistema", eventos.get(0).usuarioLogin());
    }

    @Test
    void falhaNaGravacaoNaoPropagaErro() {
        String acaoLongaDemais = "A".repeat(100);

        assertDoesNotThrow(() -> app.getAuditoria()
                .registrarComo("admin", acaoLongaDemais, null, null, null));
    }

    @Test
    void filtraPeriodoEOrdenaDaMaisRecente() {
        app.getAuditoria().registrarComo("admin", "PRIMEIRA", "VENDA", 1L, null);
        app.getAuditoria().registrarComo("operador", "SEGUNDA", "VENDA", 2L, null);

        List<EventoAuditoria> eventos = hoje();
        assertEquals(2, eventos.size());
        assertEquals("SEGUNDA", eventos.get(0).acao());
        assertEquals("PRIMEIRA", eventos.get(1).acao());

        LocalDate amanha = LocalDate.now().plusDays(1);
        assertTrue(app.getAuditoria().listar(amanha, amanha).isEmpty(),
                "fora do periodo nao deve retornar eventos");
    }

    private List<EventoAuditoria> hoje() {
        LocalDate hoje = LocalDate.now();
        return app.getAuditoria().listar(hoje, hoje);
    }
}
