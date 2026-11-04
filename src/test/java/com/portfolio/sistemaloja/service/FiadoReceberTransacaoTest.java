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
import com.portfolio.sistemaloja.repository.EventoAuditoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Trava da divida #45: receber() em quatro escritas soltas podia deixar juro cobrado
 * sem recebimento (ou, no caminho antigo, juro aplicado antes mesmo da validacao de
 * estouro). Agora tudo — fiado atualizado, recebimento e auditorias — vive e morre no
 * mesmo commit, no molde registrarVendaFiado (issue #42).
 */
class FiadoReceberTransacaoTest {

    private Aplicacao app;
    private RelogioAjustavel relogio;
    private Usuario admin;
    private Cliente cliente;
    private long vendaId;

    @BeforeEach
    void preparar() {
        relogio = new RelogioAjustavel(LocalDate.now().atTime(10, 0));
        app = BancoTeste.criar("fiadoreceber" + System.nanoTime(), relogio);
        admin = app.getAuth().autenticar("admin", "admin123");
        BancoTeste.abrirCaixa(app, admin);
        app.getConfiguracoes().definirFiadoHabilitado(true);
        app.getConfiguracoes().definirTaxaJurosFiado(new BigDecimal("5"));
        cliente = app.getClientes().salvar(new Cliente(null, "Rita Alves", "52998224725",
                "11977776666", "rita@exemplo.com", true));
        app.getFiados().definirLimiteCredito(cliente.getId(), new BigDecimal("1000.00"), admin.getId());
        vendaId = registrarVendaPix();
    }

    @Test
    void falhaNoInsertDoRecebimentoDesfazAteOsJurosEAsAuditorias() {
        Fiado criado = gerarFiadoVencidoHaDezDias();
        app.getRecebimentoFiadoRepository().falharAoSalvarParaTeste(conexao -> {
            throw new ValidacaoException("recebimento nao nasceu (falha simulada)");
        });

        assertThrows(ValidacaoException.class, () -> app.getFiados().receber(
                criado.id(), new BigDecimal("50.00"), "DINHEIRO", admin.getId(), null));

        Fiado depois = recarregar(criado.id());
        assertEquals(0, new BigDecimal("100.00").compareTo(depois.valorDevedor()),
                "juros nao podem sobreviver ao rollback do recebimento");
        assertEquals(0, new BigDecimal("0.00").compareTo(depois.jurosAplicados()));
        assertEquals("ABERTO", depois.status());
        assertTrue(app.getRecebimentoFiadoRepository().listarPorFiado(criado.id()).isEmpty());
        List<String> acoes = acoesDeHoje();
        assertFalse(acoes.contains("JUROS_APLICADOS"), acoes.toString());
        assertFalse(acoes.contains("FIADO_RECEBIMENTO"), acoes.toString());
    }

    @Test
    void quitarEscreveFiadoRecebimentoEAuditoriasNoMesmoCommit() {
        Fiado criado = gerarFiadoVencidoHaDezDias();

        Fiado quitado = app.getFiados().receber(criado.id(), new BigDecimal("101.67"),
                "DINHEIRO", admin.getId(), "quitacao");

        assertEquals("QUITADO", quitado.status());
        assertEquals(0, new BigDecimal("1.67").compareTo(quitado.jurosAplicados()));
        assertEquals(0, BigDecimal.ZERO.compareTo(quitado.valorDevedor()));
        assertEquals(1, app.getRecebimentoFiadoRepository().listarPorFiado(criado.id()).size());
        List<String> acoes = acoesDeHoje();
        assertTrue(acoes.contains("JUROS_APLICADOS"), acoes.toString());
        assertTrue(acoes.contains("FIADO_RECEBIMENTO"), acoes.toString());
        assertTrue(acoes.contains("FIADO_QUITADO"), acoes.toString());
    }

    @Test
    void valorAcimaDoDevedorNaoDeixaJuroAplicadoPeloCaminho() {
        Fiado criado = gerarFiadoVencidoHaDezDias();

        assertThrows(ValidacaoException.class, () -> app.getFiados().receber(
                criado.id(), new BigDecimal("200.00"), "DINHEIRO", admin.getId(), null));

        Fiado depois = recarregar(criado.id());
        assertEquals(0, new BigDecimal("100.00").compareTo(depois.valorDevedor()),
                "a validacao de estouro vem antes de qualquer escrita");
        assertEquals(0, new BigDecimal("0.00").compareTo(depois.jurosAplicados()));
        assertFalse(acoesDeHoje().contains("JUROS_APLICADOS"));
        assertTrue(app.getRecebimentoFiadoRepository().listarPorFiado(criado.id()).isEmpty());
    }

    private Fiado gerarFiadoVencidoHaDezDias() {
        Fiado criado = app.getFiados().gerarFiado(cliente.getId(), vendaId,
                new BigDecimal("100.00"), LocalDate.now(), null, null);
        relogio.adiantarDias(10);
        return criado;
    }

    private Fiado recarregar(long fiadoId) {
        return app.getFiados().listarPorCliente(cliente.getId()).stream()
                .filter(f -> f.id() == fiadoId)
                .findFirst()
                .orElseThrow();
    }

    private long registrarVendaPix() {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        Venda venda = new Venda();
        venda.setUsuarioId(admin.getId());
        venda.setFormaPagamento(FormaPagamento.PIX);
        venda.adicionarItem(new VendaItem(agua.getId(), agua.getNome(), 1, agua.getPrecoVenda()));
        return app.getVendas().registrar(venda).getId();
    }

    private List<String> acoesDeHoje() {
        return app.getAuditoria().listar(LocalDate.now(), LocalDate.now()).stream()
                .map(EventoAuditoria::acao)
                .toList();
    }

    private static class RelogioAjustavel extends Clock {

        private Instant agora;
        private final ZoneId zona = ZoneId.systemDefault();

        RelogioAjustavel(LocalDateTime inicio) {
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
