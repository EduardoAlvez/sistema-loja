package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PainelPDVTest {

    private Aplicacao app;
    private Usuario operador;

    @BeforeEach
    void preparar() {
        app = BancoTeste.criar("painelPdv" + System.nanoTime());
        operador = app.getAuth().autenticar("operador", "caixa123");
    }

    @Test
    void mostraPromocaoEUsaOPrecoVigenteNoCarrinho() {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        agua.setPrecoPromocional(new BigDecimal("1.50"));
        agua.setPromoInicio(LocalDate.of(2000, 1, 1));
        agua.setPromoFim(LocalDate.of(2099, 12, 31));
        app.getProdutos().salvar(agua);

        PainelPDV painel = new PainelPDV(app, operador);
        int linha = linhaDe(painel, "Água Mineral 500ml");

        assertEquals(Ui.moeda(new BigDecimal("1.50")), painel.produtoListado(linha, 3));
        assertEquals("Ativa", painel.produtoListado(linha, 4));

        painel.adicionarProduto(agua);
        painel.adicionarProduto(agua);

        assertEquals(1, painel.itensNoCarrinho());
        assertEquals(2, painel.itemDoCarrinho(0, 1));
        assertEquals(Ui.moeda(new BigDecimal("1.50")), painel.itemDoCarrinho(0, 3));
    }

    @Test
    void semPromocaoMantemOPrecoCheio() {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");

        PainelPDV painel = new PainelPDV(app, operador);
        int linha = linhaDe(painel, "Água Mineral 500ml");

        assertEquals(Ui.moeda(agua.getPrecoVenda()), painel.produtoListado(linha, 3));
        assertEquals("", painel.produtoListado(linha, 4));

        painel.adicionarProduto(agua);
        assertEquals(Ui.moeda(agua.getPrecoVenda()), painel.itemDoCarrinho(0, 3));
    }

    private int linhaDe(PainelPDV painel, String nome) {
        for (int linha = 0; linha < painel.produtosListados(); linha++) {
            if (nome.equals(painel.produtoListado(linha, 1))) {
                return linha;
            }
        }
        throw new AssertionError("Produto não encontrado na lista: " + nome);
    }
}
