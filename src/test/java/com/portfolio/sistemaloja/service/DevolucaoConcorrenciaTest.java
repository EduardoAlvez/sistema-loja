package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.Cliente;
import com.portfolio.sistemaloja.model.DevolucaoItem;
import com.portfolio.sistemaloja.model.FormaPagamento;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.model.VendaItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevolucaoConcorrenciaTest {

    private Aplicacao app;
    private Usuario operador;
    private Usuario admin;
    private Cliente cliente;
    private Produto agua;

    @BeforeEach
    void preparar() {
        app = BancoTeste.criar("concorrencia" + System.nanoTime());
        operador = app.getAuth().autenticar("operador", "caixa123");
        admin = app.getAuth().autenticar("admin", "admin123");
        cliente = app.getClientes().salvar(new Cliente(null, "Maria Souza", "52998224725", "11999998888",
                "maria@exemplo.com", true));
        agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
    }

    @Test
    void devolucoesSimultaneasNaoEstornamMaisQueOMercadoriaDevolvida() throws Exception {
        Venda registrada = app.getVendas().registrar(venda(admin, 4, BigDecimal.ZERO, 0));
        assertEquals(10, app.getFidelidade().saldo(cliente.getId()));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch largada = new CountDownLatch(1);
            Callable<Boolean> devolver = () -> {
                largada.await();
                app.getDevolucoes().devolver(registrada.getId(), List.of(itemDevolvido(registrada, 1)),
                        false, null, operador);
                return true;
            };
            Future<Boolean> primeira = pool.submit(devolver);
            Future<Boolean> segunda = pool.submit(devolver);
            largada.countDown();
            primeira.get();
            segunda.get();
        } finally {
            pool.shutdownNow();
        }

        assertEquals(2, app.getDevolucoes().jaDevolvido(registrada.getItens().get(0).getId()),
                "cada devolucao simultanea deve consumir 1 unidade, sem dupla contagem");
        assertEquals(5, app.getFidelidade().saldo(cliente.getId()),
                "2 de 4 itens devolvidos estornam 5 dos 10 pontos, sem estorno duplicado");
    }

    @Test
    void devolucaoSimultaneaNaoExcedeOEstoqueDisponivel() throws Exception {
        Venda registrada = app.getVendas().registrar(venda(admin, 2, BigDecimal.ZERO, 0));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        int concluidas;
        try {
            CountDownLatch largada = new CountDownLatch(1);
            Callable<Boolean> devolver = () -> {
                largada.await();
                try {
                    app.getDevolucoes().devolver(registrada.getId(), List.of(itemDevolvido(registrada, 2)),
                            false, null, operador);
                    return true;
                } catch (ValidacaoException esperada) {
                    return false;
                }
            };
            Future<Boolean> primeira = pool.submit(devolver);
            Future<Boolean> segunda = pool.submit(devolver);
            largada.countDown();
            boolean a = primeira.get();
            boolean b = segunda.get();
            concluidas = (a ? 1 : 0) + (b ? 1 : 0);
        } finally {
            pool.shutdownNow();
        }

        assertEquals(1, concluidas, "apenas uma das duas devolucoes pode consumir as 2 unidades");
        assertEquals(2, app.getDevolucoes().jaDevolvido(registrada.getItens().get(0).getId()));
        assertTrue(app.getFidelidade().saldo(cliente.getId()) >= 0);
    }

    private DevolucaoItem itemDevolvido(Venda venda, int quantidade) {
        VendaItem item = venda.getItens().get(0);
        return new DevolucaoItem(item.getId(), null, null, quantidade, null);
    }

    private Venda venda(Usuario usuario, int quantidade, BigDecimal desconto, int pontosResgatados) {
        Venda venda = new Venda();
        venda.setUsuarioId(usuario.getId());
        venda.setClienteId(cliente.getId());
        venda.setFormaPagamento(FormaPagamento.PIX);
        venda.setDesconto(desconto);
        venda.setPontosResgatados(pontosResgatados);
        venda.adicionarItem(new VendaItem(agua.getId(), agua.getNome(), quantidade, agua.getPrecoVenda()));
        return venda;
    }
}
