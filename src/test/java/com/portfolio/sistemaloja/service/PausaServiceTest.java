package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.FormaPagamento;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.model.VendaItem;
import com.portfolio.sistemaloja.model.VendaPagamento;
import com.portfolio.sistemaloja.model.VendaPausada;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PausaServiceTest {

    private Aplicacao app;
    private Usuario operador;

    @BeforeEach
    void preparar() {
        app = BancoTeste.criar("pausa" + System.nanoTime());
        operador = app.getAuth().autenticar("operador", "caixa123");
    }

    @Test
    void pausaERecuperaVendaComItensDescontoEParcelas() {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        Venda venda = venda(agua, 4);
        venda.setDesconto(new BigDecimal("0.50"));
        venda.adicionarPagamento(new VendaPagamento(FormaPagamento.PIX, new BigDecimal("4.50")));
        venda.adicionarPagamento(new VendaPagamento(FormaPagamento.DINHEIRO, new BigDecimal("5.00")));

        VendaPausada pausada = app.getPausas().pausar(venda, "cliente foi buscar", operador);

        assertEquals(0, new BigDecimal("9.50").compareTo(pausada.getTotal()));
        assertEquals(4, pausada.getQuantidadeItens());
        assertEquals("cliente foi buscar", pausada.getObservacao());

        VendaPausada recuperada = app.getPausas().buscarPorId(pausada.getId());
        assertEquals(1, recuperada.getItens().size());
        assertEquals(FormaPagamento.PIX, recuperada.getFormaPagamento());
        assertEquals(0, new BigDecimal("0.50").compareTo(recuperada.getDesconto()));
        assertEquals(2, recuperada.getPagamentos().size());

        Venda reconstruida = recuperada.paraVenda();
        assertEquals(FormaPagamento.PIX, reconstruida.getFormaPagamento());
        assertEquals(0, new BigDecimal("9.50").compareTo(reconstruida.getTotal()));
        assertEquals(2, reconstruida.getPagamentos().size());
    }

    @Test
    void vendaRecuperadaPodeSerFinalizada() {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        int estoqueInicial = app.getProdutos().buscarPorCodigoBarras("7891000100104").getEstoque();
        VendaPausada pausada = app.getPausas().pausar(venda(agua, 3), null, operador);

        Venda registrada = app.getVendas().registrar(app.getPausas().buscarPorId(pausada.getId()).paraVenda());

        assertEquals(0, new BigDecimal("7.50").compareTo(registrada.getTotal()));
        assertEquals(estoqueInicial - 3,
                app.getProdutos().buscarPorCodigoBarras("7891000100104").getEstoque());
    }

    @Test
    void naoPausaVendaSemItens() {
        Venda venda = new Venda();
        venda.setUsuarioId(operador.getId());

        assertThrows(ValidacaoException.class, () -> app.getPausas().pausar(venda, null, operador));
        assertTrue(app.getPausas().listar().isEmpty());
    }

    @Test
    void listaVendasPausadasComTotalEQuantidade() {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        Produto feijao = app.getProdutos().buscarPorCodigoBarras("7891000200101");
        app.getPausas().pausar(venda(agua, 2), "primeira", operador);
        app.getPausas().pausar(venda(feijao, 1), "segunda", operador);

        List<VendaPausada> pausadas = app.getPausas().listar();

        assertEquals(2, pausadas.size());
        assertEquals(2, pausadas.get(0).getQuantidadeItens());
        assertEquals("primeira", pausadas.get(0).getObservacao());
        assertEquals("Operador Caixa", pausadas.get(0).getUsuarioNome());
    }

    @Test
    void descartarRemoveDaLista() {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        VendaPausada pausada = app.getPausas().pausar(venda(agua, 1), null, operador);

        app.getPausas().descartar(pausada.getId());

        assertTrue(app.getPausas().listar().isEmpty());
        assertThrows(ValidacaoException.class, () -> app.getPausas().buscarPorId(pausada.getId()));
    }

    private Venda venda(Produto produto, int quantidade) {
        Venda venda = new Venda();
        venda.setUsuarioId(operador.getId());
        venda.setFormaPagamento(FormaPagamento.PIX);
        venda.adicionarItem(new VendaItem(produto.getId(), produto.getNome(), quantidade, produto.getPrecoVenda()));
        return venda;
    }
}
