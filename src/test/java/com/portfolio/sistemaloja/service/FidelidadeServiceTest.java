package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.Cliente;
import com.portfolio.sistemaloja.model.ExtratoFidelidade;
import com.portfolio.sistemaloja.model.FidelidadeMovimento;
import com.portfolio.sistemaloja.model.TipoMovimentoFidelidade;
import com.portfolio.sistemaloja.repository.RepositorioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FidelidadeServiceTest {

    private Aplicacao app;
    private Cliente cliente;

    @BeforeEach
    void preparar() {
        app = BancoTeste.criar("fidelidade" + System.nanoTime());
        cliente = app.getClientes().salvar(new Cliente(null, "Maria Souza", "52998224725", "11999998888",
                "maria@exemplo.com", true));
    }

    @Test
    void creditoAumentaOSaldoEApareceNoExtrato() {
        app.getFidelidadeRepository().aplicarMovimentos(List.of(
                app.getFidelidade().credito(cliente.getId(), 10L, 50, "Venda V000001")));

        assertEquals(50, app.getFidelidade().saldo(cliente.getId()));
        List<ExtratoFidelidade> extrato = app.getFidelidade().extrato(cliente.getId());
        assertEquals(1, extrato.size());
        assertEquals(TipoMovimentoFidelidade.CREDITO, extrato.get(0).tipo());
        assertEquals(50, extrato.get(0).pontos());
        assertEquals("Venda V000001", extrato.get(0).descricao());
    }

    @Test
    void debitoBaixaOSaldoEoExtratoFicaDoMaisRecenteParaOMaisAntigo() {
        app.getFidelidadeRepository().aplicarMovimentos(List.of(
                app.getFidelidade().credito(cliente.getId(), 10L, 50, "Venda V000001")));
        app.getFidelidadeRepository().aplicarMovimentos(List.of(
                app.getFidelidade().debito(cliente.getId(), 11L, 20, "Resgate V000002")));

        assertEquals(30, app.getFidelidade().saldo(cliente.getId()));
        List<ExtratoFidelidade> extrato = app.getFidelidade().extrato(cliente.getId());
        assertEquals(2, extrato.size());
        assertEquals(TipoMovimentoFidelidade.DEBITO, extrato.get(0).tipo());
        assertEquals(TipoMovimentoFidelidade.CREDITO, extrato.get(1).tipo());
    }

    @Test
    void recusaDebitoMaiorQueOSaldo() {
        app.getFidelidadeRepository().aplicarMovimentos(List.of(
                app.getFidelidade().credito(cliente.getId(), 10L, 10, "Venda V000001")));

        assertThrows(RepositorioException.class, () -> app.getFidelidadeRepository().aplicarMovimentos(List.of(
                app.getFidelidade().debito(cliente.getId(), 11L, 20, "Resgate V000002"))));
        assertEquals(10, app.getFidelidade().saldo(cliente.getId()));
    }

    @Test
    void debitoRecusadoNaoDeixaMovimentoNoExtrato() {
        app.getFidelidadeRepository().aplicarMovimentos(List.of(
                app.getFidelidade().credito(cliente.getId(), 10L, 10, "Venda V000001")));

        assertThrows(RepositorioException.class, () -> app.getFidelidadeRepository().aplicarMovimentos(List.of(
                app.getFidelidade().debito(cliente.getId(), 11L, 20, "Resgate V000002"))));

        assertEquals(1, app.getFidelidade().extrato(cliente.getId()).size());
    }

    @Test
    void estornoCreditoRetiraOsPontosDoSaldo() {
        app.getFidelidadeRepository().aplicarMovimentos(List.of(
                app.getFidelidade().credito(cliente.getId(), 10L, 50, "Venda V000001")));
        app.getFidelidadeRepository().aplicarMovimentos(List.of(
                app.getFidelidade().estornoCredito(cliente.getId(), 10L, 30, "Estorno V000001")));

        assertEquals(20, app.getFidelidade().saldo(cliente.getId()));
    }

    @Test
    void estornoDebitoDevolveOsPontosResgatadosAoSaldo() {
        app.getFidelidadeRepository().aplicarMovimentos(List.of(
                app.getFidelidade().credito(cliente.getId(), 10L, 50, "Venda V000001")));
        app.getFidelidadeRepository().aplicarMovimentos(List.of(
                app.getFidelidade().debito(cliente.getId(), 11L, 20, "Resgate V000002")));
        app.getFidelidadeRepository().aplicarMovimentos(List.of(
                app.getFidelidade().estornoDebito(cliente.getId(), 11L, 20, "Cancelamento V000002")));

        assertEquals(50, app.getFidelidade().saldo(cliente.getId()));
    }

    @Test
    void movimentoExigeClienteEPontosPositivos() {
        assertThrows(IllegalArgumentException.class, () -> new FidelidadeMovimento(
                0, 1L, null, TipoMovimentoFidelidade.CREDITO, 10, "Sem cliente"));
        assertThrows(IllegalArgumentException.class, () -> new FidelidadeMovimento(
                1, 1L, null, TipoMovimentoFidelidade.CREDITO, 0, "Sem pontos"));
    }
}
