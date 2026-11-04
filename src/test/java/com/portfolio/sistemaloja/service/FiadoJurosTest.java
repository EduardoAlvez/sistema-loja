package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.Cliente;
import com.portfolio.sistemaloja.model.Fiado;
import com.portfolio.sistemaloja.model.FormaPagamento;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.model.VendaItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Trava da duplicacao de juros do fiado. O codigo recalculava VENCIMENTO->HOJE inteiro
 * a cada recebimento, e a criacao retro-assava juros sem marcar o carimbo de calculo:
 * 1) criar com vencimento atrasado, receber 101.67 hoje => deveria quitar, mas o
 *    primeiro recebimento cobrava os mesmos 1.67 outra vez;
 * 2) receber em dois dias distintos => o segundo cobrava o periodo inteiro de novo.
 * A base correta e max(vencimento, ultimo_calculo_juros).
 *
 * A passagem dos dias e simulada adiantando o Clock injetado no app (divida #45):
 * sem UPDATE JDBC cru e sem corrida de meia-noite entre o teste e o servico.
 */
class FiadoJurosTest {

    private RelogioAjustavel relogio;
    private Aplicacao app;
    private Usuario admin;
    private Cliente cliente;
    private long vendaId;

    @BeforeEach
    void preparar() {
        relogio = new RelogioAjustavel(LocalDate.now().atTime(10, 0));
        app = BancoTeste.criar("fiadojuros" + System.nanoTime(), relogio);
        admin = app.getAuth().autenticar("admin", "admin123");
        BancoTeste.abrirCaixa(app, admin);

        app.getConfiguracoes().definirFiadoHabilitado(true);
        app.getConfiguracoes().definirTaxaJurosFiado(new BigDecimal("5"));

        cliente = app.getClientes().salvar(new Cliente(null, "Joao Lima", "52998224725",
                "11988887777", "joao@exemplo.com", true));
        app.getFiados().definirLimiteCredito(cliente.getId(), new BigDecimal("1000.00"), admin.getId());
        vendaId = registrarVendaPix();
    }

    @Test
    void criacaoRetroativaComJurosNaoReCobraNoPrimeiroRecebimento() {
        Fiado fiado = app.getFiados().gerarFiado(cliente.getId(), vendaId,
                new BigDecimal("100.00"), LocalDate.now().minusDays(10), null, null);

        // 10 dias * 5% a.m. sobre 100.00 = 1.67 ja embutidos na criacao
        assertEquals(0, new BigDecimal("101.67").compareTo(fiado.valorDevedor()),
                fiado.valorDevedor().toString());
        assertNotNull(fiado.ultimoCalculoJuros(),
                "criar com juros ja cobrados deve carimbar o calculo, senao o receber cobra de novo");

        Fiado quitado = app.getFiados().receber(fiado.id(), new BigDecimal("101.67"),
                "DINHEIRO", admin.getId(), null);

        assertEquals("QUITADO", quitado.status());
        assertEquals(0, BigDecimal.ZERO.compareTo(quitado.valorDevedor()));
        assertEquals(0, new BigDecimal("1.67").compareTo(quitado.jurosAplicados()),
                quitado.jurosAplicados().toString());
    }

    @Test
    void recebimentosEmDiasDistintosCobramAPenaOMesmoPeriodoUmaVez() {
        Fiado criado = app.getFiados().gerarFiado(cliente.getId(), vendaId,
                new BigDecimal("100.00"), LocalDate.now(), null, null);
        assertEquals(0, new BigDecimal("100.00").compareTo(criado.valorDevedor()));

        relogio.adiantarDias(10);

        Fiado aposPrimeiro = app.getFiados().receber(criado.id(), new BigDecimal("50.00"),
                "DINHEIRO", admin.getId(), null);
        // 10 dias de atraso: +1.67; devedor 101.67 - 50 = 51.67
        assertEquals(0, new BigDecimal("1.67").compareTo(aposPrimeiro.jurosAplicados()),
                aposPrimeiro.jurosAplicados().toString());
        assertEquals(0, new BigDecimal("51.67").compareTo(aposPrimeiro.valorDevedor()),
                aposPrimeiro.valorDevedor().toString());

        relogio.adiantarDias(1);

        Fiado aposSegundo = app.getFiados().receber(criado.id(), new BigDecimal("50.00"),
                "DINHEIRO", admin.getId(), null);
        // um dia a mais: +0.17 (nao outros 1.67 do periodo inteiro)
        assertEquals(0, new BigDecimal("1.84").compareTo(aposSegundo.jurosAplicados()),
                "juros acumulados devem ser 10+1 dias = 1.84, nao dois periodos: "
                        + aposSegundo.jurosAplicados());
        assertEquals(0, new BigDecimal("1.84").compareTo(aposSegundo.valorDevedor()));
        // re-derivacao da issue #43: parcializou sem quitar e o vencimento ficou para
        // tras -> o status no banco e a verdade do momento, nao um rebaixamento
        assertEquals("INADIMPLENTE", aposSegundo.status(),
                "pagar parcialmente um vencido mantem inadimplente (sem flap nem evento repetido)");
    }

    private long registrarVendaPix() {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        Venda venda = new Venda();
        venda.setUsuarioId(admin.getId());
        venda.setFormaPagamento(FormaPagamento.PIX);
        venda.adicionarItem(new VendaItem(agua.getId(), agua.getNome(), 1, agua.getPrecoVenda()));
        return app.getVendas().registrar(venda).getId();
    }

    private static class RelogioAjustavel extends Clock {

        private Instant agora;
        private final ZoneId zona = ZoneId.systemDefault();

        RelogioAjustavel(java.time.LocalDateTime inicio) {
            agora = inicio.atZone(zona).toInstant();
        }

        void adiantarDias(int dias) {
            agora = agora.plus(dias, ChronoUnit.DAYS);
        }

        @Override
        public ZoneId getZone() {
            return zona;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }
}
