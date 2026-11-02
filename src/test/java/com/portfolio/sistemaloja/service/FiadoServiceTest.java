package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.Autorizacao;
import com.portfolio.sistemaloja.model.Cliente;
import com.portfolio.sistemaloja.model.Fiado;
import com.portfolio.sistemaloja.model.FormaPagamento;
import com.portfolio.sistemaloja.model.MotivoAutorizacao;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.model.VendaItem;
import com.portfolio.sistemaloja.repository.EventoAuditoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cobertura do modulo fiado (venda a prazo): habilitacao, validacoes, limite composto,
 * autorizacao de supervisor, admin que fura limite, recebimento parcial/total,
 * vencidos read-only e administracao do limite -- sempre com a trilha de auditoria
 * afirmada no banco.
 */
class FiadoServiceTest {

    private Aplicacao app;
    private String nomeBanco;
    private Usuario admin;
    private Cliente cliente;

    @BeforeEach
    void preparar() {
        nomeBanco = "fiado" + System.nanoTime();
        app = BancoTeste.criar(nomeBanco);
        admin = app.getAuth().autenticar("admin", "admin123");
        BancoTeste.abrirCaixa(app, admin);
        app.getConfiguracoes().definirFiadoHabilitado(true);
        cliente = app.getClientes().salvar(new Cliente(null, "Joao Lima", "52998224725",
                "11988887777", "joao@exemplo.com", true));
    }

    @Test
    void gerarFiadoFelizGravaAbertoEAuditoria() {
        app.getFiados().definirLimiteCredito(cliente.getId(), new BigDecimal("500.00"), admin.getId());
        long vendaId = registrarVendaPix();

        Fiado fiado = app.getFiados().gerarFiado(cliente.getId(), vendaId,
                new BigDecimal("100.00"), LocalDate.now().plusDays(30), "parcela unica", null);

        assertNotNull(fiado.id());
        assertEquals("ABERTO", fiado.status());
        assertEquals(0, new BigDecimal("100.00").compareTo(fiado.valorDevedor()));
        assertEquals(0, BigDecimal.ZERO.compareTo(fiado.jurosAplicados()));
        assertNull(fiado.ultimoCalculoJuros(), "sem juros na criacao, sem carimbo");
        assertTrue(acoesDeHoje().contains("FIADO_GERADO"));
        assertTrue(app.getFiados().fiadoHabilitado());
    }

    @Test
    void fiadoDesabilitadoRecusaEGeradoNaoAudita() {
        app.getConfiguracoes().definirFiadoHabilitado(false);

        ValidacaoException erro = assertThrows(ValidacaoException.class,
                () -> app.getFiados().gerarFiado(cliente.getId(), 1L, new BigDecimal("10.00"),
                        LocalDate.now().plusDays(1), null, null));
        assertTrue(erro.getMessage().contains("desabilitado"), erro.getMessage());
        assertFalse(acoesDeHoje().contains("FIADO_GERADO"));
    }

    @Test
    void validacoesDeEntradaNaGeracao() {
        app.getFiados().definirLimiteCredito(cliente.getId(), new BigDecimal("500.00"), admin.getId());
        long vendaId = registrarVendaPix();

        assertThrows(ValidacaoException.class, () -> app.getFiados().gerarFiado(cliente.getId(),
                vendaId, BigDecimal.ZERO, LocalDate.now().plusDays(1), null, null));
        assertThrows(ValidacaoException.class, () -> app.getFiados().gerarFiado(cliente.getId(),
                vendaId, new BigDecimal("10.00"), null, null, null));
        ValidacaoException semCliente = assertThrows(ValidacaoException.class,
                () -> app.getFiados().gerarFiado(99999L, vendaId, new BigDecimal("10.00"),
                        LocalDate.now().plusDays(1), null, null));
        assertTrue(semCliente.getMessage().contains("Cliente nao encontrado"), semCliente.getMessage());

        Cliente inativo = app.getClientes().salvar(new Cliente(null, "Ana Piosa", "12345678909",
                "11966665555", "ana@exemplo.com", true));
        app.getClientes().salvar(new Cliente(inativo.getId(), inativo.getNome(), inativo.getCpf(),
                inativo.getTelefone(), inativo.getEmail(), false));
        ValidacaoException erro = assertThrows(ValidacaoException.class,
                () -> app.getFiados().gerarFiado(inativo.getId(), vendaId, new BigDecimal("10.00"),
                        LocalDate.now().plusDays(1), null, null));
        assertTrue(erro.getMessage().contains("desativado"), erro.getMessage());
    }

    @Test
    void limiteExatoPassaSemPinUMCentavoAcimaExigeAutorizacao() {
        app.getFiados().definirLimiteCredito(cliente.getId(), new BigDecimal("100.00"), admin.getId());
        long vendaId = registrarVendaPix();
        app.getAuth().autenticar("operador", "caixa123");

        app.getFiados().gerarFiado(cliente.getId(), vendaId, new BigDecimal("100.00"),
                LocalDate.now().plusDays(10), null, null);

        ValidacaoException erro = assertThrows(AutorizacaoNecessariaException.class,
                () -> app.getFiados().gerarFiado(cliente.getId(), vendaId, new BigDecimal("100.01"),
                        LocalDate.now().plusDays(10), null, null));
        assertNotNull(erro);
        assertEquals(1, app.getFiados().listarPorCliente(cliente.getId()).size(),
                "a tentativa recusada nao pode ter persistido fiado");
    }

    @Test
    void supervisorAutorizaExcessoDeLimite() {
        app.getFiados().definirLimiteCredito(cliente.getId(), new BigDecimal("100.00"), admin.getId());
        long vendaId = registrarVendaPix();

        Autorizacao autorizacao = BancoTeste.autorizarSupervisor(app, MotivoAutorizacao.LIMITE_CREDITO);
        Fiado fiado = app.getFiados().gerarFiado(cliente.getId(), vendaId, new BigDecimal("150.00"),
                LocalDate.now().plusDays(10), null, autorizacao);

        assertEquals("ABERTO", fiado.status());
        assertTrue(acoesDeHoje().contains("AUTORIZACAO_SUPERVISOR"));
    }

    @Test
    void adminFuraLimiteSemPrecisarDePin() {
        app.getFiados().definirLimiteCredito(cliente.getId(), new BigDecimal("100.00"), admin.getId());
        long vendaId = registrarVendaPix();

        Fiado fiado = app.getFiados().gerarFiado(cliente.getId(), vendaId, new BigDecimal("500.00"),
                LocalDate.now().plusDays(10), null, null);
        assertNotNull(fiado.id());
    }

    @Test
    void marcadoFicaInadimplenteIdempotenteEAudita() {
        app.getFiados().definirLimiteCredito(cliente.getId(), new BigDecimal("500.00"), admin.getId());
        long vendaId = registrarVendaPix();
        Fiado vencido = app.getFiados().gerarFiado(cliente.getId(), vendaId, new BigDecimal("80.00"),
                LocalDate.now().minusDays(2), null, null);

        assertEquals(1, app.getFiados().marcarVencidos(LocalDate.now()));
        assertEquals("INADIMPLENTE",
                app.getFiados().listarPorCliente(cliente.getId()).get(0).status());
        assertTrue(acoesDeHoje().contains("FIADO_INADIMPLENTE"));

        assertEquals(0, app.getFiados().marcarVencidos(LocalDate.now()), "re-rodar nao marca de novo");
        long eventos = app.getAuditoria().listar(LocalDate.now(), LocalDate.now()).stream()
                .filter(e -> "FIADO_INADIMPLENTE".equals(e.acao()) && vencido.id().equals(e.entidadeId()))
                .count();
        assertEquals(1, eventos, "um fiado, um evento de transicao");
    }

    @Test
    void inadimplenteContaNoSaldoEConsomeLimite() {
        app.getFiados().definirLimiteCredito(cliente.getId(), new BigDecimal("100.00"), admin.getId());
        long vendaId = registrarVendaPix();
        app.getFiados().gerarFiado(cliente.getId(), vendaId, new BigDecimal("100.00"),
                LocalDate.now().minusDays(1), null, null);
        app.getFiados().marcarVencidos();

        assertEquals(0, new BigDecimal("100.00").compareTo(
                app.getFiados().saldoDevedorPorCliente(cliente.getId())),
                "INADIMPLENTE fora do saldo = credito de graca para o caloteiro");

        app.getAuth().autenticar("operador", "caixa123");
        assertThrows(AutorizacaoNecessariaException.class,
                () -> app.getFiados().gerarFiado(cliente.getId(), vendaId, new BigDecimal("1.00"),
                        LocalDate.now().plusDays(5), null, null),
                "inadimplencia precisa bloquear nova compra a prazo sem PIN");
    }

    @Test
    void saldoDevedorCompoeAbertoEMaisParcialEExcluiQuitado() {
        app.getFiados().definirLimiteCredito(cliente.getId(), new BigDecimal("200.00"), admin.getId());
        long vendaId = registrarVendaPix();

        Fiado primeiro = app.getFiados().gerarFiado(cliente.getId(), vendaId,
                new BigDecimal("100.00"), LocalDate.now().plusDays(10), null, null);
        assertEquals(0, new BigDecimal("100.00").compareTo(
                app.getFiados().saldoDevedorPorCliente(cliente.getId())));

        app.getFiados().receber(primeiro.id(), new BigDecimal("40.00"), "DINHEIRO", admin.getId(), null);
        assertEquals(0, new BigDecimal("60.00").compareTo(
                app.getFiados().saldoDevedorPorCliente(cliente.getId())), "PARCIAL conta no saldo");

        app.getAuth().autenticar("operador", "caixa123");
        assertThrows(AutorizacaoNecessariaException.class,
                () -> app.getFiados().gerarFiado(cliente.getId(), vendaId, new BigDecimal("140.01"),
                        LocalDate.now().plusDays(10), null, null));
        app.getAuth().autenticar("admin", "admin123");

        app.getFiados().receber(primeiro.id(), new BigDecimal("60.00"), "DINHEIRO", admin.getId(), null);
        assertEquals(0, BigDecimal.ZERO.compareTo(
                app.getFiados().saldoDevedorPorCliente(cliente.getId())), "QUITADO nao conta");

        Fiado terceiro = app.getFiados().gerarFiado(cliente.getId(), vendaId, new BigDecimal("140.00"),
                LocalDate.now().plusDays(10), null, null);
        assertNotNull(terceiro.id());
    }

    @Test
    void recebimentoParcialGravaMovimentoSemQuitacao() {
        app.getFiados().definirLimiteCredito(cliente.getId(), new BigDecimal("200.00"), admin.getId());
        long vendaId = registrarVendaPix();
        Fiado fiado = app.getFiados().gerarFiado(cliente.getId(), vendaId, new BigDecimal("100.00"),
                LocalDate.now().plusDays(10), null, null);

        Fiado apos = app.getFiados().receber(fiado.id(), new BigDecimal("30.00"),
                "PIX", admin.getId(), "sinal");

        assertEquals("PARCIAL", apos.status());
        assertEquals(0, new BigDecimal("30.00").compareTo(apos.valorPago()));
        assertEquals(0, new BigDecimal("70.00").compareTo(apos.valorDevedor()));
        List<String> acoes = acoesDeHoje();
        assertTrue(acoes.contains("FIADO_RECEBIMENTO"));
        assertFalse(acoes.contains("FIADO_QUITADO"));
    }

    @Test
    void quitacaoExataEscalaIndependenteFechaEBlindaContraRecebimentoExtra() {
        app.getFiados().definirLimiteCredito(cliente.getId(), new BigDecimal("200.00"), admin.getId());
        long vendaId = registrarVendaPix();
        Fiado fiado = app.getFiados().gerarFiado(cliente.getId(), vendaId, new BigDecimal("100.00"),
                LocalDate.now().plusDays(10), null, null);

        Fiado quitado = app.getFiados().receber(fiado.id(), new BigDecimal("100.000"),
                "DINHEIRO", admin.getId(), null);

        assertEquals("QUITADO", quitado.status());
        assertEquals(0, BigDecimal.ZERO.compareTo(quitado.valorDevedor()));
        List<String> acoes = acoesDeHoje();
        assertTrue(acoes.contains("FIADO_RECEBIMENTO"));
        assertTrue(acoes.contains("FIADO_QUITADO"));

        assertThrows(ValidacaoException.class, () -> app.getFiados().receber(fiado.id(),
                new BigDecimal("10.00"), "DINHEIRO", admin.getId(), null));
    }

    @Test
    void valorAcimaDoDevedorErecusadoSemAlterarFiado() {
        app.getFiados().definirLimiteCredito(cliente.getId(), new BigDecimal("200.00"), admin.getId());
        long vendaId = registrarVendaPix();
        Fiado fiado = app.getFiados().gerarFiado(cliente.getId(), vendaId, new BigDecimal("100.00"),
                LocalDate.now().plusDays(10), null, null);

        ValidacaoException erro = assertThrows(ValidacaoException.class,
                () -> app.getFiados().receber(fiado.id(), new BigDecimal("100.01"),
                        "DINHEIRO", admin.getId(), null));
        assertTrue(erro.getMessage().contains("excede"), erro.getMessage());

        Fiado intacto = app.getFiados().listarPorCliente(cliente.getId()).get(0);
        assertEquals("ABERTO", intacto.status());
        assertEquals(0, new BigDecimal("100.00").compareTo(intacto.valorDevedor()));
    }

    @Test
    void listarVencidosSoPreveNaoPersiste() throws Exception {
        app.getFiados().definirLimiteCredito(cliente.getId(), new BigDecimal("500.00"), admin.getId());
        app.getConfiguracoes().definirTaxaJurosFiado(new BigDecimal("5"));
        long vendaId = registrarVendaPix();
        Fiado fiado = app.getFiados().gerarFiado(cliente.getId(), vendaId, new BigDecimal("100.00"),
                LocalDate.now(), null, null);

        try (Connection conexao = BancoTeste.conexao(nomeBanco);
             PreparedStatement ps = conexao.prepareStatement(
                     "UPDATE fiados SET data_vencimento = ? WHERE id = ?")) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDate.now().minusDays(10).atStartOfDay()));
            ps.setLong(2, fiado.id());
            ps.executeUpdate();
        }

        List<Fiado> vencidos = app.getFiados().listarVencidos(LocalDate.now());
        assertEquals(1, vencidos.size());
        assertEquals(0, new BigDecimal("101.67").compareTo(vencidos.get(0).valorDevedor()),
                "previsao em memoria dos 10 dias de atraso: " + vencidos.get(0).valorDevedor());

        Fiado persistido = app.getFiados().listarPorCliente(cliente.getId()).get(0);
        assertEquals(0, new BigDecimal("100.00").compareTo(persistido.valorDevedor()),
                "listarVencidos nao pode gravar nada");
        assertFalse(acoesDeHoje().contains("JUROS_APLICADOS"));
    }

    @Test
    void definirLimiteValidaRejeitaAbaixoDoSaldoEAudita() {
        assertThrows(ValidacaoException.class,
                () -> app.getFiados().definirLimiteCredito(cliente.getId(), new BigDecimal("-1"), admin.getId()));
        assertThrows(ValidacaoException.class,
                () -> app.getFiados().definirLimiteCredito(cliente.getId(), null, admin.getId()));
        assertThrows(ValidacaoException.class,
                () -> app.getFiados().definirLimiteCredito(99999L, new BigDecimal("100"), admin.getId()));

        app.getFiados().definirLimiteCredito(cliente.getId(), new BigDecimal("100.00"), admin.getId());
        long vendaId = registrarVendaPix();
        app.getFiados().gerarFiado(cliente.getId(), vendaId, new BigDecimal("100.00"),
                LocalDate.now().plusDays(10), null, null);

        ValidacaoException erro = assertThrows(ValidacaoException.class,
                () -> app.getFiados().definirLimiteCredito(cliente.getId(), new BigDecimal("50.00"),
                        admin.getId()));
        assertTrue(erro.getMessage().contains("100.00"), erro.getMessage());

        assertTrue(acoesDeHoje().contains("LIMITE_CREDITO_ALTERADO"));
        assertEquals(0, new BigDecimal("100.00").compareTo(
                app.getClientes().listarAtivos().stream()
                        .filter(c -> c.getId().equals(cliente.getId()))
                        .findFirst().orElseThrow().getLimiteCredito()));
    }

    @Test
    void listarPendentesVemPorVencimentoEExcluiQuitado() {
        app.getFiados().definirLimiteCredito(cliente.getId(), new BigDecimal("500.00"), admin.getId());
        long vendaId = registrarVendaPix();
        Fiado longoPrazo = app.getFiados().gerarFiado(cliente.getId(), vendaId, new BigDecimal("100.00"),
                LocalDate.now().plusDays(30), null, null);
        app.getFiados().gerarFiado(cliente.getId(), vendaId, new BigDecimal("50.00"),
                LocalDate.now().plusDays(5), null, null);
        app.getFiados().receber(longoPrazo.id(), new BigDecimal("100.00"), "PIX", admin.getId(), null);

        List<Fiado> pendentes = app.getFiados().listarPendentes();

        assertEquals(1, pendentes.size(), "QUITADO nao pode voltar a fila");
        assertEquals(0, new BigDecimal("50.00").compareTo(pendentes.get(0).valorTotal()));
    }

    @Test
    void configDeFiadoExigeAdmin() {
        app.getAuth().autenticar("operador", "caixa123");
        ValidacaoException erro = assertThrows(ValidacaoException.class,
                () -> app.getConfiguracoes().definirFiadoHabilitado(false));
        assertTrue(erro.getMessage().contains("administrador"), erro.getMessage());
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
}
