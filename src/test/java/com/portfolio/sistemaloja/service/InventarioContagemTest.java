package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.ContagemInventario;
import com.portfolio.sistemaloja.model.ContagemItem;
import com.portfolio.sistemaloja.model.PerfilUsuario;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.StatusContagem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventarioContagemTest {

    private Aplicacao app;
    private Produto refrigerante;
    private Produto agua;

    @BeforeEach
    void preparar() {
        app = BancoTeste.criar("inventariocontagem" + System.nanoTime());
        refrigerante = app.getProdutos().buscarPorCodigoBarras("7891000100103");
        agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        app.getUsuarios().criar("Gerente Loja", "gerente", "gerente123", PerfilUsuario.GERENTE);
    }

    private void autenticarGerente() {
        app.getAuth().autenticar("gerente", "gerente123");
    }

    @Test
    void abreContagemComTodosOsProdutosAtivos() {
        autenticarGerente();

        long contagemId = app.getInventario().abrirContagem();

        ContagemInventario contagem = app.getInventario().buscarContagemAberta();
        assertNotNull(contagem);
        assertEquals(contagemId, contagem.getId());
        assertEquals(StatusContagem.ABERTA, contagem.getStatus());
        assertTrue(contagem.getNumero().startsWith("INV-"), contagem.getNumero());
        assertEquals(5, app.getInventario().listarItens(contagemId).size());
    }

    @Test
    void abreContagemDeLoteSelecionado() {
        autenticarGerente();

        long contagemId = app.getInventario().abrirContagem(List.of(refrigerante.getId(), agua.getId()));

        assertEquals(2, app.getInventario().listarItens(contagemId).size());
    }

    @Test
    void guardaEstoqueDoSistemaComoReferenciaDaContagem() {
        autenticarGerente();

        long contagemId = app.getInventario().abrirContagem(List.of(refrigerante.getId()));

        ContagemItem item = app.getInventario().listarItens(contagemId).get(0);
        assertEquals(40, item.getEstoqueSistema());
        assertNull(item.getQuantidadeContada());
        assertFalse(item.foiContado());
        assertFalse(item.isAjustado());
    }

    @Test
    void operadorNaoAbreContagem() {
        app.getAuth().autenticar("operador", "caixa123");

        ValidacaoException erro = assertThrows(ValidacaoException.class,
                () -> app.getInventario().abrirContagem());

        assertTrue(erro.getMessage().contains("GERENTE/ADMIN"));
    }

    @Test
    void semSessaoNaoAbreContagem() {
        ValidacaoException erro = assertThrows(ValidacaoException.class,
                () -> app.getInventario().abrirContagem());

        assertTrue(erro.getMessage().contains("autenticado"));
    }

    @Test
    void naoAbreSegundaContagemEnquantoHaUmaEmAberto() {
        autenticarGerente();
        app.getInventario().abrirContagem();

        ValidacaoException erro = assertThrows(ValidacaoException.class,
                () -> app.getInventario().abrirContagem());

        assertTrue(erro.getMessage().contains("contagem em aberto"), erro.getMessage());
    }

    @Test
    void rejeitaAberturaSemProdutos() {
        autenticarGerente();

        assertThrows(ValidacaoException.class, () -> app.getInventario().abrirContagem(List.of()));
    }

    @Test
    void rejeitaProdutoInexistenteEAbreNenhumaContagem() {
        autenticarGerente();

        assertThrows(ValidacaoException.class,
                () -> app.getInventario().abrirContagem(List.of(999999L)));

        assertNull(app.getInventario().buscarContagemAberta());
        assertTrue(app.getInventario().listarContagens().isEmpty());
    }

    @Test
    void informarContagemNaoAlteraEstoque() {
        autenticarGerente();
        long contagemId = app.getInventario().abrirContagem(List.of(refrigerante.getId()));

        app.getInventario().informarContagem(contagemId, refrigerante.getId(), 37);

        assertEquals(40, estoqueAtual(refrigerante));
    }

    @Test
    void fechaContagemAplicaAjusteParaMais() {
        autenticarGerente();
        long contagemId = app.getInventario().abrirContagem(List.of(refrigerante.getId()));
        app.getInventario().informarContagem(contagemId, refrigerante.getId(), 45);

        app.getInventario().fecharContagem(contagemId, "Sobra de mercadoria");

        assertEquals(45, estoqueAtual(refrigerante));
    }

    @Test
    void fechaContagemAplicaAjusteParaMenos() {
        autenticarGerente();
        long contagemId = app.getInventario().abrirContagem(List.of(refrigerante.getId()));
        app.getInventario().informarContagem(contagemId, refrigerante.getId(), 33);

        app.getInventario().fecharContagem(contagemId, "Faltou mercadoria");

        assertEquals(33, estoqueAtual(refrigerante));
    }

    @Test
    void fechaContagemSemDivergenciaMantemEstoque() {
        autenticarGerente();
        long contagemId = app.getInventario().abrirContagem(List.of(refrigerante.getId()));
        app.getInventario().informarContagem(contagemId, refrigerante.getId(), 40);

        app.getInventario().fecharContagem(contagemId, null);

        assertEquals(40, estoqueAtual(refrigerante));
    }

    @Test
    void itensNaoContadosNaoTrocamEstoque() {
        autenticarGerente();
        long contagemId = app.getInventario().abrirContagem(
                List.of(refrigerante.getId(), agua.getId()));
        app.getInventario().informarContagem(contagemId, refrigerante.getId(), 35);

        app.getInventario().fecharContagem(contagemId, null);

        assertEquals(35, estoqueAtual(refrigerante));
        assertEquals(120, estoqueAtual(agua));
    }

    @Test
    void contagemAdotaOEstoqueContadoQuandoOMovimentoAconteceNoMeio() {
        autenticarGerente();
        long contagemId = app.getInventario().abrirContagem(List.of(refrigerante.getId()));
        app.getInventario().registrarPerda(refrigerante.getId(), 3, "QUEBRA", null);
        assertEquals(37, estoqueAtual(refrigerante));

        app.getInventario().informarContagem(contagemId, refrigerante.getId(), 38);
        app.getInventario().fecharContagem(contagemId, null);

        assertEquals(38, estoqueAtual(refrigerante));
    }

    @Test
    void rejeitaFecharContagemSemNenhumItemContado() {
        autenticarGerente();
        long contagemId = app.getInventario().abrirContagem(List.of(refrigerante.getId()));

        ValidacaoException erro = assertThrows(ValidacaoException.class,
                () -> app.getInventario().fecharContagem(contagemId, null));

        assertTrue(erro.getMessage().contains("ao menos uma contagem"), erro.getMessage());
        assertEquals(40, estoqueAtual(refrigerante));
    }

    @Test
    void rejeitaFecharContagemJaFechada() {
        autenticarGerente();
        long contagemId = app.getInventario().abrirContagem(List.of(refrigerante.getId()));
        app.getInventario().informarContagem(contagemId, refrigerante.getId(), 40);
        app.getInventario().fecharContagem(contagemId, null);

        ValidacaoException erro = assertThrows(ValidacaoException.class,
                () -> app.getInventario().fecharContagem(contagemId, null));

        assertTrue(erro.getMessage().contains("já está"), erro.getMessage());
    }

    @Test
    void rejeitaInformarContagemFechada() {
        autenticarGerente();
        long contagemId = app.getInventario().abrirContagem(List.of(refrigerante.getId()));
        app.getInventario().informarContagem(contagemId, refrigerante.getId(), 40);
        app.getInventario().fecharContagem(contagemId, null);

        assertThrows(ValidacaoException.class,
                () -> app.getInventario().informarContagem(contagemId, refrigerante.getId(), 41));
    }

    @Test
    void rejeitaProdutoQueNaoFazParteDaContagem() {
        autenticarGerente();
        long contagemId = app.getInventario().abrirContagem(List.of(refrigerante.getId()));

        ValidacaoException erro = assertThrows(ValidacaoException.class,
                () -> app.getInventario().informarContagem(contagemId, agua.getId(), 10));

        assertTrue(erro.getMessage().contains("não faz parte"), erro.getMessage());
    }

    @Test
    void rejeitaQuantidadeContadaNegativa() {
        autenticarGerente();
        long contagemId = app.getInventario().abrirContagem(List.of(refrigerante.getId()));

        ValidacaoException erro = assertThrows(ValidacaoException.class,
                () -> app.getInventario().informarContagem(contagemId, refrigerante.getId(), -1));

        assertTrue(erro.getMessage().contains("negativa"), erro.getMessage());
    }

    @Test
    void rejeitaObservacaoLongaNoFechamento() {
        autenticarGerente();
        long contagemId = app.getInventario().abrirContagem(List.of(refrigerante.getId()));
        app.getInventario().informarContagem(contagemId, refrigerante.getId(), 40);
        String observacaoLonga = "x".repeat(InventarioService.OBSERVACAO_MAX + 1);

        ValidacaoException erro = assertThrows(ValidacaoException.class,
                () -> app.getInventario().fecharContagem(contagemId, observacaoLonga));

        assertTrue(erro.getMessage().contains("255"), erro.getMessage());
    }

    @Test
    void fechaContagemGravaDeltaAplicadoEAjustadoNoItem() {
        autenticarGerente();
        long contagemId = app.getInventario().abrirContagem(List.of(refrigerante.getId()));
        app.getInventario().informarContagem(contagemId, refrigerante.getId(), 45);

        app.getInventario().fecharContagem(contagemId, "Sobra");

        ContagemItem item = app.getInventario().listarItens(contagemId).get(0);
        assertTrue(item.isAjustado());
        assertEquals(5, item.getDeltaAplicado());
    }

    @Test
    void naoPermiteInformarItemEmContagemCancelada() {
        autenticarGerente();
        long contagemId = app.getInventario().abrirContagem(List.of(refrigerante.getId()));
        app.getInventario().cancelarContagem(contagemId, "Erro na contagem");

        ValidacaoException erro = assertThrows(ValidacaoException.class,
                () -> app.getInventario().informarContagem(contagemId, refrigerante.getId(), 10));
        assertTrue(erro.getMessage().toLowerCase().contains("cancelada"), erro.getMessage());
    }

    @Test
    void naoPermiteFecharContagemCancelada() {
        autenticarGerente();
        long contagemId = app.getInventario().abrirContagem(List.of(refrigerante.getId()));
        app.getInventario().informarContagem(contagemId, refrigerante.getId(), 10);
        app.getInventario().cancelarContagem(contagemId, "Desistir");

        ValidacaoException erro = assertThrows(ValidacaoException.class,
                () -> app.getInventario().fecharContagem(contagemId, null));
        assertTrue(erro.getMessage().toLowerCase().contains("cancelada"), erro.getMessage());
    }

    @Test
    void cancelarExigeMotivo() {
        autenticarGerente();
        long contagemId = app.getInventario().abrirContagem(List.of(refrigerante.getId()));

        assertThrows(ValidacaoException.class, () -> app.getInventario().cancelarContagem(contagemId, null));
        assertThrows(ValidacaoException.class, () -> app.getInventario().cancelarContagem(contagemId, "   "));
    }

    @Test
    void cancelarNaoAlteraEstoque() {
        autenticarGerente();
        int estoqueInicial = refrigerante.getEstoque();
        long contagemId = app.getInventario().abrirContagem(List.of(refrigerante.getId()));
        app.getInventario().informarContagem(contagemId, refrigerante.getId(), 0);
        app.getInventario().cancelarContagem(contagemId, "Contagem errada");

        refrigerante = app.getProdutos().buscarPorCodigoBarras("7891000100103");
        assertEquals(estoqueInicial, refrigerante.getEstoque());
        ContagemInventario cancelada = null;
        for (ContagemInventario c : app.getInventario().listarContagens()) {
            if (c.getId() == contagemId) {
                cancelada = c;
                break;
            }
        }
        assertNotNull(cancelada);
        assertEquals(StatusContagem.CANCELADA, cancelada.getStatus());
    }

    @Test
    void contagemFechadaSaiDaBuscaDeContagensAbertas() {
        autenticarGerente();
        long contagemId = app.getInventario().abrirContagem(List.of(refrigerante.getId()));
        app.getInventario().informarContagem(contagemId, refrigerante.getId(), 40);
        app.getInventario().fecharContagem(contagemId, null);

        assertNull(app.getInventario().buscarContagemAberta());
    }

    @Test
    void listaHistoricoComAContagemFechada() {
        autenticarGerente();
        long contagemId = app.getInventario().abrirContagem(List.of(refrigerante.getId()));
        app.getInventario().informarContagem(contagemId, refrigerante.getId(), 40);
        app.getInventario().fecharContagem(contagemId, "Fim do ciclo");

        List<ContagemInventario> contagens = app.getInventario().listarContagens();
        assertEquals(1, contagens.size());
        assertEquals(StatusContagem.FECHADA, contagens.get(0).getStatus());
        assertEquals("Fim do ciclo", contagens.get(0).getObservacao());
        assertNotNull(contagens.get(0).getDataFechamento());
    }

    @Test
    void registraAuditoriaDaAberturaDaContagemEAjusteEFechamento() {
        autenticarGerente();
        long contagemId = app.getInventario().abrirContagem(List.of(refrigerante.getId()));
        app.getInventario().informarContagem(contagemId, refrigerante.getId(), 45);
        app.getInventario().fecharContagem(contagemId, null);

        List<String> acoes = app.getAuditoria().listar(LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(1)).stream()
                .map(evento -> evento.acao())
                .toList();
        assertTrue(acoes.contains("ABRIR_CONTAGEM_INVENTARIO"));
        assertTrue(acoes.contains("INFORMAR_CONTAGEM_INVENTARIO"));
        assertTrue(acoes.contains("AJUSTAR_ESTOQUE_CONTAGEM"));
        assertTrue(acoes.contains("FECHAR_CONTAGEM_INVENTARIO"));
    }

    @Test
    void operadorNaoListaItensDaContagem() {
        app.getAuth().autenticar("operador", "caixa123");

        assertThrows(ValidacaoException.class, () -> app.getInventario().listarItens(1L));
        assertThrows(ValidacaoException.class, () -> app.getInventario().buscarContagemAberta());
        assertThrows(ValidacaoException.class, () -> app.getInventario().listarContagens());
    }

    private int estoqueAtual(Produto produto) {
        return app.getProdutos().buscarPorCodigoBarras(produto.getCodigoBarras()).getEstoque();
    }
}
