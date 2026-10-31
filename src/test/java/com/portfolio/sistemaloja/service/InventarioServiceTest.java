package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.PerdaEstoque;
import com.portfolio.sistemaloja.model.PerfilUsuario;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    private Usuario gerente() {
        Usuario gerente = app.getUsuarios().criar("Gerente Loja", "gerente", "gerente123", PerfilUsuario.GERENTE);
        return gerente;
    }

    @Test
    void gerenteRegistraPerdaEBaixaEstoque() {
        Usuario gerente = gerente();
        int estoqueInicial = refrigerante.getEstoque();

        app.getInventario().registrarPerda(gerente, refrigerante.getId(), 3, "QUEBRA", "Caixa amassada");

        assertEquals(estoqueInicial - 3, estoqueAtual());
        List<PerdaEstoque> perdas = app.getInventario().listarPerdas(refrigerante.getId());
        assertEquals(1, perdas.size());
        assertEquals(3, perdas.get(0).getQuantidade());
        assertEquals("QUEBRA", perdas.get(0).getMotivo());
        assertEquals("Caixa amassada", perdas.get(0).getObservacao());
    }

    @Test
    void adminRegistraPerda() {
        Usuario admin = app.getAuth().autenticar("admin", "admin123");
        int estoqueInicial = refrigerante.getEstoque();

        app.getInventario().registrarPerda(admin, refrigerante.getId(), 1, "VENCIMENTO", null);

        assertEquals(estoqueInicial - 1, estoqueAtual());
    }

    @Test
    void operadorNaoRegistraPerda() {
        Usuario operador = app.getAuth().autenticar("operador", "caixa123");
        int estoqueInicial = refrigerante.getEstoque();

        ValidacaoException erro = assertThrows(ValidacaoException.class,
                () -> app.getInventario().registrarPerda(operador, refrigerante.getId(), 1, "QUEBRA", null));

        assertTrue(erro.getMessage().contains("GERENTE/ADMIN"));
        assertEquals(estoqueInicial, estoqueAtual());
    }

    @Test
    void rejeitaQuantidadeNaoPositiva() {
        Usuario gerente = gerente();

        assertThrows(ValidacaoException.class,
                () -> app.getInventario().registrarPerda(gerente, refrigerante.getId(), 0, "QUEBRA", null));
        assertThrows(ValidacaoException.class,
                () -> app.getInventario().registrarPerda(gerente, refrigerante.getId(), -5, "QUEBRA", null));
    }

    @Test
    void rejeitaMotivoDesconhecido() {
        Usuario gerente = gerente();

        ValidacaoException erro = assertThrows(ValidacaoException.class,
                () -> app.getInventario().registrarPerda(gerente, refrigerante.getId(), 1, "MOTIVO_QUALQUER", null));

        assertTrue(erro.getMessage().contains("Motivo inválido"));
    }

    @Test
    void rejeitaPerdaMaiorQueEstoque() {
        Usuario gerente = gerente();
        int estoqueInicial = refrigerante.getEstoque();

        ValidacaoException erro = assertThrows(ValidacaoException.class,
                () -> app.getInventario().registrarPerda(gerente, refrigerante.getId(), estoqueInicial + 1, "QUEBRA", null));

        assertTrue(erro.getMessage().contains("Estoque insuficiente"));
        assertEquals(estoqueInicial, estoqueAtual());
    }

    @Test
    void rejeitaProdutoInexistente() {
        Usuario gerente = gerente();

        assertThrows(ValidacaoException.class,
                () -> app.getInventario().registrarPerda(gerente, 999999L, 1, "QUEBRA", null));
    }

    @Test
    void normalizaMotivoParaMaiusculas() {
        Usuario gerente = gerente();

        app.getInventario().registrarPerda(gerente, refrigerante.getId(), 1, "quebra", null);

        List<PerdaEstoque> perdas = app.getInventario().listarPerdas(refrigerante.getId());
        assertEquals(1, perdas.size());
        assertEquals("QUEBRA", perdas.get(0).getMotivo());
    }

    @Test
    void registraAuditoriaDaPerda() {
        Usuario gerente = gerente();

        app.getInventario().registrarPerda(gerente, refrigerante.getId(), 2, "AVARIA", "Embalagem violada");

        List<String> acoes = app.getAuditoria().listar(java.time.LocalDate.now().minusDays(1),
                java.time.LocalDate.now().plusDays(1)).stream()
                .map(evento -> evento.acao())
                .toList();
        assertTrue(acoes.contains("REGISTRAR_PERDA_ESTOQUE"));
    }

    private int estoqueAtual() {
        return app.getProdutos().buscarPorCodigoBarras("7891000100103").getEstoque();
    }
}
