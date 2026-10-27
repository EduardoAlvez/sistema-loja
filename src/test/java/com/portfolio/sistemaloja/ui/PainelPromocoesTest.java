package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.PromocaoProduto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PainelPromocoesTest {

    private static final LocalDateTime MOMENTO = LocalDateTime.of(2026, 10, 20, 12, 0);

    private Aplicacao app;
    private Produto refrigerante;

    @BeforeEach
    void preparar() {
        ZoneId zona = ZoneId.systemDefault();
        app = BancoTeste.criar("painelpromocoes" + System.nanoTime(),
                Clock.fixed(MOMENTO.atZone(zona).toInstant(), zona));
        refrigerante = app.getProdutos().buscarPorCodigoBarras("7891000100103");
    }

    @Test
    void listaPromocaoComProdutoPrecoESituacao() {
        app.getPromocoes().salvar(promocao("Refrigerante 3 por 7", 3, "7.00", true));

        PainelPromocoes painel = new PainelPromocoes(app);

        assertEquals(1, painel.totalLinhas());
        assertEquals("Refrigerante Cola 2L", painel.valor(0, 0));
        assertEquals("Refrigerante 3 por 7", painel.valor(0, 1));
        assertEquals(3, painel.valor(0, 2));
        assertEquals("Em vigor", painel.valor(0, 7));
        assertTrue(painel.textoAlerta().contains("1 promoção(ões) ativas"), painel.textoAlerta());
    }

    @Test
    void marcaPromocaoInativaEProgramada() {
        PromocaoProduto inativa = promocao("Promo inativa", 2, "7.50", false);
        app.getPromocoes().salvar(inativa);

        PromocaoProduto programada = promocao("Promo futura", 2, "7.00", true);
        programada.setDataInicio(MOMENTO.toLocalDate().plusDays(3));
        app.getPromocoes().salvar(programada);

        PainelPromocoes painel = new PainelPromocoes(app);

        assertEquals(2, painel.totalLinhas());
        assertEquals("Inativa", painel.valor(indiceDe(painel, "Promo inativa"), 7));
        assertEquals("Programada", painel.valor(indiceDe(painel, "Promo futura"), 7));
    }

    @Test
    void mostraPeriodoDiasEHorarioDaPromocao() {
        PromocaoProduto promocao = promocao("Promo da tarde", 3, "7.00", true);
        promocao.setDataInicio(MOMENTO.toLocalDate());
        promocao.setDataFim(MOMENTO.toLocalDate().plusDays(7));
        promocao.setHorarioInicio(java.time.LocalTime.of(13, 0));
        promocao.setHorarioFim(java.time.LocalTime.of(18, 0));
        app.getPromocoes().salvar(promocao);

        PainelPromocoes painel = new PainelPromocoes(app);

        assertEquals("20/10/2026 a 27/10/2026", painel.valor(0, 4));
        assertEquals("Todos", painel.valor(0, 5));
        assertEquals("13:00 a 18:00", painel.valor(0, 6));
    }

    private int indiceDe(PainelPromocoes painel, String nome) {
        for (int i = 0; i < painel.totalLinhas(); i++) {
            if (nome.equals(painel.valor(i, 1))) {
                return i;
            }
        }
        throw new AssertionError("promocao nao encontrada na tabela: " + nome);
    }

    private PromocaoProduto promocao(String nome, int quantidade, String preco, boolean ativa) {
        PromocaoProduto promocao = new PromocaoProduto();
        promocao.setNome(nome);
        promocao.setProdutoId(refrigerante.getId());
        promocao.setQuantidadeMinima(quantidade);
        promocao.setPrecoPromocional(new BigDecimal(preco));
        promocao.setAtiva(ativa);
        return promocao;
    }
}
