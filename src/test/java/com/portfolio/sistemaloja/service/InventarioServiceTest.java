package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.PerdaEstoque;
import com.portfolio.sistemaloja.model.PerfilUsuario;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventarioServiceTest {

    private Aplicacao app;
    private Produto refrigerante;

    @BeforeEach
    void preparar() {
        app = BancoTeste.criar("inventario" + System.nanoTime());
        refrigerante = app.getProdutos().buscarPorCodigoBarras("7891000100103");
    }

    private Usuario autenticarGerente() {
        app.getUsuarios().criar("Gerente Loja", "gerente", "gerente123", PerfilUsuario.GERENTE);
        return app.getAuth().autenticar("gerente", "gerente123");
    }

    @Test
    void gerenteRegistraPerdaEBaixaEstoque() {
        autenticarGerente();
        int estoqueInicial = refrigerante.getEstoque();

        app.getInventario().registrarPerda(refrigerante.getId(), 3, "QUEBRA", "Caixa amassada");

        assertEquals(estoqueInicial - 3, estoqueAtual());
        List<PerdaEstoque> perdas = app.getInventario().listarPerdas(refrigerante.getId());
        assertEquals(1, perdas.size());
        assertEquals(3, perdas.get(0).getQuantidade());
        assertEquals("QUEBRA", perdas.get(0).getMotivo());
        assertEquals("Caixa amassada", perdas.get(0).getObservacao());
    }

    @Test
    void adminRegistraPerda() {
        app.getAuth().autenticar("admin", "admin123");
        int estoqueInicial = refrigerante.getEstoque();

        app.getInventario().registrarPerda(refrigerante.getId(), 1, "VENCIMENTO", null);

        assertEquals(estoqueInicial - 1, estoqueAtual());
    }

    @Test
    void operadorNaoRegistraPerda() {
        app.getAuth().autenticar("operador", "caixa123");
        int estoqueInicial = refrigerante.getEstoque();

        ValidacaoException erro = assertThrows(ValidacaoException.class,
                () -> app.getInventario().registrarPerda(refrigerante.getId(), 1, "QUEBRA", null));

        assertTrue(erro.getMessage().contains("GERENTE/ADMIN"));
        assertEquals(estoqueInicial, estoqueAtual());
    }

    @Test
    void semSessaoNaoRegistraPerda() {
        ValidacaoException erro = assertThrows(ValidacaoException.class,
                () -> app.getInventario().registrarPerda(refrigerante.getId(), 1, "QUEBRA", null));

        assertTrue(erro.getMessage().contains("autenticado"));
    }

    @Test
    void operadorNaoListaPerdas() {
        app.getAuth().autenticar("operador", "caixa123");

        assertThrows(ValidacaoException.class, () -> app.getInventario().listarPerdas(refrigerante.getId()));
    }

    @Test
    void rejeitaQuantidadeNaoPositiva() {
        autenticarGerente();

        assertThrows(ValidacaoException.class,
                () -> app.getInventario().registrarPerda(refrigerante.getId(), 0, "QUEBRA", null));
        assertThrows(ValidacaoException.class,
                () -> app.getInventario().registrarPerda(refrigerante.getId(), -5, "QUEBRA", null));
    }

    @Test
    void rejeitaMotivoDesconhecido() {
        autenticarGerente();

        ValidacaoException erro = assertThrows(ValidacaoException.class,
                () -> app.getInventario().registrarPerda(refrigerante.getId(), 1, "MOTIVO_QUALQUER", null));

        assertTrue(erro.getMessage().contains("Motivo inválido"));
    }

    @Test
    void rejeitaMotivoVazio() {
        autenticarGerente();

        ValidacaoException erro = assertThrows(ValidacaoException.class,
                () -> app.getInventario().registrarPerda(refrigerante.getId(), 1, "   ", null));

        assertTrue(erro.getMessage().contains("obrigatório"));
    }

    @Test
    void motivoOutrosExigeObservacao() {
        autenticarGerente();
        int estoqueInicial = refrigerante.getEstoque();

        ValidacaoException erro = assertThrows(ValidacaoException.class,
                () -> app.getInventario().registrarPerda(refrigerante.getId(), 1, "OUTROS", "  "));

        assertTrue(erro.getMessage().contains("observação"));
        assertEquals(estoqueInicial, estoqueAtual());
    }

    @Test
    void motivoOutrosComObservacaoEhAceito() {
        autenticarGerente();

        app.getInventario().registrarPerda(refrigerante.getId(), 1, "OUTROS", "Perda não classificada");

        assertEquals(1, app.getInventario().listarPerdas(refrigerante.getId()).size());
    }

    @Test
    void rejeitaObservacaoAcimaDoLimiteSemAlterarEstoque() {
        autenticarGerente();
        int estoqueInicial = refrigerante.getEstoque();
        String observacaoLonga = "x".repeat(InventarioService.OBSERVACAO_MAX + 1);

        ValidacaoException erro = assertThrows(ValidacaoException.class,
                () -> app.getInventario().registrarPerda(refrigerante.getId(), 1, "QUEBRA", observacaoLonga));

        assertTrue(erro.getMessage().contains("255"));
        assertEquals(estoqueInicial, estoqueAtual());
        assertTrue(app.getInventario().listarPerdas(refrigerante.getId()).isEmpty());
    }

    @Test
    void rejeitaPerdaMaiorQueEstoque() {
        autenticarGerente();
        int estoqueInicial = refrigerante.getEstoque();

        ValidacaoException erro = assertThrows(ValidacaoException.class,
                () -> app.getInventario().registrarPerda(refrigerante.getId(), estoqueInicial + 1, "QUEBRA", null));

        assertTrue(erro.getMessage().contains("Estoque insuficiente"));
        assertEquals(estoqueInicial, estoqueAtual());
    }

    @Test
    void rejeitaProdutoInexistente() {
        autenticarGerente();

        assertThrows(ValidacaoException.class,
                () -> app.getInventario().registrarPerda(999999L, 1, "QUEBRA", null));
    }

    @Test
    void normalizaMotivoParaMaiusculas() {
        autenticarGerente();

        app.getInventario().registrarPerda(refrigerante.getId(), 1, "quebra", null);

        List<PerdaEstoque> perdas = app.getInventario().listarPerdas(refrigerante.getId());
        assertEquals(1, perdas.size());
        assertEquals("QUEBRA", perdas.get(0).getMotivo());
    }

    @Test
    void registraAuditoriaDaPerda() {
        autenticarGerente();

        app.getInventario().registrarPerda(refrigerante.getId(), 2, "AVARIA", "Embalagem violada");

        List<String> acoes = app.getAuditoria().listar(LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(1)).stream()
                .map(evento -> evento.acao())
                .toList();
        assertTrue(acoes.contains("REGISTRAR_PERDA_ESTOQUE"));
    }

    @Test
    void auditoriaECriadaNaMesmaTransacaoDaPerda() {
        autenticarGerente();

        app.getInventario().registrarPerda(refrigerante.getId(), 1, "QUEBRA", "Teste de atomicidade");

        List<String> acoes = app.getAuditoria().listar(LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(1)).stream()
                .map(evento -> evento.acao())
                .toList();
        assertTrue(acoes.contains("REGISTRAR_PERDA_ESTOQUE"));
        assertEquals(1, app.getInventario().listarPerdas(refrigerante.getId()).size());
    }

    @Test
    void historicoMostraNomeDoResponsavel() {
        Usuario gerente = autenticarGerente();

        app.getInventario().registrarPerda(refrigerante.getId(), 1, "QUEBRA", null);

        PerdaEstoque perda = app.getInventario().listarPerdas(refrigerante.getId()).get(0);
        assertEquals(gerente.getId(), perda.getUsuarioId());
        assertEquals("Gerente Loja", perda.getUsuarioNome());
    }

    private int estoqueAtual() {
        return app.getProdutos().buscarPorCodigoBarras("7891000100103").getEstoque();
    }
}
