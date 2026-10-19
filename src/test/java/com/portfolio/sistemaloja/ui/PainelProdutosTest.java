package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.Produto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PainelProdutosTest {

    private Aplicacao app;

    @BeforeEach
    void preparar() {
        app = BancoTeste.criar("painelprodutos" + System.nanoTime());
    }

    @Test
    void alertaEDestacaProdutoComEstoqueBaixo() {
        Long bebidas = app.getProdutos().salvarCategoria("Bebidas").getId();
        app.getProdutos().salvar(produto("Refrigerante Zero 2L", "7899000000001", 3, 3, bebidas));
        app.getProdutos().salvar(produto("Refrigerante Uva 2L", "7899000000002", 60, 10, bebidas));

        PainelProdutos painel = new PainelProdutos(app);

        assertEquals(7, painel.totalLinhas(), "5 produtos do seed + 2 novos");
        assertTrue(painel.textoAlerta().contains("1 produto(s) com estoque baixo"), painel.textoAlerta());
        assertEquals("Estoque baixo", painel.situacao(indiceDe(painel, "Refrigerante Zero 2L")));
        assertEquals("Ativo", painel.situacao(indiceDe(painel, "Refrigerante Uva 2L")));
    }

    @Test
    void filtraSomenteEstoqueBaixo() {
        Long limpeza = app.getProdutos().salvarCategoria("Limpeza").getId();
        app.getProdutos().salvar(produto("Detergente Neutro", "7899000000011", 0, 2, limpeza));
        app.getProdutos().salvar(produto("Desinfetante Pinho", "7899000000012", 40, 5, limpeza));

        PainelProdutos painel = new PainelProdutos(app);
        painel.somenteEstoqueBaixo(true);

        assertEquals(1, painel.totalLinhas());
        assertEquals("Detergente Neutro", painel.valor(0, 1));
        assertEquals(0, painel.valor(0, 6));
        assertEquals(2, painel.valor(0, 7));
    }

    @Test
    void filtraProdutosPelaCategoriaSelecionada() {
        Long higiene = app.getProdutos().salvarCategoria("Higiene").getId();
        app.getProdutos().salvar(produto("Bicarbonato", "7899000000021", 12, 3, higiene));
        app.getProdutos().salvar(produto("Fósforo", "7899000000022", 12, 3, null));

        PainelProdutos painel = new PainelProdutos(app);
        painel.selecionarCategoria(higiene);

        assertEquals(1, painel.totalLinhas());
        assertEquals("Bicarbonato", painel.valor(0, 1));
        assertTrue(painel.textoAlerta().isEmpty(), painel.textoAlerta());

        painel.somenteEstoqueBaixo(false);
        painel.selecionarCategoria(null);
        assertEquals(7, painel.totalLinhas());
    }

    private int indiceDe(PainelProdutos painel, String nome) {
        for (int i = 0; i < painel.totalLinhas(); i++) {
            if (nome.equals(painel.valor(i, 1))) {
                return i;
            }
        }
        throw new AssertionError("produto nao encontrado na tabela: " + nome);
    }

    private Produto produto(String nome, String codigo, int estoque, int minimo, Long categoriaId) {
        Produto produto = new Produto();
        produto.setNome(nome);
        produto.setCodigoBarras(codigo);
        produto.setCategoriaId(categoriaId);
        produto.setPrecoCusto(new BigDecimal("5.00"));
        produto.setPrecoVenda(new BigDecimal("9.90"));
        produto.setEstoque(estoque);
        produto.setEstoqueMinimo(minimo);
        produto.setAtivo(true);
        return produto;
    }

    @Test
    void ignoraProdutoInativoMesmoComEstoqueBaixo() {
        Produto inativo = produto("Sabonete Glicerinado", "7899000000031", 1, 5, null);
        inativo.setAtivo(false);
        app.getProdutos().salvar(inativo);

        PainelProdutos painel = new PainelProdutos(app);

        assertEquals("Inativo", painel.situacao(indiceDe(painel, "Sabonete Glicerinado")));
        assertTrue(painel.textoAlerta().isEmpty(), painel.textoAlerta());
        assertFalse(app.getProdutos().listarComEstoqueBaixo().stream()
                .anyMatch(produto -> produto.getNome().equals("Sabonete Glicerinado")));
    }
}
