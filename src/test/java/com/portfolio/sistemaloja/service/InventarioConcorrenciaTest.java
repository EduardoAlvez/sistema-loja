package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.ContagemInventario;
import com.portfolio.sistemaloja.model.ContagemItem;
import com.portfolio.sistemaloja.model.PerfilUsuario;
import com.portfolio.sistemaloja.model.Produto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventarioConcorrenciaTest {

    private Aplicacao app;
    private Produto refrigerante;
    private Produto biscoito;

    @BeforeEach
    void preparar() {
        app = BancoTeste.criar("inventarioconcorrencia" + System.nanoTime());
        refrigerante = app.getProdutos().buscarPorCodigoBarras("7891000100103");
        biscoito = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        app.getUsuarios().criar("Gerente Loja", "gerente", "gerente123", PerfilUsuario.GERENTE);
    }

    private void autenticarGerente() {
        app.getAuth().autenticar("gerente", "gerente123");
    }

    @Test
    void abrirContagemEmParaleloPermiteApenasUmaAbertura() throws Exception {
        autenticarGerente();
        List<Long> produtos = List.of(refrigerante.getId(), biscoito.getId());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch pronto = new CountDownLatch(2);
        CountDownLatch inicio = new CountDownLatch(1);
        try {
            List<Future<Long>> resultados = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                resultados.add(executor.submit(() -> {
                    pronto.countDown();
                    inicio.await(5, TimeUnit.SECONDS);
                    return app.getInventario().abrirContagem(produtos);
                }));
            }
            assertTrue(pronto.await(5, TimeUnit.SECONDS));
            inicio.countDown();

            int ok = 0;
            int falhas = 0;
            for (Future<Long> futuro : resultados) {
                try {
                    Long id = futuro.get(10, TimeUnit.SECONDS);
                    if (id != null && id > 0) {
                        ok++;
                    }
                } catch (Exception e) {
                    falhas++;
                }
            }

            assertEquals(1, ok, "Apenas uma abertura deve ter sucesso");
            assertEquals(1, falhas, "A segunda tentativa deve falhar");
            assertNotNull(app.getInventario().buscarContagemAberta());

            List<ContagemInventario> todas = app.getInventario().listarContagens();
            long abertas = 0;
            for (ContagemInventario c : todas) {
                if (c != null && c.estaAberta()) {
                    abertas++;
                }
            }
            assertEquals(1, abertas, "Nunca devem existir duas contagens ABERTAS");
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void fecharDuasVezesEmParaleloAplicaAjustesApenasUmaVez() throws Exception {
        autenticarGerente();
        long contagemId = app.getInventario().abrirContagem(List.of(refrigerante.getId()));
        app.getInventario().informarContagem(contagemId, refrigerante.getId(), 40);

        int estoqueInicial = refrigerante.getEstoque();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch pronto = new CountDownLatch(2);
        CountDownLatch inicio = new CountDownLatch(1);
        try {
            List<Future<ContagemInventario>> resultados = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                resultados.add(executor.submit(() -> {
                    pronto.countDown();
                    inicio.await(5, TimeUnit.SECONDS);
                    return app.getInventario().fecharContagem(contagemId, null);
                }));
            }
            assertTrue(pronto.await(5, TimeUnit.SECONDS));
            inicio.countDown();

            int ok = 0;
            int falhas = 0;
            for (Future<ContagemInventario> futuro : resultados) {
                try {
                    ContagemInventario fechada = futuro.get(10, TimeUnit.SECONDS);
                    if (fechada != null && !fechada.estaAberta()) {
                        ok++;
                    }
                } catch (Exception e) {
                    falhas++;
                }
            }

            assertEquals(1, ok);
            assertTrue(falhas >= 1, "O segundo fechamento deve falhar");

            refrigerante = app.getProdutos().buscarPorCodigoBarras("7891000100103");
            assertEquals(estoqueInicial, refrigerante.getEstoque(), "Ajuste aplicado apenas uma vez");

            List<ContagemItem> itens = app.getInventario().listarItens(contagemId);
            assertEquals(1, itens.size());
            ContagemItem item = itens.get(0);
            assertTrue(item.isAjustado());
            assertEquals(0, item.getDeltaAplicado(), "Sem diferença, delta aplicado deve ser 0");
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void abrirEnquantoOutraThreadFechaNaoCriaSegundaContagemAberta() throws Exception {
        autenticarGerente();
        long contagemId = app.getInventario().abrirContagem(List.of(refrigerante.getId()));
        app.getInventario().informarContagem(contagemId, refrigerante.getId(), 45);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch pronto = new CountDownLatch(2);
        CountDownLatch inicio = new CountDownLatch(1);
        AtomicInteger abertos = new AtomicInteger();
        try {
            Future<ContagemInventario> fechar = executor.submit(() -> {
                pronto.countDown();
                inicio.await(5, TimeUnit.SECONDS);
                return app.getInventario().fecharContagem(contagemId, null);
            });
            Future<Long> abrir = executor.submit(() -> {
                pronto.countDown();
                inicio.await(5, TimeUnit.SECONDS);
                return app.getInventario().abrirContagem(List.of(biscoito.getId()));
            });
            assertTrue(pronto.await(5, TimeUnit.SECONDS));
            inicio.countDown();

            boolean fechou = false;
            boolean abriu = false;
            try {
                ContagemInventario c = fechar.get(10, TimeUnit.SECONDS);
                fechou = c != null && !c.estaAberta();
            } catch (Exception ignored) {
            }
            try {
                Long id = abrir.get(10, TimeUnit.SECONDS);
                abriu = id != null && id > 0;
            } catch (Exception ignored) {
            }

            assertTrue(fechou, "Fechamento deveria ocorrer");

            List<ContagemInventario> todas = app.getInventario().listarContagens();
            for (ContagemInventario c : todas) {
                if (c != null && c.estaAberta()) {
                    abertos.incrementAndGet();
                }
            }
            assertTrue(abertos.get() <= 1, "Nunca mais de uma contagem ABERTA simultânea");
            if (abriu) {
                assertNotNull(app.getInventario().buscarContagemAberta());
            }
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
