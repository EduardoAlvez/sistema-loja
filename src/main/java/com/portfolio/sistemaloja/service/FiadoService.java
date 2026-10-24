package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.Sessao;
import com.portfolio.sistemaloja.model.Autorizacao;
import com.portfolio.sistemaloja.model.Cliente;
import com.portfolio.sistemaloja.model.Fiado;
import com.portfolio.sistemaloja.model.RecebimentoFiado;
import com.portfolio.sistemaloja.repository.ClienteRepository;
import com.portfolio.sistemaloja.repository.ConfiguracaoRepository;
import com.portfolio.sistemaloja.repository.FiadoRepository;
import com.portfolio.sistemaloja.repository.RecebimentoFiadoRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
                            LocalDate dataVencimento, String observacao) {
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

        BigDecimal limite = cliente.getLimiteCredito();
        BigDecimal saldoDevedor = fiados.saldoDevedorTotalPorCliente(clienteId);
        BigDecimal disponivel = limite.subtract(saldoDevedor);
        if (disponivel.compareTo(valorTotal) < 0) {
            verificarAutorizacaoLimite(clienteId, valorTotal, disponivel);
        }

        BigDecimal juros = calcularJuros(valorTotal, dataVencimento, LocalDate.now());
        BigDecimal valorDevedor = valorTotal.add(juros);

        Fiado fiado = new Fiado(null, vendaId, clienteId, valorTotal,
                BigDecimal.ZERO, valorDevedor, juros,
                dataVencimento, "ABERTO", observacao);
        fiados.salvar(fiado);

        auditoria.registrar("FIADO_GERADO", "FIADO", fiado.id(),
                "cliente " + clienteId + " valor " + fiado.valorTotal().toPlainString()
                        + " vencimento " + dataVencimento);

        return fiado;
    }

    private void verificarAutorizacaoLimite(Long clienteId, BigDecimal valorTotal, BigDecimal disponivel) {
        if (sessao.getAtual() == null || !sessao.getAtual().isAdmin()) {
            throw new ValidacaoException("O cliente excedeu o limite de credito disponível ("
                    + disponivel.toPlainString() + "). E necessario autorizacao de um supervisor.");
        }
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

        // Aplica juros vencidos ate hoje antes de receber
        BigDecimal jurosVencidos = calcularJurosVencidos(fiado);
        if (jurosVencidos.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal novoDevedor = fiado.valorDevedor().add(jurosVencidos);
            fiado = new Fiado(fiado.id(), fiado.vendaId(), fiado.clienteId(),
                    fiado.valorTotal(), fiado.valorPago(), novoDevedor,
                    fiado.jurosAplicados().add(jurosVencidos),
                    fiado.dataVencimento(), fiado.status(), fiado.observacao());
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
                fiado.jurosAplicados(), fiado.dataVencimento(), novoStatus, fiado.observacao());
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
        List<Fiado> vencidos = fiados.listarVencidos(dataReferencia);
        // Aplica juros vencidos em cada fiado vencido
        for (int i = 0; i < vencidos.size(); i++) {
            Fiado f = vencidos.get(i);
            BigDecimal jurosVencidos = calcularJurosVencidos(f);
            if (jurosVencidos.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal novoDevedor = f.valorDevedor().add(jurosVencidos);
                Fiado atualizado = new Fiado(f.id(), f.vendaId(), f.clienteId(),
                        f.valorTotal(), f.valorPago(), novoDevedor,
                        f.jurosAplicados().add(jurosVencidos),
                        f.dataVencimento(), f.status(), f.observacao());
                fiados.salvar(atualizado);
                vencidos.set(i, atualizado);
                auditoria.registrar("JUROS_APLICADOS", "FIADO", f.id(),
                        "juros " + jurosVencidos.toPlainString() + " aplicados ao fiado " + f.id());
            }
        }
        return vencidos;
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
        // Juros simples: valor * taxa * dias / 30
        return valorTotal.multiply(taxa).multiply(BigDecimal.valueOf(diasAtraso))
                .divide(BigDecimal.valueOf(30), 6, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularJurosVencidos(Fiado fiado) {
        return calcularJuros(fiado.valorTotal(), fiado.dataVencimento(), LocalDate.now());
    }

    public boolean fiadoHabilitado() {
        return configuracoes.fiadoHabilitado();
    }

    public BigDecimal taxaJurosFiado() {
        return configuracoes.taxaJurosFiado();
    }
}
