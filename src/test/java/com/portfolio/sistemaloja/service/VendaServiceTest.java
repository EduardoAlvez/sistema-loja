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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VendaServiceTest {

    private Aplicacao app;
    private Usuario operador;

    @BeforeEach
    void preparar() {
        app = BancoTeste.criar("venda" + System.nanoTime());
        operador = app.getAuth().autenticar("operador", "caixa123");
    }

    @Test
    void registrarVendaBaixaEstoqueECalculaTroco() {
        Produto refrigerante = app.getProdutos().buscarPorCodigoBarras("7891000100103");
        int estoqueInicial = refrigerante.getEstoque();

        Venda venda = novaVenda(refrigerante, 3, FormaPagamento.DINHEIRO, new BigDecimal("50.00"));
        Venda registrada = app.getVendas().registrar(venda);

        assertNotNull(registrada.getId());
        assertTrue(registrada.getNumero().startsWith("V"));
        assertEquals(0, new BigDecimal("26.97").compareTo(registrada.getTotal()));
        assertEquals(0, new BigDecimal("23.03").compareTo(registrada.getTroco()));

        Produto aposVenda = app.getProdutos().buscarPorCodigoBarras("7891000100103");
        assertEquals(estoqueInicial - 3, aposVenda.getEstoque());
    }

    @Test
    void naoPermiteVenderAcimaDoEstoque() {
        Produto arroz = app.getProdutos().buscarPorCodigoBarras("7891000200100");
        int estoqueInicial = arroz.getEstoque();

        Venda venda = novaVenda(arroz, estoqueInicial + 1, FormaPagamento.PIX, null);
        assertThrows(ValidacaoException.class, () -> app.getVendas().registrar(venda));

        assertEquals(estoqueInicial, app.getProdutos().buscarPorCodigoBarras("7891000200100").getEstoque());
    }

    @Test
    void naoPermiteCarrinhoVazio() {
        Venda venda = new Venda();
        venda.setUsuarioId(operador.getId());
        venda.setFormaPagamento(FormaPagamento.PIX);
        assertThrows(ValidacaoException.class, () -> app.getVendas().registrar(venda));
    }

    @Test
    void naoPermiteValorPagoMenorQueTotalNoDinheiro() {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        Venda venda = novaVenda(agua, 10, FormaPagamento.DINHEIRO, new BigDecimal("1.00"));
        assertThrows(ValidacaoException.class, () -> app.getVendas().registrar(venda));
    }

    @Test
    void cancelarVendaDevolveEstoque() {
        Produto feijao = app.getProdutos().buscarPorCodigoBarras("7891000200101");
        int estoqueInicial = feijao.getEstoque();

        Venda registrada = app.getVendas().registrar(
                novaVenda(feijao, 5, FormaPagamento.DEBITO, null));
        assertEquals(estoqueInicial - 5, app.getProdutos().buscarPorCodigoBarras("7891000200101").getEstoque());

        app.getVendas().cancelar(registrada.getId());

        assertEquals(estoqueInicial, app.getProdutos().buscarPorCodigoBarras("7891000200101").getEstoque());
        assertEquals("CANCELADA", app.getVendas().buscarPorId(registrada.getId()).getStatus().name());
    }

    @Test
    void somaQuantidadesDoMesmoProdutoNoCarrinho() {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        Venda venda = new Venda();
        venda.setUsuarioId(operador.getId());
        venda.setFormaPagamento(FormaPagamento.PIX);
        venda.adicionarItem(new VendaItem(agua.getId(), agua.getNome(), 2, agua.getPrecoVenda()));
        venda.adicionarItem(new VendaItem(agua.getId(), agua.getNome(), 3, agua.getPrecoVenda()));

        Venda registrada = app.getVendas().registrar(venda);

        assertEquals(1, registrada.getItens().size());
        assertEquals(5, registrada.getItens().get(0).getQuantidade());
        assertEquals(0, new BigDecimal("12.50").compareTo(registrada.getTotal()));
    }

    private Venda novaVenda(Produto produto, int quantidade, FormaPagamento pagamento, BigDecimal pago) {
        Venda venda = new Venda();
        venda.setUsuarioId(operador.getId());
        venda.setFormaPagamento(pagamento);
        venda.setValorPago(pago);
        venda.adicionarItem(new VendaItem(produto.getId(), produto.getNome(), quantidade, produto.getPrecoVenda()));
        return venda;
    }
}
