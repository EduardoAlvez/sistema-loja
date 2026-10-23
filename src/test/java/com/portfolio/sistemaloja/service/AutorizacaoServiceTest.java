package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.Autorizacao;
import com.portfolio.sistemaloja.model.MotivoAutorizacao;
import com.portfolio.sistemaloja.model.PerfilUsuario;
import com.portfolio.sistemaloja.repository.EventoAuditoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AutorizacaoServiceTest {

    private Aplicacao app;

    @BeforeEach
    void preparar() {
        app = BancoTeste.criar("aut" + System.nanoTime());
        app.getAuth().autenticar("operador", "caixa123");
    }

    @Test
    void autorizaSupervisorComPinValido() {
        Autorizacao autorizacao = app.getAutorizacoes().autorizar("admin", "1234",
                MotivoAutorizacao.CANCELAMENTO_VENDA);

        assertNotNull(autorizacao);
        assertEquals("admin", autorizacao.autorizadorLogin());
        assertEquals(MotivoAutorizacao.CANCELAMENTO_VENDA, autorizacao.motivo());
    }

    @Test
    void rejeitaPinInvalido() {
        assertThrows(ValidacaoException.class,
                () -> app.getAutorizacoes().autorizar("admin", "0000",
                        MotivoAutorizacao.CANCELAMENTO_VENDA));
    }

    @Test
    void rejeitaLoginInexistente() {
        assertThrows(ValidacaoException.class,
                () -> app.getAutorizacoes().autorizar("naoexiste", "1234",
                        MotivoAutorizacao.CANCELAMENTO_VENDA));
    }

    @Test
    void naoPermiteOperadorAutorizarASiMesmo() {
        app.getAuth().autenticar("admin", "admin123");
        assertThrows(ValidacaoException.class,
                () -> app.getAutorizacoes().autorizar("admin", "1234",
                        MotivoAutorizacao.SANGRIA));
    }

    @Test
    void rejeitaSupervisorSemPinCadastrado() {
        app.getUsuarios().criar("Supervisor Sem Pin", "super", "senha123456",
                PerfilUsuario.OPERADOR, BigDecimal.ZERO);
        app.getAuth().autenticar("super", "senha123456");

        assertThrows(ValidacaoException.class,
                () -> app.getAutorizacoes().autorizar("super", "1234", MotivoAutorizacao.SANGRIA));
    }

    @Test
    void rejeitaNivelSemAlcadaSuperior() {
        app.getUsuarios().criar("Caixa Dois", "caixa2", "senha123456",
                PerfilUsuario.OPERADOR, BigDecimal.ZERO);
        app.getAuth().autenticar("caixa2", "senha123456");

        assertThrows(ValidacaoException.class,
                () -> app.getAutorizacoes().autorizar("operador", "5678",
                        MotivoAutorizacao.CANCELAMENTO_VENDA));
    }

    @Test
    void exigirLancaQuandoNaoHaToken() {
        assertThrows(AutorizacaoNecessariaException.class,
                () -> app.getAutorizacoes().exigir(null, MotivoAutorizacao.SANGRIA));
    }

    @Test
    void adminNaoPrecisaDeTokenDeAutorizacao() {
        app.getAuth().autenticar("admin", "admin123");
        app.getAutorizacoes().exigir(null, MotivoAutorizacao.CANCELAMENTO_VENDA);
    }

    @Test
    void registraEventoQuandoAutorizacaoEhNegada() {
        assertThrows(ValidacaoException.class,
                () -> app.getAutorizacoes().autorizar("admin", "0000",
                        MotivoAutorizacao.SANGRIA));

        List<EventoAuditoria> eventos = app.getAuditoria().listar(LocalDate.now(), LocalDate.now());
        assertTrue(eventos.stream().anyMatch(e -> "AUTORIZACAO_NEGADA".equals(e.acao())),
                "evento de negativa ausente: " + eventos);
    }
}
