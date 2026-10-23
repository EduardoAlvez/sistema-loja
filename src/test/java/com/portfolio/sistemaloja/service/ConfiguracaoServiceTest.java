package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.repository.EventoAuditoria;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguracaoServiceTest {

    private Aplicacao app;

    @BeforeEach
    void preparar() {
        app = BancoTeste.criar("config" + System.nanoTime());
    }

    @AfterEach
    void limpar() {
        System.clearProperty("fidelidade.pontosPorReal");
        System.clearProperty("fidelidade.valorPorPonto");
        System.clearProperty("pix.chave");
        System.clearProperty("pix.nome");
        System.clearProperty("pix.cidade");
    }

    @Test
    void carregaOsPadroesSemeAdosNaV16() {
        FidelidadeConfig config = app.getConfiguracoes().atual().fidelidade();

        assertEquals(0, new BigDecimal("1").compareTo(config.pontosPorReal()));
        assertEquals(0, new BigDecimal("0.05").compareTo(config.valorPorPonto()));
        assertEquals("pagamentos@lojaexemplo.com.br", app.getConfiguracoes().atual().pix().chave());
    }

    @Test
    void salvaNoBancoEEfeITOaplicadoNaHoraNaFidelidade() {
        app.getAuth().autenticar("admin", "admin123");
        app.getConfiguracoes().salvarLoja(new FidelidadeConfig(new BigDecimal("2"), new BigDecimal("0.25")),
                app.getConfiguracoes().atual().pix());

        assertEquals(0, new BigDecimal("2").compareTo(app.getConfiguracoes().atual().fidelidade().pontosPorReal()));
        assertEquals(14, app.getFidelidade().calcularCredito(new BigDecimal("7.00")));
        assertEquals(0, new BigDecimal("0.50").compareTo(app.getFidelidade().valorDosPontos(2)));
    }

    @Test
    void precedenciaDaPropriedadeVenceOBanco() {
        app.getAuth().autenticar("admin", "admin123");
        app.getConfiguracoes().salvarLoja(new FidelidadeConfig(new BigDecimal("1"), new BigDecimal("0.10")),
                app.getConfiguracoes().atual().pix());

        System.setProperty("fidelidade.valorPorPonto", "0.20");
        app.getConfiguracoes().invalidar();

        assertEquals(0, new BigDecimal("0.20").compareTo(app.getConfiguracoes().atual().fidelidade().valorPorPonto()));
    }

    @Test
    void rejeitaConfiguracaoEfetivaInvalidaComOverrideDeAmbiente() {
        app.getAuth().autenticar("admin", "admin123");
        System.setProperty("fidelidade.pontosPorReal", "10");

        assertThrows(ValidacaoException.class, () -> app.getConfiguracoes().salvarLoja(
                new FidelidadeConfig(new BigDecimal("1"), new BigDecimal("0.20")),
                app.getConfiguracoes().atual().pix()));
    }

    @Test
    void soAdminAlteraConfiguracoes() {
        app.getAuth().autenticar("operador", "caixa123");

        assertThrows(ValidacaoException.class, () -> app.getConfiguracoes().salvarLoja(
                new FidelidadeConfig(new BigDecimal("1"), new BigDecimal("0.05")),
                app.getConfiguracoes().atual().pix()));
    }

    @Test
    void auditaSomenteAsChavesQueMudaram() {
        app.getAuth().autenticar("admin", "admin123");
        app.getConfiguracoes().salvarLoja(new FidelidadeConfig(new BigDecimal("1"), new BigDecimal("0.10")),
                app.getConfiguracoes().atual().pix());

        List<EventoAuditoria> eventos = hoje();
        List<String> detalhes = eventos.stream()
                .filter(e -> "CONFIG_ALTERADA".equals(e.acao()))
                .map(EventoAuditoria::detalhe)
                .toList();

        assertEquals(1, detalhes.size(), "so o valorPorPonto mudou: " + detalhes);
        assertTrue(detalhes.get(0).contains("fidelidade.valorPorPonto"));
        assertEquals("admin", eventos.stream()
                .filter(e -> "CONFIG_ALTERADA".equals(e.acao()))
                .findFirst().orElseThrow().usuarioLogin());
    }

    @Test
    void naoPersisteValorQueVeiodoAmbiente() {
        app.getAuth().autenticar("admin", "admin123");
        System.setProperty("fidelidade.valorPorPonto", "0.20");
        app.getConfiguracoes().invalidar();

        app.getConfiguracoes().salvarLoja(
                new FidelidadeConfig(new BigDecimal("2"), new BigDecimal("0.20")),
                app.getConfiguracoes().atual().pix());

        System.clearProperty("fidelidade.valorPorPonto");
        app.getConfiguracoes().invalidar();
        FidelidadeConfig gravada = app.getConfiguracoes().atual().fidelidade();
        assertEquals(2, gravada.pontosPorReal().intValue());
        assertEquals(0, new BigDecimal("0.05").compareTo(gravada.valorPorPonto()),
                "o valor do ambiente nao pode ser gravado no banco");
    }

    @Test
    void desfazAGravacaoQuandoAConfiguracaoEfetivaEIinvalida() {
        app.getAuth().autenticar("admin", "admin123");
        System.setProperty("fidelidade.valorPorPonto", "0.20");

        assertThrows(ValidacaoException.class, () -> app.getConfiguracoes().salvarLoja(
                new FidelidadeConfig(new BigDecimal("10"), new BigDecimal("0.20")),
                app.getConfiguracoes().atual().pix()));

        System.clearProperty("fidelidade.valorPorPonto");
        app.getConfiguracoes().invalidar();
        assertEquals(0, new BigDecimal("1").compareTo(
                app.getConfiguracoes().atual().fidelidade().pontosPorReal()));
    }

    @Test
    void exigeSessaoAutenticada() {
        assertThrows(ValidacaoException.class, () -> app.getConfiguracoes().salvarLoja(
                new FidelidadeConfig(new BigDecimal("1"), new BigDecimal("0.05")),
                app.getConfiguracoes().atual().pix()));
    }

    @Test
    void recusaNumeroInvalidoVindoDoAmbiente() {
        System.setProperty("fidelidade.valorPorPonto", "abc");
        app.getConfiguracoes().invalidar();

        assertThrows(ValidacaoException.class, () -> app.getConfiguracoes().atual());
    }

    @Test
    void recusaPixComCampoAcimaDoLimite() {
        app.getAuth().autenticar("admin", "admin123");

        assertThrows(ValidacaoException.class, () -> app.getConfiguracoes().salvarLoja(
                new FidelidadeConfig(new BigDecimal("1"), new BigDecimal("0.05")),
                new ConfiguracaoPix("chave@loja.com", "NOME DO RECEBEDOR BEM COMPRIDO", "SAO PAULO")));
    }

    private List<EventoAuditoria> hoje() {
        LocalDate hoje = LocalDate.now();
        return app.getAuditoria().listar(hoje, hoje);
    }
}
