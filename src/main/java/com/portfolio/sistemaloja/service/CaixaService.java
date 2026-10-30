package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.model.Autorizacao;
import com.portfolio.sistemaloja.model.Caixa;
import com.portfolio.sistemaloja.model.CaixaLancamento;
import com.portfolio.sistemaloja.model.FormaPagamento;
import com.portfolio.sistemaloja.model.MotivoAutorizacao;
import com.portfolio.sistemaloja.model.TipoLancamentoCaixa;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.repository.CaixaRepository;
import com.portfolio.sistemaloja.repository.VendaRepository;
import com.portfolio.sistemaloja.tef.OperadoraCartaoFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Serviço responsável por abertura, movimentações e fechamento de caixa.
 * <p>
 * Regras principais:
 * <ul>
 *   <li>Não permite abrir novo caixa enquanto houver um aberto.</li>
 *   <li>Sangria/suprimento exigem caixa aberto.</li>
 *   <li>Fechamento calcula saldo esperado (fundo + suprimentos + vendas - sangrias).</li>
 *   <li>Suporta conciliação TEF via interface abstrata.</li>
 * </ul>
 */
public class CaixaService {

    private static final Logger LOG = LogManager.getLogger(CaixaService.class);

    private final CaixaRepository caixas;
    private final VendaRepository vendas;
    private final AuditoriaService auditoria;
    private final AutorizacaoService autorizacoes;

    public CaixaService(CaixaRepository caixas, VendaRepository vendas, AuditoriaService auditoria) {
        this(caixas, vendas, auditoria, null);
    }

    public CaixaService(CaixaRepository caixas, VendaRepository vendas, AuditoriaService auditoria,
                        AutorizacaoService autorizacoes) {
        this.caixas = caixas;
        this.vendas = vendas;
        this.auditoria = auditoria;
        this.autorizacoes = autorizacoes;
    }

    /**
     * Abre um novo caixa.
     * 
     * @param valorAbertura fundo inicial
     * @param usuario usuário que abre o caixa
     * @return caixa aberto
     * @throws ValidacaoException se já houver caixa aberto ou usuário inválido
     */
    public Caixa abrir(BigDecimal valorAbertura, Usuario usuario) {
        if (usuario == null) {
            throw new ValidacaoException("É necessário estar autenticado para abrir o caixa.");
        }
        if (caixas.buscarAberto().isPresent()) {
            throw new ValidacaoException("Já existe um caixa aberto. Feche-o antes de abrir outro.");
        }
        BigDecimal valor = valorAbertura == null ? BigDecimal.ZERO : valorAbertura;
        if (valor.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidacaoException("O valor de abertura não pode ser negativo.");
        }
        Caixa caixa = new Caixa();
        caixa.setUsuarioAberturaId(usuario.getId());
        caixa.setValorAbertura(valor);
        Caixa aberto = caixas.abrir(caixa);
        LOG.info("Caixa {} aberto por {} com valor {}", aberto.getId(), usuario.getLogin(), valor);
        auditoria.registrar("CAIXA_ABERTO", "CAIXA", aberto.getId(),
                "valor abertura " + valor.toPlainString());
        return aberto;
    }

    public Optional<Caixa> caixaAberto() {
        return caixas.buscarAberto();
    }

    public CaixaLancamento lancar(TipoLancamentoCaixa tipo, BigDecimal valor, String observacao, Usuario usuario) {
        return lancar(tipo, valor, observacao, usuario, null);
    }

    public CaixaLancamento lancar(TipoLancamentoCaixa tipo, BigDecimal valor, String observacao, Usuario usuario,
                                  Autorizacao autorizacao) {
        if (usuario == null) {
            throw new ValidacaoException("É necessário estar autenticado para movimentar o caixa.");
        }
        Caixa aberto = caixas.buscarAberto()
                .orElseThrow(() -> new ValidacaoException(
                        "Abra o caixa antes de registrar sangria ou suprimento."));
        BigDecimal lancamentoValor = valor == null ? BigDecimal.ZERO : valor;
        if (lancamentoValor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidacaoException("O valor do lançamento deve ser maior que zero.");
        }
        if (tipo == TipoLancamentoCaixa.SANGRIA) {
            if (autorizacoes != null) {
                autorizacoes.exigir(usuario, autorizacao, MotivoAutorizacao.SANGRIA);
            }
            BigDecimal saldo = saldoEspecie(aberto);
            if (lancamentoValor.compareTo(saldo) > 0) {
                throw new ValidacaoException("A sangria de " + lancamentoValor.toPlainString()
                        + " excede o saldo em espécie de " + saldo.toPlainString() + ".");
            }
        }
        CaixaLancamento lancamento = new CaixaLancamento();
        lancamento.setCaixaId(aberto.getId());
        lancamento.setTipo(tipo);
        lancamento.setValor(lancamentoValor);
        lancamento.setUsuarioId(usuario.getId());
        lancamento.setObservacao(observacao);
        CaixaLancamento registrado = caixas.registrarLancamento(lancamento);
        LOG.info("Caixa {} {} de {} por {}", aberto.getId(), tipo.getDescricao(),
                lancamentoValor, usuario.getLogin());
        auditoria.registrar("CAIXA_" + tipo.name(), "CAIXA", aberto.getId(),
                tipo.getDescricao() + " " + lancamentoValor.toPlainString());
        return registrado;
    }

    public BigDecimal saldoEspecie(Caixa caixa) {
        BigDecimal saldo = caixa.getValorAbertura()
                .add(vendas.totalEmDinheiroApos(caixa.getDataAbertura()));
        for (CaixaLancamento lancamento : caixas.listarLancamentos(caixa.getId())) {
            saldo = saldo.add(lancamento.getEfeitoNoSaldo());
        }
        return saldo;
    }

    public List<CaixaLancamento> lancamentos(Caixa caixa) {
        return caixas.listarLancamentos(caixa.getId());
    }

    public Resumo resumo(Caixa caixa) {
        return resumo(caixa, null);
    }

    public Resumo resumo(Caixa caixa, BigDecimal valorInformado) {
        List<CaixaLancamento> lancamentos = caixas.listarLancamentos(caixa.getId());
        BigDecimal suprimentos = somar(lancamentos, TipoLancamentoCaixa.SUPRIMENTO);
        BigDecimal sangrias = somar(lancamentos, TipoLancamentoCaixa.SANGRIA);
        Map<FormaPagamento, BigDecimal> porForma = vendas.totaisPorFormaApos(caixa.getDataAbertura());
        BigDecimal dinheiro = porForma.getOrDefault(FormaPagamento.DINHEIRO, BigDecimal.ZERO);
        BigDecimal saldoEsperado = caixa.getValorAbertura().add(dinheiro)
                .add(suprimentos).subtract(sangrias);
        BigDecimal informado = valorInformado == null ? saldoEsperado : valorInformado;
        return new Resumo(vendas.quantidadeApos(caixa.getDataAbertura()),
                porForma.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add),
                porForma, caixa.getValorAbertura(), suprimentos, sangrias, saldoEsperado,
                informado, informado.subtract(saldoEsperado), lancamentos);
    }

    public void conciliarCartoes(Caixa caixa, Usuario operador) {
        if (caixa == null || caixa.getId() == null) {
            throw new ValidacaoException("Caixa inválido para conciliação.");
        }
        if (operador == null) {
            throw new ValidacaoException("Operador não informado.");
        }
        auditoria.registrar("CAIXA_CONCILIACAO_TEF", "CAIXA", caixa.getId(), "Conciliação TEF via interface abstrata");
    }

    /**
     * Fecha o caixa aberto, calculando diferença entre valor informado e saldo esperado.
     * 
     * @param valorInformado valor conferido pelo operador
     * @param observacao observação opcional
     * @param usuario usuário que fecha o caixa
     * @return resumo do fechamento
     * @throws ValidacaoException se não houver caixa aberto ou valor inválido
     */
    public Resumo fechar(BigDecimal valorInformado, String observacao, Usuario usuario) {
        if (usuario == null) {
            throw new ValidacaoException("É necessário estar autenticado para fechar o caixa.");
        }
        Caixa aberto = caixas.buscarAberto()
                .orElseThrow(() -> new ValidacaoException("Não há caixa aberto para fechar."));
        Resumo previsto = resumo(aberto);
        BigDecimal informado = valorInformado == null ? previsto.saldoEsperado() : valorInformado;
        if (informado.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidacaoException("O valor informado de fechamento não pode ser negativo.");
        }
        BigDecimal diferenca = informado.subtract(previsto.saldoEsperado());
        Caixa fechado = caixas.fechar(aberto.getId(), informado, informado, diferenca,
                usuario.getId(), observacao);
        LOG.info("Caixa {} fechado por {} com valor conferido {} e diferenca {}",
                aberto.getId(), usuario.getLogin(), informado, diferenca);
        auditoria.registrar("CAIXA_FECHADO", "CAIXA", aberto.getId(),
                "informado " + informado.toPlainString() + " esperado " + previsto.saldoEsperado().toPlainString());
        auditoria.registrar("CAIXA_CONFERIDO", "CAIXA", aberto.getId(),
                "conferido " + informado.toPlainString() + " esperado "
                        + previsto.saldoEsperado().toPlainString() + " diferenca "
                        + diferenca.toPlainString());
        return resumo(fechado, informado);
    }

    private BigDecimal somar(List<CaixaLancamento> lancamentos, TipoLancamentoCaixa tipo) {
        return lancamentos.stream()
                .filter(lancamento -> lancamento.getTipo() == tipo)
                .map(CaixaLancamento::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public record Resumo(int quantidadeVendas, BigDecimal totalVendas,
                         Map<FormaPagamento, BigDecimal> porForma, BigDecimal fundoInicial,
                         BigDecimal suprimentos, BigDecimal sangrias, BigDecimal saldoEsperado,
                         BigDecimal valorInformado, BigDecimal diferenca, List<CaixaLancamento> lancamentos) {
    }

    public Caixa buscarPorId(long id) {
        return caixas.buscarPorId(id)
                .orElseThrow(() -> new ValidacaoException("Caixa não encontrado."));
    }
}
