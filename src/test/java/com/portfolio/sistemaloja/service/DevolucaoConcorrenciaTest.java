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
import java.util.concurrent.TimeUnit;

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

        CountDownLatch primeiraDentroDoLock = new CountDownLatch(1);
        CountDownLatch liberarPrimeira = new CountDownLatch(1);
        app.getDevolucaoRepository().aposBloqueioParaTeste(conexao -> {
            primeiraDentroDoLock.countDown();
            aguardar(liberarPrimeira);
        });

        ExecutorService pool = Executors.newFixedThreadPool(2);
        boolean primeiraOk;
        boolean segundaOk;
        try {
            Future<Boolean> primeira = pool.submit(devolver(registrada, 1));
            aguardar(primeiraDentroDoLock);
            Future<Boolean> segunda = pool.submit(devolver(registrada, 1));
            Thread.sleep(200);
            liberarPrimeira.countDown();
            primeiraOk = primeira.get(10, TimeUnit.SECONDS);
            segundaOk = segunda.get(10, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        assertTrue(primeiraOk && segundaOk);
        assertEquals(2, app.getDevolucoes().jaDevolvido(registrada.getItens().get(0).getId()),
                "cada devolucao simultanea deve consumir 1 unidade, sem dupla contagem");
        assertEquals(5, app.getFidelidade().saldo(cliente.getId()),
                "2 de 4 itens devolvidos estornam 5 dos 10 pontos, sem estorno duplicado");
    }

    @Test
    void devolucaoSimultaneaNaoExcedeOEstoqueDisponivel() throws Exception {
        Venda registrada = app.getVendas().registrar(venda(admin, 2, BigDecimal.ZERO, 0));

        CountDownLatch primeiraDentroDoLock = new CountDownLatch(1);
        CountDownLatch liberarPrimeira = new CountDownLatch(1);
        app.getDevolucaoRepository().aposBloqueioParaTeste(conexao -> {
            primeiraDentroDoLock.countDown();
            aguardar(liberarPrimeira);
        });

        ExecutorService pool = Executors.newFixedThreadPool(2);
        boolean a;
        boolean b;
        try {
            Future<Boolean> primeira = pool.submit(devolver(registrada, 2));
            aguardar(primeiraDentroDoLock);
            Future<Boolean> segunda = pool.submit(devolver(registrada, 2));
            Thread.sleep(200);
            liberarPrimeira.countDown();
            a = primeira.get(10, TimeUnit.SECONDS);
            b = segunda.get(10, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        assertEquals(1, (a ? 1 : 0) + (b ? 1 : 0),
                "apenas uma das duas devolucoes pode consumir as 2 unidades");
        assertEquals(2, app.getDevolucoes().jaDevolvido(registrada.getItens().get(0).getId()));
    }

    private Callable<Boolean> devolver(Venda venda, int quantidade) {
        return () -> {
            try {
                app.getDevolucoes().devolver(venda.getId(), List.of(itemDevolvido(venda, quantidade)),
                        false, null, operador);
                return true;
            } catch (ValidacaoException esperada) {
                return false;
            }
        };
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

    private static void aguardar(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("timeout aguardando coordenacao do teste");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
