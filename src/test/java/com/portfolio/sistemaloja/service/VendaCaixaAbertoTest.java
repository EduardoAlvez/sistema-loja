package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.FormaPagamento;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.model.VendaItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regressão do bug da guarda de caixa aberto: durante todo o tempo em que
 * {@code VendaService.registrar} procurou o caixa por reflexão com nomes de método
 * inexistentes (engolidos por {@code catch (Throwable)}), vendas entravam sem caixa
 * aberto. Estes testes travam o comportamento prometido pelo Javadoc.
 */
class VendaCaixaAbertoTest {

    private Aplicacao app;
    private Usuario operador;

    @BeforeEach
    void preparar() {
        app = BancoTeste.criar("vendacaixa" + System.nanoTime());
        operador = app.getAuth().autenticar("operador", "caixa123");
    }

    @Test
    void recusaVendaSemCaixaAberto() {
        Venda venda = novaVenda();

        ValidacaoException erro = assertThrows(ValidacaoException.class,
                () -> app.getVendas().registrar(venda));
        assertTrue(erro.getMessage().contains("Abra o caixa"), erro.getMessage());
    }

    @Test
    void registraVendaComCaixaAberto() {
        app.getCaixa().abrir(new BigDecimal("20.00"), operador);
        Venda venda = novaVenda();

        assertNotNull(app.getVendas().registrar(venda).getId());
    }

    private Venda novaVenda() {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        Venda venda = new Venda();
        venda.setUsuarioId(operador.getId());
        venda.setFormaPagamento(FormaPagamento.PIX);
        venda.adicionarItem(new VendaItem(agua.getId(), agua.getNome(), 2, agua.getPrecoVenda()));
        return venda;
    }
}
