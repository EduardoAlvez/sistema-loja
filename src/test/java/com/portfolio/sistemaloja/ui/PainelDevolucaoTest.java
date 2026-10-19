package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.Cliente;
import com.portfolio.sistemaloja.model.DevolucaoItem;
import com.portfolio.sistemaloja.model.FormaPagamento;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.model.VendaItem;
import com.portfolio.sistemaloja.service.ValidacaoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PainelDevolucaoTest {

    private Aplicacao app;
    private Usuario operador;

    @BeforeEach
    void preparar() {
        app = BancoTeste.criar("paineldevolucao" + System.nanoTime());
        operador = app.getAuth().autenticar("operador", "caixa123");
    }

    @Test
    void limitaADevolucaoAoDisponivelDescontandoOQueJaFoiDevolvido() {
        Venda registrada = vendaRegistrada(5, null);
        VendaItem item = registrada.getItens().get(0);

        PainelDevolucao painel = new PainelDevolucao(registrada, id -> id == item.getId() ? 2 : 0);

        assertEquals(1, painel.getQuantidadeLinhas());
        assertEquals(3, painel.quantidadeMaxima(0));
        painel.definirQuantidade(0, 10);
        assertEquals(3, painel.quantidade(0));
        painel.definirQuantidade(0, -4);
        assertEquals(0, painel.quantidade(0));
    }

    @Test
    void totalSomaUnitarioVezesQuantidade() {
        Venda registrada = vendaRegistrada(5, null);
        PainelDevolucao painel = new PainelDevolucao(registrada, id -> 0);

        painel.definirQuantidade(0, 3);

        assertEquals(0, new BigDecimal("7.50").compareTo(painel.total()));
        List<DevolucaoItem> itens = painel.itensParaDevolucao();
        assertEquals(1, itens.size());
        assertEquals(3, itens.get(0).getQuantidade());
        assertEquals(0, new BigDecimal("2.50").compareTo(itens.get(0).getValorUnitario()));
    }

    @Test
    void semQuantidadeInformadaNaoHaItensParaDevolver() {
        Venda registrada = vendaRegistrada(2, null);

        PainelDevolucao painel = new PainelDevolucao(registrada, id -> 0);

        assertThrows(ValidacaoException.class, painel::itensParaDevolucao);
        assertEquals(0, BigDecimal.ZERO.compareTo(painel.total()));
    }

    @Test
    void creditoSoFicaDisponivelQuandoAVendaTemCliente() {
        Cliente maria = app.getClientes().salvar(new Cliente(null, "Maria Souza", "52998224725",
                "11999998888", "maria@exemplo.com", true));

        PainelDevolucao semCliente = new PainelDevolucao(vendaRegistrada(1, null), id -> 0);
        PainelDevolucao comCliente = new PainelDevolucao(vendaRegistrada(1, maria.getId()), id -> 0);

        assertFalse(semCliente.creditoDisponivel());
        assertTrue(comCliente.creditoDisponivel());
        comCliente.definirCreditoNoCliente(true);
        assertTrue(comCliente.creditoNoCliente());
    }

    private Venda vendaRegistrada(int quantidade, Long clienteId) {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        Venda venda = new Venda();
        venda.setUsuarioId(operador.getId());
        venda.setClienteId(clienteId);
        venda.setFormaPagamento(FormaPagamento.DINHEIRO);
        venda.setValorPago(agua.getPrecoVenda().multiply(BigDecimal.valueOf(quantidade)));
        venda.adicionarItem(new VendaItem(agua.getId(), agua.getNome(), quantidade, agua.getPrecoVenda()));
        return app.getVendas().registrar(venda);
    }
}
