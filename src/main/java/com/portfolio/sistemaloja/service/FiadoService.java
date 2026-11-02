package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.Sessao;
import com.portfolio.sistemaloja.model.Autorizacao;
import com.portfolio.sistemaloja.model.Cliente;
import com.portfolio.sistemaloja.model.Fiado;
import com.portfolio.sistemaloja.model.MotivoAutorizacao;
import com.portfolio.sistemaloja.model.RecebimentoFiado;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.repository.ClienteRepository;
import com.portfolio.sistemaloja.repository.ConfiguracaoRepository;
import com.portfolio.sistemaloja.repository.FiadoRepository;
import com.portfolio.sistemaloja.repository.RecebimentoFiadoRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class FiadoService {

    private final FiadoRepository fiados;
    private final RecebimentoFiadoRepository recebimentos;
    private final ClienteRepository clientes;
    private final ConfiguracaoRepository configuracoes;
    private final AutorizacaoService autorizacoes;
    private final AuditoriaService auditoria;
    private final Sessao sessao;
    private final com.portfolio.sistemaloja.db.ConexaoFonte fonte;

    public FiadoService(FiadoRepository fiados, RecebimentoFiadoRepository recebimentos,
                        ClienteRepository clientes, ConfiguracaoRepository configuracoes,
                        AutorizacaoService autorizacoes, AuditoriaService auditoria, Sessao sessao,
                        com.portfolio.sistemaloja.db.ConexaoFonte fonte) {
        this.fiados = fiados;
        this.recebimentos = recebimentos;
        this.clientes = clientes;
        this.configuracoes = configuracoes;
        this.autorizacoes = autorizacoes;
        this.auditoria = auditoria;
        this.sessao = sessao;
        this.fonte = fonte;
    }

    public Fiado gerarFiado(Long clienteId, Long vendaId, BigDecimal valorTotal,
                            LocalDate dataVencimento, String observacao,
                            Autorizacao autorizacao) {
        ContaFiado conta = preValidar(clienteId, valorTotal, dataVencimento, autorizacao);
        Fiado fiado = new Fiado(null, vendaId, clienteId, conta.valorTotal(),
                BigDecimal.ZERO, conta.valorDevedor(), conta.juros(),
                dataVencimento, "ABERTO", observacao, conta.estampaInicialDeCalculo());
        Fiado salvo = fiados.salvar(fiado);

        auditoria.registrar("FIADO_GERADO", "FIADO", salvo.id(),
                "cliente " + clienteId + " valor " + salvo.valorTotal().toPlainString()
                        + " vencimento " + dataVencimento);

        return salvo;
    }

    /**
     * Conta do fiado pre-validada: tudo o que {@link #gerarFiado} precisa saber antes de
     * existir um fiado, sem escrever nada. Existe para que a venda a prazo do PDV possa
     * validar o limite ANTES de cometer a venda (issue #42: nada pode ser persistido para
     * depois descobrir que faltava PIN).
     */
    public ContaFiado preValidar(Long clienteId, BigDecimal valorTotal,
                                 LocalDate dataVencimento, Autorizacao autorizacao) {
        if (!configuracoes.fiadoHabilitado()) {
            throw new ValidacaoException("O fiado esta desabilitado para esta loja. Peca ao administrador habilitar.");
        }
        if (valorTotal == null || valorTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidacaoException("O valor do fiado deve ser maior que zero.");
        }
        if (dataVencimento == null) {
            throw new ValidacaoException("Informe a data de vencimento.");
        }

        Cliente cliente = clientes.buscarPorId(clienteId)
                .orElseThrow(() -> new ValidacaoException("Cliente nao encontrado."));
        if (!cliente.isAtivo()) {
            throw new ValidacaoException("O cliente esta desativado e nao pode fazer compra a prazo.");
        }

        // Verifica limite de credito (considerando ja o devedor com juros)
        BigDecimal limite = cliente.getLimiteCredito();
        BigDecimal saldoDevedorExistente = fiados.saldoDevedorTotalPorCliente(clienteId);
        BigDecimal juros = calcularJuros(valorTotal, dataVencimento, LocalDate.now());
        BigDecimal valorDevedor = valorTotal.add(juros);
        BigDecimal disponivel = limite.subtract(saldoDevedorExistente);

        if (disponivel.compareTo(valorDevedor) < 0) {
            // Excede o limite: exige autorizacao de supervisor (PIN)
            autorizacoes.exigir(sessao.getAtual(), autorizacao, MotivoAutorizacao.LIMITE_CREDITO);
        }

        // Se a criacao ja assou juros retroativos, o carimbo de calculo nasce agora:
        // sem ele, o primeiro receber recobraria o mesmo vencimento->hoje por cima.
        LocalDateTime estampaInicialDeCalculo = juros.compareTo(BigDecimal.ZERO) > 0
                ? LocalDateTime.now()
                : null;

        return new ContaFiado(valorTotal, valorDevedor, juros, estampaInicialDeCalculo);
    }

    /**
     * Persiste uma conta pre-validada dentro da transacao do chamador (venda + fiado
     * atomicos, issue #42). Audita com a mesma conexao: se o commit nao vier, o evento
     * tampouco existe.
     */
    public Fiado salvarPreValidado(Long clienteId, Long vendaId, LocalDate dataVencimento,
                                   ContaFiado conta, String observacao,
                                   java.sql.Connection conexao) throws java.sql.SQLException {
        Fiado fiado = new Fiado(null, vendaId, clienteId, conta.valorTotal(),
                BigDecimal.ZERO, conta.valorDevedor(), conta.juros(),
                dataVencimento, "ABERTO", observacao, conta.estampaInicialDeCalculo());
        Fiado salvo = fiados.salvar(fiado, conexao);
        auditoria.registrarComo(loginDaSessao(), "FIADO_GERADO", "FIADO", salvo.id(),
                "cliente " + clienteId + " valor " + salvo.valorTotal().toPlainString()
                        + " vencimento " + dataVencimento, conexao);
        return salvo;
    }

    private String loginDaSessao() {
        Usuario atual = sessao.getAtual();
        return atual == null ? "sistema" : atual.getLogin();
    }

    public record ContaFiado(BigDecimal valorTotal, BigDecimal valorDevedor, BigDecimal juros,
                             LocalDateTime estampaInicialDeCalculo) {
    }

    public Fiado receber(Long fiadoId, BigDecimal valor, String formaPagamento,
                         Long usuarioId, String observacao) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidacaoException("O valor do recebimento deve ser maior que zero.");
        }

        Fiado fiado = fiados.buscarPorId(fiadoId)
                .orElseThrow(() -> new ValidacaoException("Fiado nao encontrado."));

        if (fiado.isQuitado()) {
            throw new ValidacaoException("Este ja foi quitado.");
        }

        // Aplica juros vencidos ate hoje (incremental, so o que nao foi cobrado ainda)
        BigDecimal jurosVencidos = calcularJurosVencidos(fiado);
        if (jurosVencidos.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal novoDevedor = fiado.valorDevedor().add(jurosVencidos);
            fiado = new Fiado(fiado.id(), fiado.vendaId(), fiado.clienteId(),
                    fiado.valorTotal(), fiado.valorPago(), novoDevedor,
                    fiado.jurosAplicados().add(jurosVencidos),
                    fiado.dataVencimento(), fiado.status(), fiado.observacao(),
                    LocalDateTime.now());
            fiados.salvar(fiado);
            auditoria.registrar("JUROS_APLICADOS", "FIADO", fiado.id(),
                    "juros " + jurosVencidos.toPlainString() + " aplicados ao fiado " + fiadoId);
        }

        BigDecimal valorPagoNovo = fiado.valorPago().add(valor);
        BigDecimal valorDevedorNovo = fiado.valorDevedor().subtract(valor);
        if (valorDevedorNovo.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidacaoException("O valor recebido excede o devedor do fiado.");
        }

        // Re-derivacao (issue #43): o status persistido eh sempre a verdade do momento,
        // sem depender de o painel ter rodado a marcacao antes. Um vencido que recebe
        // e nao quita segue INADIMPLENTE — rebaixar para PARCIAL o faria "esquentar"
        // de novo na proxima marcacao, com evento de auditoria repetido a cada visita.
        // O guard acima continua bloqueando apenas QUITADO: inadimplente tem de poder pagar.
        String novoStatus;
        if (valorDevedorNovo.compareTo(BigDecimal.ZERO) == 0) {
            novoStatus = "QUITADO";
        } else if (fiado.isInadimplente() || fiado.dataVencimento().isBefore(LocalDate.now())) {
            novoStatus = "INADIMPLENTE";
        } else {
            novoStatus = "PARCIAL";
        }

        RecebimentoFiado recebimento = new RecebimentoFiado(null, fiadoId, LocalDateTime.now(),
                valor, formaPagamento, usuarioId, observacao);
        recebimentos.salvar(recebimento);

        Fiado atualizado = new Fiado(fiado.id(), fiado.vendaId(), fiado.clienteId(),
                fiado.valorTotal(), valorPagoNovo, valorDevedorNovo,
                fiado.jurosAplicados(), fiado.dataVencimento(), novoStatus, fiado.observacao(),
                fiado.ultimoCalculoJuros());
        fiados.salvar(atualizado);

        auditoria.registrar("FIADO_RECEBIMENTO", "FIADO", fiado.id(),
                "recebido " + valor.toPlainString() + " forma " + formaPagamento
                        + " devedor restante " + valorDevedorNovo.toPlainString());

        if (novoStatus.equals("QUITADO")) {
            auditoria.registrar("FIADO_QUITADO", "FIADO", fiado.id(),
                    "fiado quitado pelo cliente " + fiado.clienteId());
        }

        return atualizado;
    }

    public List<Fiado> listarPendentes() {
        return fiados.listarPendentes();
    }

    public List<Fiado> listarVencidos(LocalDate dataReferencia) {
        if (dataReferencia == null) {
            dataReferencia = LocalDate.now();
        }
        // Nao persiste alteracoes: apenas retorna os vencidos com juros calculados em memoria
        List<Fiado> vencidos = fiados.listarVencidos(dataReferencia);
        List<Fiado> resultado = new ArrayList<>();
        for (Fiado f : vencidos) {
            BigDecimal jurosVencidos = calcularJurosVencidos(f);
            if (jurosVencidos.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal novoDevedor = f.valorDevedor().add(jurosVencidos);
                resultado.add(new Fiado(f.id(), f.vendaId(), f.clienteId(),
                        f.valorTotal(), f.valorPago(), novoDevedor,
                        f.jurosAplicados().add(jurosVencidos),
                        f.dataVencimento(), f.status(), f.observacao(),
                        f.ultimoCalculoJuros()));
            } else {
                resultado.add(f);
            }
        }
        return resultado;
    }

    /**
     * Marca fiados vencidos como INADIMPLENTE e audita cada transicao (issue #43).
     * Chamado ao abrir/atualizar o painel — a marcação é idempotente: quem roda de
     * novo nao encontra mais ABERTO/PARCIAL vencido, e o lock do SELECT impede que
     * dois paineis emitam o mesmo evento.
     */
    public int marcarVencidos() {
        return marcarVencidos(LocalDate.now());
    }

    public int marcarVencidos(LocalDate dataReferencia) {
        LocalDate referencia = dataReferencia == null ? LocalDate.now() : dataReferencia;
        try (java.sql.Connection conexao = fonte.getConnection()) {
            try {
                conexao.setAutoCommit(false);
                List<Long> marcados = fiados.marcarVencidos(referencia, conexao);
                for (Long id : marcados) {
                    auditoria.registrarComo(loginDaSessao(), "FIADO_INADIMPLENTE", "FIADO", id,
                            "vencimento anterior a " + referencia, conexao);
                }
                conexao.commit();
                return marcados.size();
            } catch (RuntimeException e) {
                rollbackSilencioso(conexao, e);
                throw e;
            } catch (java.sql.SQLException e) {
                rollbackSilencioso(conexao, e);
                throw new ValidacaoException("Erro ao marcar fiados vencidos.", e);
            }
        } catch (java.sql.SQLException e) {
            throw new ValidacaoException("Erro ao marcar fiados vencidos.", e);
        }
    }

    private void rollbackSilencioso(java.sql.Connection conexao, Exception causa) {
        try {
            conexao.rollback();
        } catch (java.sql.SQLException rollback) {
            causa.addSuppressed(rollback);
        }
    }

    public List<Fiado> listarPorCliente(Long clienteId) {
        return fiados.listarPorCliente(clienteId);
    }

    public BigDecimal saldoDevedorPorCliente(Long clienteId) {
        return fiados.saldoDevedorTotalPorCliente(clienteId);
    }

    public void definirLimiteCredito(Long clienteId, BigDecimal limite, Long usuarioId) {
        if (limite == null || limite.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidacaoException("O limite de credito deve ser maior ou igual a zero.");
        }
        Cliente cliente = clientes.buscarPorId(clienteId)
                .orElseThrow(() -> new ValidacaoException("Cliente nao encontrado."));

        // Nao permite definir limite abaixo do saldo devedor atual
        BigDecimal saldoDevedor = fiados.saldoDevedorTotalPorCliente(clienteId);
        if (limite.compareTo(saldoDevedor) < 0) {
            throw new ValidacaoException("O limite nao pode ser menor que o saldo devedor atual ("
                    + saldoDevedor.toPlainString() + ").");
        }

        cliente.setLimiteCredito(limite);
        clientes.salvar(cliente);
        auditoria.registrar("LIMITE_CREDITO_ALTERADO", "CLIENTE", clienteId,
                "limite " + limite.toPlainString());
    }

    private BigDecimal calcularJuros(BigDecimal valorTotal, LocalDate dataVencimento, LocalDate dataReferencia) {
        BigDecimal taxa = configuracoes.taxaJurosFiado();
        if (taxa == null || taxa.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (dataReferencia.isBefore(dataVencimento) || dataReferencia.equals(dataVencimento)) {
            return BigDecimal.ZERO;
        }
        long diasAtraso = ChronoUnit.DAYS.between(dataVencimento, dataReferencia);
        if (diasAtraso <= 0) {
            return BigDecimal.ZERO;
        }
        // Juros simples: valor * taxa(%) * dias / 30
        // taxa vem em percentual (ex: 5 = 5%), entao divide por 100
        return valorTotal.multiply(taxa.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP))
                .multiply(BigDecimal.valueOf(diasAtraso))
                .divide(BigDecimal.valueOf(30), 6, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularJurosVencidos(Fiado fiado) {
        LocalDateTime ultimoCalculo = fiado.ultimoCalculoJuros();
        LocalDate dataReferencia = LocalDate.now();

        // Guarda intra-dia: hoje ja calculado nao cobra de novo. Redundante com a base
        // incremental abaixo (mesmo dia => 0 dias => ZERO), mas barata e mantem a
        // protecao explicita contra recálculos repetidos no mesmo dia.
        if (ultimoCalculo != null && ultimoCalculo.toLocalDate().equals(dataReferencia)) {
            return BigDecimal.ZERO;
        }

        // Base incremental: cobra apenas do ultimo calculo (se posterior ao vencimento)
        // ate hoje. Recalcular sempre vencimento->hoje faria recebimentos em dias
        // distintos cobrarem o mesmo periodo de atraso mais de uma vez.
        LocalDate base = fiado.dataVencimento();
        if (ultimoCalculo != null && ultimoCalculo.toLocalDate().isAfter(base)) {
            base = ultimoCalculo.toLocalDate();
        }
        return calcularJuros(fiado.valorTotal(), base, dataReferencia);
    }

    public boolean fiadoHabilitado() {
        return configuracoes.fiadoHabilitado();
    }

    public BigDecimal taxaJurosFiado() {
        return configuracoes.taxaJurosFiado();
    }
}
