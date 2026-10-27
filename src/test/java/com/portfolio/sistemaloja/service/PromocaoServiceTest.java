package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.FormaPagamento;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.PromocaoProduto;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.model.VendaItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromocaoServiceTest {

    private static final LocalDateTime MOMENTO = LocalDateTime.of(2026, 10, 20, 12, 0);

    private Aplicacao app;
    private Produto refrigerante;

    @BeforeEach
    void preparar() {
        app = appNoHorario(MOMENTO);
        refrigerante = app.getProdutos().buscarPorCodigoBarras("7891000100103");
    }

    @Test
    void aplicaPrecoPromocionalQuandoAtingeQuantidadeMinima() {
        app.getPromocoes().salvar(promocao("Refrigerante 3 por 7", 3, "7.00"));

        assertEquals(0, new BigDecimal("8.99").compareTo(
                app.getPromocoes().precoUnitarioVigente(refrigerante, 2, MOMENTO)));
        assertEquals(0, new BigDecimal("7.00").compareTo(
                app.getPromocoes().precoUnitarioVigente(refrigerante, 3, MOMENTO)));
    }

    @Test
    void naoAplicaPromocaoInativa() {
        PromocaoProduto promocao = promocao("Refrigerante inativa", 3, "7.00");
        promocao.setAtiva(false);
        app.getPromocoes().salvar(promocao);

        assertEquals(0, new BigDecimal("8.99").compareTo(
                app.getPromocoes().precoUnitarioVigente(refrigerante, 5, MOMENTO)));
    }

    @Test
    void naoAplicaPromocaoForaDoPeriodo() {
        PromocaoProduto futura = promocao("Promo futura", 3, "7.00");
        futura.setDataInicio(MOMENTO.toLocalDate().plusDays(1));
        app.getPromocoes().salvar(futura);

        PromocaoProduto encerrada = promocao("Promo encerrada", 3, "7.00");
        encerrada.setDataFim(MOMENTO.toLocalDate().minusDays(1));
        app.getPromocoes().salvar(encerrada);

        assertEquals(0, new BigDecimal("8.99").compareTo(
                app.getPromocoes().precoUnitarioVigente(refrigerante, 5, MOMENTO)));
    }

    @Test
    void aplicaPromocaoSomenteNosDiasProgramados() {
        PromocaoProduto promocao = promocao("Promo de segunda", 3, "7.00");
        promocao.setDiasSemana(EnumSet.of(DayOfWeek.MONDAY));
        app.getPromocoes().salvar(promocao);

        assertEquals(0, new BigDecimal("8.99").compareTo(
                app.getPromocoes().precoUnitarioVigente(refrigerante, 5, MOMENTO)),
                "fora do dia programado o preço normal deve prevalecer");
    }

    @Test
    void aplicaPromocaoNoDiaProgramado() {
        PromocaoProduto promocao = promocao("Promo do dia", 3, "7.00");
        promocao.setDiasSemana(EnumSet.of(MOMENTO.getDayOfWeek()));
        app.getPromocoes().salvar(promocao);

        assertEquals(0, new BigDecimal("7.00").compareTo(
                app.getPromocoes().precoUnitarioVigente(refrigerante, 5, MOMENTO)));
    }

    @Test
    void naoAplicaPromocaoForaDoHorario() {
        PromocaoProduto promocao = promocao("Promo vespertina", 3, "7.00");
        promocao.setHorarioInicio(LocalTime.of(13, 0));
        promocao.setHorarioFim(LocalTime.of(18, 0));
        app.getPromocoes().salvar(promocao);

        assertEquals(0, new BigDecimal("8.99").compareTo(
                app.getPromocoes().precoUnitarioVigente(refrigerante, 5, MOMENTO)));
    }

    @Test
    void escolheAPromocaoMaisBarata() {
        app.getPromocoes().salvar(promocao("Promo 7", 3, "7.00"));
        app.getPromocoes().salvar(promocao("Promo 6.50", 3, "6.50"));

        assertEquals(0, new BigDecimal("6.50").compareTo(
                app.getPromocoes().precoUnitarioVigente(refrigerante, 5, MOMENTO)));
    }

    @Test
    void aplicaPromocaoNasFronteirasInclusivasDeData() {
        PromocaoProduto promocao = promocao("Promo de hoje", 3, "7.00");
        promocao.setDataInicio(MOMENTO.toLocalDate());
        promocao.setDataFim(MOMENTO.toLocalDate());
        app.getPromocoes().salvar(promocao);

        assertEquals(0, new BigDecimal("7.00").compareTo(
                app.getPromocoes().precoUnitarioVigente(refrigerante, 3, MOMENTO)));
    }

    @Test
    void aplicaPromocaoNasFronteirasInclusivasDeHorario() {
        PromocaoProduto promocao = promocao("Promo no intervalo", 3, "7.00");
        promocao.setHorarioInicio(LocalTime.of(11, 0));
        promocao.setHorarioFim(LocalTime.of(13, 0));
        app.getPromocoes().salvar(promocao);

        assertEquals(0, new BigDecimal("7.00").compareTo(
                app.getPromocoes().precoUnitarioVigente(refrigerante, 3,
                        MOMENTO.withHour(11).withMinute(0))), "o inicio do horario e inclusivo");
        assertEquals(0, new BigDecimal("7.00").compareTo(
                app.getPromocoes().precoUnitarioVigente(refrigerante, 3,
                        MOMENTO.withHour(13).withMinute(0))), "o fim do horario e inclusivo");
        assertEquals(0, new BigDecimal("8.99").compareTo(
                app.getPromocoes().precoUnitarioVigente(refrigerante, 3,
                        MOMENTO.withHour(13).withMinute(1))), "depois do fim o preco normal volta");
    }

    @Test
    void mantemPrecoPromocionalDoProdutoQuandoForMenor() {
        refrigerante.setPrecoPromocional(new BigDecimal("6.00"));
        refrigerante.setPromoInicio(MOMENTO.toLocalDate().minusDays(1));
        refrigerante.setPromoFim(MOMENTO.toLocalDate().plusDays(1));
        app.getProdutos().salvar(refrigerante);
        app.getPromocoes().salvar(promocao("Promo por quantidade", 3, "7.00"));

        assertEquals(0, new BigDecimal("6.00").compareTo(
                app.getPromocoes().precoUnitarioVigente(refrigerante, 5, MOMENTO)));
    }

    @Test
    void normalizaOPrecoPromocionalParaDuasCasasDecimais() {
        PromocaoProduto promocao = app.getPromocoes().salvar(promocao("Promo com 3 casas", 3, "7.005"));

        assertEquals(0, new BigDecimal("7.01").compareTo(promocao.getPrecoPromocional()));
        assertEquals(0, new BigDecimal("7.01").compareTo(
                app.getPromocoes().buscarPorId(promocao.getId()).getPrecoPromocional()));
    }

    @Test
    void nomeLongoERecusado() {
        PromocaoProduto promocao = promocao("N".repeat(101), 3, "7.00");

        assertThrows(ValidacaoException.class, () -> app.getPromocoes().salvar(promocao));
    }

    @Test
    void removePromocaoEDevolvePrecoNormal() {
        PromocaoProduto salva = app.getPromocoes().salvar(promocao("Promo temporária", 3, "7.00"));

        app.getPromocoes().remover(salva.getId());

        assertTrue(app.getPromocoes().listarPorProduto(refrigerante.getId()).isEmpty());
        assertEquals(0, new BigDecimal("8.99").compareTo(
                app.getPromocoes().precoUnitarioVigente(refrigerante, 5, MOMENTO)));
    }

    @Test
    void persisteOsDiasDaSemanaNoBanco() {
        PromocaoProduto promocao = promocao("Promo segunda e sexta", 3, "7.00");
        promocao.setDiasSemana(EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY));
        app.getPromocoes().salvar(promocao);

        PromocaoProduto recarregada = app.getPromocoes().buscarPorId(promocao.getId());

        assertEquals(EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), recarregada.getDiasSemana());
        assertEquals("Refrigerante Cola 2L", recarregada.getProdutoNome());
    }

    @Test
    void rejeitaPrecoPromocionalMaiorOuIgualAoPrecoDeVenda() {
        PromocaoProduto igual = promocao("Promo igual", 3, "8.99");
        assertThrows(ValidacaoException.class, () -> app.getPromocoes().salvar(igual));

        PromocaoProduto maior = promocao("Promo maior", 3, "9.50");
        assertThrows(ValidacaoException.class, () -> app.getPromocoes().salvar(maior));
    }

    @Test
    void rejeitaQuantidadeMinimaInvalida() {
        PromocaoProduto promocao = promocao("Promo sem quantidade", 0, "7.00");
        assertThrows(ValidacaoException.class, () -> app.getPromocoes().salvar(promocao));
    }

    @Test
    void rejeitaNomeVazio() {
        PromocaoProduto promocao = promocao(" ", 3, "7.00");
        assertThrows(ValidacaoException.class, () -> app.getPromocoes().salvar(promocao));
    }

    @Test
    void rejeitaPeriodoInvertido() {
        PromocaoProduto promocao = promocao("Promo invertida", 3, "7.00");
        promocao.setDataInicio(LocalDate.of(2026, 10, 30));
        promocao.setDataFim(LocalDate.of(2026, 10, 1));
        assertThrows(ValidacaoException.class, () -> app.getPromocoes().salvar(promocao));
    }

    @Test
    void rejeitaHorarioIncompleto() {
        PromocaoProduto promocao = promocao("Promo sem horário", 3, "7.00");
        promocao.setHorarioInicio(LocalTime.of(13, 0));
        assertThrows(ValidacaoException.class, () -> app.getPromocoes().salvar(promocao));
    }

    @Test
    void aplicaPrecoPromocionalPorQuantidadeNaVenda() {
        Usuario caixa = app.getAuth().autenticar("operador", "caixa123");
        app.getPromocoes().salvar(promocao("Refrigerante 3 por 7", 3, "7.00"));

        Venda venda = new Venda();
        venda.setUsuarioId(caixa.getId());
        venda.setFormaPagamento(FormaPagamento.PIX);
        venda.adicionarItem(new VendaItem(refrigerante.getId(), refrigerante.getNome(), 3,
                refrigerante.getPrecoVenda()));

        Venda registrada = app.getVendas().registrar(venda);

        VendaItem item = registrada.getItens().get(0);
        assertEquals(0, new BigDecimal("7.00").compareTo(item.getPrecoUnitario()));
        assertEquals(0, new BigDecimal("21.00").compareTo(registrada.getTotal()));
    }

    @Test
    void mantemPrecoNormalNaVendaAbaixoDaQuantidadeMinima() {
        Usuario caixa = app.getAuth().autenticar("operador", "caixa123");
        app.getPromocoes().salvar(promocao("Refrigerante 3 por 7", 3, "7.00"));

        Venda venda = new Venda();
        venda.setUsuarioId(caixa.getId());
        venda.setFormaPagamento(FormaPagamento.PIX);
        venda.adicionarItem(new VendaItem(refrigerante.getId(), refrigerante.getNome(), 2,
                refrigerante.getPrecoVenda()));

        Venda registrada = app.getVendas().registrar(venda);

        assertEquals(0, new BigDecimal("8.99").compareTo(
                registrada.getItens().get(0).getPrecoUnitario()));
    }

    @Test
    void usaOPrecoMaisBaratoEntrePromocaoDoProdutoEPromocaoPorQuantidade() {
        Usuario caixa = app.getAuth().autenticar("operador", "caixa123");
        refrigerante.setPrecoPromocional(new BigDecimal("6.00"));
        refrigerante.setPromoInicio(MOMENTO.toLocalDate().minusDays(1));
        refrigerante.setPromoFim(MOMENTO.toLocalDate().plusDays(1));
        app.getProdutos().salvar(refrigerante);
        app.getPromocoes().salvar(promocao("Promo por quantidade", 3, "7.00"));

        Venda venda = new Venda();
        venda.setUsuarioId(caixa.getId());
        venda.setFormaPagamento(FormaPagamento.PIX);
        venda.adicionarItem(new VendaItem(refrigerante.getId(), refrigerante.getNome(), 3,
                refrigerante.getPrecoVenda()));

        Venda registrada = app.getVendas().registrar(venda);

        assertEquals(0, new BigDecimal("6.00").compareTo(
                registrada.getItens().get(0).getPrecoUnitario()));
        assertEquals(0, new BigDecimal("18.00").compareTo(registrada.getTotal()));
    }

    @Test
    void buscaPromocaoInexistenteFalhaComMensagemClara() {
        ValidacaoException erro = assertThrows(ValidacaoException.class,
                () -> app.getPromocoes().buscarPorId(999L));

        assertNotNull(erro.getMessage());
    }

    private PromocaoProduto promocao(String nome, int quantidadeMinima, String preco) {
        PromocaoProduto promocao = new PromocaoProduto();
        promocao.setNome(nome);
        promocao.setProdutoId(refrigerante.getId());
        promocao.setQuantidadeMinima(quantidadeMinima);
        promocao.setPrecoPromocional(new BigDecimal(preco));
        promocao.setAtiva(true);
        return promocao;
    }

    private Aplicacao appNoHorario(LocalDateTime momento) {
        ZoneId zona = ZoneId.systemDefault();
        return BancoTeste.criar("promocao" + System.nanoTime(),
                Clock.fixed(momento.atZone(zona).toInstant(), zona));
    }
}
