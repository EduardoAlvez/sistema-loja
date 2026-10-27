package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.Produto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class DialogoBuscaProdutoTest {

    private Aplicacao app;
    private DialogoBuscaProduto dialogo;

    @BeforeEach
    void preparar() {
        assumeFalse(GraphicsEnvironment.isHeadless(),
                "o dialogo precisa de um display para ser instanciado");
        app = BancoTeste.criar("dialogobusca" + System.nanoTime());
        dialogo = new DialogoBuscaProduto(null, app);
    }

    @AfterEach
    void limpar() {
        if (dialogo != null) {
            dialogo.dispose();
        }
    }

    @Test
    void listaOsProdutosAtivosNoInicio() {
        assertEquals(5, dialogo.totalLinhas(), "5 produtos ativos do seed");
        assertEquals(5, dialogo.totalProdutosCarregados());
        assertTrue(nomes().contains("Refrigerante Cola 2L"), "lista carregada: " + nomes());
        assertTrue(nomes().contains("Água Mineral 500ml"), "lista carregada: " + nomes());
    }

    @Test
    void filtraPeloNomeDigitado() {
        dialogo.informarBusca("Arroz");

        assertEquals(1, dialogo.totalLinhas());
        assertEquals("Arroz Tipo 1 5kg", dialogo.valor(0, 1));
    }

    @Test
    void filtraPeloCodigoDeBarras() {
        dialogo.informarBusca("7891000100104");

        assertEquals(1, dialogo.totalLinhas());
        assertEquals("Água Mineral 500ml", dialogo.valor(0, 1));
    }

    @Test
    void termoComCaractereEspecialNaoQuebraAFiltra() {
        dialogo.informarBusca("(");

        assertEquals(0, dialogo.totalLinhas(), "nenhum produto casa com o parenteses");
        assertEquals(-1, dialogo.linhaSelecionada(), "sem resultado nao ha linha selecionada");
        assertTrue(dialogo.textoStatus().contains("Nenhum produto encontrado"),
                dialogo.textoStatus());
    }

    @Test
    void buscaSemResultadoEsvaziaALista() {
        dialogo.informarBusca("produto inexistente");

        assertEquals(0, dialogo.totalLinhas());
    }

    @Test
    void mostraOPrecoVigenteComPromocaoDoProduto() {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        agua.setPrecoPromocional(new BigDecimal("1.50"));
        agua.setPromoInicio(java.time.LocalDate.of(2000, 1, 1));
        agua.setPromoFim(java.time.LocalDate.of(2099, 12, 31));
        app.getProdutos().salvar(agua);

        dialogo.dispose();
        dialogo = new DialogoBuscaProduto(null, app);
        dialogo.informarBusca("Água Mineral");

        assertEquals(1, dialogo.totalLinhas());
        assertEquals(Ui.moeda(new BigDecimal("1.50")), dialogo.valor(0, 3));
    }

    @Test
    void selecionaAPrimeiraLinhaAoAbrir() {
        assertTrue(dialogo.linhaSelecionada() >= 0, "a primeira linha deve ficar selecionada");
    }

    private java.util.List<String> nomes() {
        java.util.List<String> lista = new java.util.ArrayList<>();
        for (int linha = 0; linha < dialogo.totalLinhas(); linha++) {
            lista.add(String.valueOf(dialogo.valor(linha, 1)));
        }
        return lista;
    }
}
