package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.FormaPagamento;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.model.VendaItem;
import com.portfolio.sistemaloja.model.VendaPagamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CupomServiceTest {

    private Aplicacao app;
    private Usuario operador;

    @BeforeEach
    void preparar() {
        app = BancoTeste.criar("cupom" + System.nanoTime());
        operador = app.getAuth().autenticar("operador", "caixa123");
    }

    @Test
    void cupomTrazItensOperadorTotalEPagamento() {
        Venda venda = venda(new BigDecimal("7.50"));
        venda.setFormaPagamento(FormaPagamento.DINHEIRO);
        venda.setValorPago(new BigDecimal("10.00"));
        Venda registrada = app.getVendas().registrar(venda);

        String cupom = app.getCupom().formatar(registrada);

        assertTrue(cupom.contains("CUPOM NÃO FISCAL"), cupom);
        assertTrue(cupom.contains(registrada.getNumero()), cupom);
        assertTrue(cupom.contains("Água Mineral 500ml"), cupom);
        assertTrue(cupom.contains("Operador Caixa"), cupom);
        assertTrue(cupom.contains("TOTAL:"), cupom);
        assertTrue(cupom.contains("7,50"), cupom);
        assertTrue(cupom.contains("Pagamento: Dinheiro"), cupom);
        assertTrue(cupom.contains("Troco:"), cupom);
        assertTrue(cupom.contains("2,50"), cupom);
    }

    @Test
    void cupomMostraDescontoDaVendaEDoItem() {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        Venda venda = new Venda();
        venda.setUsuarioId(operador.getId());
        venda.setFormaPagamento(FormaPagamento.PIX);
        VendaItem item = new VendaItem(agua.getId(), agua.getNome(), 4, agua.getPrecoVenda());
        item.setDesconto(new BigDecimal("0.50"));
        venda.adicionarItem(item);
        venda.setDesconto(new BigDecimal("0.50"));
        Venda registrada = app.getVendas().registrar(venda);

        String cupom = app.getCupom().formatar(registrada);

        assertTrue(cupom.contains("desc. item: -"), cupom);
        assertTrue(cupom.contains("Desconto: -"), cupom);
        assertTrue(cupom.contains("9,00"), cupom);
    }

    @Test
    void cupomDePagamentoDivididoListaAsParcelas() {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        Venda venda = new Venda();
        venda.setUsuarioId(operador.getId());
        venda.setFormaPagamento(FormaPagamento.PIX);
        venda.adicionarItem(new VendaItem(agua.getId(), agua.getNome(), 4, agua.getPrecoVenda()));
        venda.adicionarPagamento(new VendaPagamento(FormaPagamento.PIX, new BigDecimal("4.00")));
        venda.adicionarPagamento(new VendaPagamento(FormaPagamento.DINHEIRO, new BigDecimal("6.00")));
        Venda registrada = app.getVendas().registrar(venda);

        String cupom = app.getCupom().formatar(registrada);

        assertTrue(cupom.contains("Pagamento dividido:"), cupom);
        assertTrue(cupom.contains("PIX: "), cupom);
        assertTrue(cupom.contains("Dinheiro: "), cupom);
    }

    private Venda venda(BigDecimal pago) {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        Venda venda = new Venda();
        venda.setUsuarioId(operador.getId());
        venda.setValorPago(pago);
        venda.adicionarItem(new VendaItem(agua.getId(), agua.getNome(), 3, agua.getPrecoVenda()));
        return venda;
    }
}
