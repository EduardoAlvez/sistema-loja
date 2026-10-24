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

    public FiadoService(FiadoRepository fiados, RecebimentoFiadoRepository recebimentos,
                        ClienteRepository clientes, ConfiguracaoRepository configuracoes,
                        AutorizacaoService autorizacoes, AuditoriaService auditoria, Sessao sessao) {
        this.fiados = fiados;
        this.recebimentos = recebimentos;
        this.clientes = clientes;
        this.configuracoes = configuracoes;
        this.autorizacoes = autorizacoes;
        this.auditoria = auditoria;
        this.sessao = sessao;
    }

    public Fiado gerarFiado(Long clienteId, Long vendaId, BigDecimal valorTotal,
                            LocalDate dataVencimento, String observacao,
                            Autorizacao autorizacao) {
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
        BigDecimal valorDevedorNovo = valorTotal.add(calcularJuros(valorTotal, dataVencimento, LocalDate.now()));
        BigDecimal disponivel = limite.subtract(saldoDevedorExistente);

        if (disponivel.compareTo(valorDevedorNovo) < 0) {
            // Admin esta isento de autorizacao
            Usuario usuarioAtual = sessao.getAtual();
            if (usuarioAtual != null && usuarioAtual.isAdmin()) {
                // Admin pode exceder o limite sem autorizacao
            } else if (autorizacao == null) {
                throw new AutorizacaoNecessariaException(MotivoAutorizacao.LIMITE_CREDITO);
            } else if (autorizacao.operadorId() != sessao.getAtual().getId()) {
                throw new ValidacaoException("A autorizacao nao pertence ao operador da sessao.");
            } else if (autorizacao.motivo() != MotivoAutorizacao.LIMITE_CREDITO) {
                throw new ValidacaoException("A autorizacao nao e para limite de credito.");
            }
        }

        BigDecimal juros = calcularJuros(valorTotal, dataVencimento, LocalDate.now());
        BigDecimal valorDevedor = valorTotal.add(juros);

        Fiado fiado = new Fiado(null, vendaId, clienteId, valorTotal,
                BigDecimal.ZERO, valorDevedor, juros,
                dataVencimento, "ABERTO", observacao, null);
        fiados.salvar(fiado);

        auditoria.registrar("FIADO_GERADO", "FIADO", fiado.id(),
                "cliente " + clienteId + " valor " + fiado.valorTotal().toPlainString()
                        + " vencimento " + dataVencimento);

        return fiado;
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

        String novoStatus;
        if (valorDevedorNovo.compareTo(BigDecimal.ZERO) == 0) {
            novoStatus = "QUITADO";
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

    public List<Fiado> listarAbertos() {
        return fiados.listarAbertos();
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

        // Se ja calculou juros hoje, nao cobra de novo
        if (ultimoCalculo != null && ultimoCalculo.toLocalDate().equals(dataReferencia)) {
            return BigDecimal.ZERO;
        }

        return calcularJuros(fiado.valorTotal(), fiado.dataVencimento(), dataReferencia);
    }

    public boolean fiadoHabilitado() {
        return configuracoes.fiadoHabilitado();
    }

    public BigDecimal taxaJurosFiado() {
        return configuracoes.taxaJurosFiado();
    }
}
