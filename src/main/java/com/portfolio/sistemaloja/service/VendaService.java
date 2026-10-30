package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.model.Autorizacao;
import com.portfolio.sistemaloja.model.FidelidadeMovimento;
import com.portfolio.sistemaloja.model.FormaPagamento;
import com.portfolio.sistemaloja.model.MotivoAutorizacao;
import com.portfolio.sistemaloja.model.PerfilUsuario;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.StatusVenda;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.model.VendaItem;
import com.portfolio.sistemaloja.model.VendaPagamento;
import com.portfolio.sistemaloja.repository.ProdutoRepository;
import com.portfolio.sistemaloja.repository.RepositorioException;
import com.portfolio.sistemaloja.repository.UsuarioRepository;
import com.portfolio.sistemaloja.repository.VendaRepository;
import com.portfolio.sistemaloja.tef.OperadoraCartaoFactory;
import com.portfolio.sistemaloja.tef.RespostaTransacao;
import com.portfolio.sistemaloja.tef.TransacaoCartao;
import com.portfolio.sistemaloja.tef.OperadoraCartaoFactory;
import com.portfolio.sistemaloja.tef.RespostaTransacao;
import com.portfolio.sistemaloja.tef.TransacaoCartao;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Serviço responsável pelas regras de venda e PDV.
 * <p>
 * Principais regras:
 * <ul>
 *   <li>Exige caixa aberto para registrar qualquer venda.</li>
 *   <li>Valida itens, formas de pagamento e valores.</li>
 *   <li>Para pagamentos com cartão, autoriza via {@link com.portfolio.sistemaloja.tef.OperadoraCartao} (interface abstrata).</li>
 *   <li>Aplica promoções vigentes e regras de autorização/fidelidade quando necessário.</li>
 * </ul>
 */
public class VendaService {

    private static final Logger LOG = LogManager.getLogger(VendaService.class);

    private final VendaRepository vendas;
    private final ProdutoRepository produtos;
    private final UsuarioRepository usuarios;
    private final AuditoriaService auditoria;
    private final FidelidadeService fidelidade;
    private final AutorizacaoService autorizacoes;
    private final PromocaoService promocoes;
    private final CaixaService caixas;
    private final Clock relogio;

    public VendaService(VendaRepository vendas, ProdutoRepository produtos, UsuarioRepository usuarios,
                        AuditoriaService auditoria) {
        this(vendas, produtos, usuarios, auditoria, Clock.systemDefaultZone());
    }

    public VendaService(VendaRepository vendas, ProdutoRepository produtos, UsuarioRepository usuarios,
                        AuditoriaService auditoria, Clock relogio) {
        this(vendas, produtos, usuarios, auditoria, relogio, null, null);
    }

    public VendaService(VendaRepository vendas, ProdutoRepository produtos, UsuarioRepository usuarios,
                        AuditoriaService auditoria, Clock relogio, FidelidadeService fidelidade) {
        this(vendas, produtos, usuarios, auditoria, relogio, fidelidade, null);
    }

    public VendaService(VendaRepository vendas, ProdutoRepository produtos, UsuarioRepository usuarios,
                        AuditoriaService auditoria, Clock relogio, FidelidadeService fidelidade,
                        AutorizacaoService autorizacoes) {
        this(vendas, produtos, usuarios, auditoria, relogio, fidelidade, autorizacoes, null);
    }

    public VendaService(VendaRepository vendas, ProdutoRepository produtos, UsuarioRepository usuarios,
                        AuditoriaService auditoria, Clock relogio, FidelidadeService fidelidade,
                        AutorizacaoService autorizacoes, PromocaoService promocoes) {
        this(vendas, produtos, usuarios, auditoria, relogio, fidelidade, autorizacoes, promocoes, null);
    }

    public VendaService(VendaRepository vendas, ProdutoRepository produtos, UsuarioRepository usuarios,
                        AuditoriaService auditoria, Clock relogio, FidelidadeService fidelidade,
                        AutorizacaoService autorizacoes, PromocaoService promocoes, CaixaService caixas) {
        this.vendas = vendas;
        this.produtos = produtos;
        this.usuarios = usuarios;
        this.auditoria = auditoria;
        this.relogio = relogio;
        this.fidelidade = fidelidade;
        this.autorizacoes = autorizacoes;
        this.promocoes = promocoes;
        this.caixas = caixas;
    }

    /**
     * Registra uma venda no PDV.
     */
    public Venda registrar(Venda venda) {
        return registrar(venda, null);
    }

    /**
     * Registra uma venda no PDV.
     * <p>
     * Regras aplicadas:
     * <ul>
     *   <li>Exige caixa aberto.</li>
     *   <li>Valida itens e formas de pagamento.</li>
     *   <li>Para cartão (crédito/débito), autoriza via operadora TEF.</li>
     *   <li>Aplica promoções, autorização e fidelidade conforme regras.</li>
     * </ul>
     * 
     * @param venda dados da venda a ser registrada
     * @param autorizacao autorização opcional para descontos/supervisão
     * @return venda persistida
     * @throws ValidacaoException caso exista violação de regra de negócio
     */
    public Venda registrar(Venda venda, Autorizacao autorizacao) {
        if (venda == null) {
            throw new ValidacaoException("Venda inválida.");
        }
        if (caixas != null) {
            try {
                java.lang.reflect.Method m = caixas.getClass().getMethod("buscarAberto");
                Object r = m.invoke(caixas);
                if (r instanceof java.util.Optional<?> opt && opt.isEmpty()) {
                    throw new ValidacaoException("Abra o caixa antes de registrar uma venda.");
                }
            } catch (ValidacaoException ve) {
                throw ve;
            } catch (NoSuchMethodException ex) {
                try {
                    java.lang.reflect.Method m2 = caixas.getClass().getMethod("obterAberto");
                    Object r2 = m2.invoke(caixas);
                    if (r2 == null) {
                        throw new ValidacaoException("Abra o caixa antes de registrar uma venda.");
                    }
                } catch (java.lang.reflect.InvocationTargetException ite) {
                    Throwable t = ite.getTargetException();
                    if (t instanceof ValidacaoException ve) {
                        throw ve;
                    }
                    throw new ValidacaoException("Abra o caixa antes de registrar uma venda.");
                } catch (Throwable ex2) {
                }
            } catch (java.lang.reflect.InvocationTargetException ite) {
                Throwable t = ite.getTargetException();
                if (t instanceof ValidacaoException ve) {
                    throw ve;
                }
                throw new ValidacaoException("Abra o caixa antes de registrar uma venda.");
            } catch (Throwable ignored) {
            }
        }
        if (venda.getFormaPagamento() == null) {
            throw new ValidacaoException("Selecione a forma de pagamento.");
        }
        List<VendaItem> itens = mesclar(venda.getItens());
        if (itens.isEmpty()) {
            throw new ValidacaoException("Adicione pelo menos um item à venda.");
        }
        Map<Long, Produto> catalogo = carregarProdutos(itens);
        aplicarPrecoVigente(itens, catalogo, LocalDateTime.now(relogio));
        aplicarResgatePontos(venda, itens);
        validarDescontos(venda, itens);
        validarLimiteDescontoPorPerfil(venda, itens, autorizacao);
        venda.getItens().clear();
        itens.forEach(venda::adicionarItem);
        validarEstoque(venda, catalogo);
        finalizarPagamento(venda);
        venda.setStatus(StatusVenda.CONCLUIDA);
        venda.setNumero(vendas.proximoNumero());
        List<FidelidadeMovimento> movimentos = movimentosDaVenda(venda);
        Venda registrada = registrarComMovimentos(venda, movimentos);
        LOG.info("Venda registrada: numero {} total {} forma {}",
                registrada.getNumero(), registrada.getTotal(), registrada.getFormaPagamento());
        auditoria.registrar("VENDA_REGISTRADA", "VENDA", registrada.getId(),
                "numero " + registrada.getNumero() + " total " + registrada.getTotal());
        if (registrada.getPontosResgatados() > 0) {
            auditoria.registrar("PONTOS_RESGATADOS", "CLIENTE", registrada.getClienteId(),
                    "Resgate de " + registrada.getPontosResgatados() + " ponto(s) na venda "
                            + registrada.getNumero());
        }
        if (registrada.getPontosCreditados() > 0) {
            auditoria.registrar("PONTOS_CREDITADOS", "CLIENTE", registrada.getClienteId(),
                    registrada.getPontosCreditados() + " ponto(s) pela venda " + registrada.getNumero());
        }
        return registrada;
    }

    public void cancelar(long vendaId) {
        cancelar(vendaId, null);
    }

    public void cancelar(long vendaId, Autorizacao autorizacao) {
        Venda venda = vendas.buscarPorId(vendaId)
                .orElseThrow(() -> new ValidacaoException("Venda não encontrada."));
        if (venda.getStatus() == StatusVenda.CANCELADA) {
            return;
        }
        if (autorizacoes != null) {
            autorizacoes.exigir(autorizacao, MotivoAutorizacao.CANCELAMENTO_VENDA);
        }
        int pontosEstornados = vendas.cancelar(vendaId);
        LOG.warn("Venda {} cancelada", vendaId);
        auditoria.registrar("VENDA_CANCELADA", "VENDA", vendaId, null);
        if (venda.getClienteId() != null && pontosEstornados > 0) {
            auditoria.registrar("PONTOS_ESTORNADOS", "CLIENTE", venda.getClienteId(),
                    pontosEstornados + " ponto(s) estornados no cancelamento da venda "
                            + venda.getNumero());
        }
        if (venda.getClienteId() != null && venda.getPontosResgatados() > 0) {
            auditoria.registrar("PONTOS_ESTORNADOS", "CLIENTE", venda.getClienteId(),
                    venda.getPontosResgatados() + " ponto(s) devolvidos ao cliente no cancelamento da venda "
                            + venda.getNumero());
        }
    }

    public List<Venda> listar(java.time.LocalDate inicio, java.time.LocalDate fim) {
        return vendas.listar(inicio, fim);
    }

    public Venda buscarPorId(long id) {
        return vendas.buscarPorId(id)
                .orElseThrow(() -> new ValidacaoException("Venda não encontrada."));
    }

    public Optional<Venda> ultimaDoOperador(long usuarioId) {
        return vendas.ultimaDoUsuario(usuarioId);
    }

    private List<VendaItem> mesclar(List<VendaItem> itens) {
        Map<Long, VendaItem> porProduto = new LinkedHashMap<>();
        for (VendaItem item : itens) {
            if (item.getProdutoId() == null || item.getQuantidade() <= 0) {
                throw new ValidacaoException("Quantidade inválida para o item " + item.getProdutoNome() + ".");
            }
            VendaItem existente = porProduto.get(item.getProdutoId());
            if (existente == null) {
                porProduto.put(item.getProdutoId(), item);
            } else {
                existente.setDesconto(existente.getDesconto().add(
                        item.getDesconto() == null ? BigDecimal.ZERO : item.getDesconto()));
                existente.setQuantidade(existente.getQuantidade() + item.getQuantidade());
            }
        }
        return new ArrayList<>(porProduto.values());
    }

    private void validarDescontos(Venda venda, List<VendaItem> itens) {
        BigDecimal desconto = venda.getDesconto() == null ? BigDecimal.ZERO : venda.getDesconto();
        venda.setDesconto(desconto);
        if (desconto.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidacaoException("O desconto não pode ser negativo.");
        }
        for (VendaItem item : itens) {
            BigDecimal descontoItem = item.getDesconto() == null ? BigDecimal.ZERO : item.getDesconto();
            item.setDesconto(descontoItem);
            if (descontoItem.compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidacaoException("O desconto do item " + item.getProdutoNome()
                        + " não pode ser negativo.");
            }
            if (descontoItem.compareTo(item.getValorBruto()) >= 0) {
                throw new ValidacaoException("O desconto do item " + item.getProdutoNome()
                        + " não pode alcançar o subtotal do item.");
            }
        }
        if (desconto.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        BigDecimal somaItens = itens.stream()
                .map(VendaItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (desconto.compareTo(somaItens) >= 0) {
            throw new ValidacaoException("O desconto não pode alcançar o total dos itens.");
        }
    }

    private void validarLimiteDescontoPorPerfil(Venda venda, List<VendaItem> itens,
                                                Autorizacao autorizacao) {
        if (venda.getUsuarioId() == null) {
            return;
        }
        Usuario usuario = usuarios.buscarPorId(venda.getUsuarioId())
                .orElseThrow(() -> new ValidacaoException("Operador da venda não encontrado."));
        int percentual = usuario.getPerfil().getPercentualMaximoDesconto();
        BigDecimal bruto = itens.stream()
                .map(VendaItem::getValorBruto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal descontoTotal = venda.getDesconto()
                .add(venda.getValorPontos())
                .add(itens.stream()
                        .map(VendaItem::getDesconto)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal limite = bruto.multiply(BigDecimal.valueOf(percentual))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        if (descontoTotal.compareTo(limite) > 0) {
            if (usuario.getPerfil() == PerfilUsuario.ADMIN || autorizacoes == null) {
                throw new ValidacaoException("O desconto total de " + descontoTotal.toPlainString()
                        + " excede o limite de " + percentual + "% do perfil "
                        + usuario.getPerfil().getDescricao() + ".");
            }
            autorizacoes.exigir(usuario, autorizacao, MotivoAutorizacao.DESCONTO_ACIMA_DA_ALCADA);
        }
    }

    private void aplicarResgatePontos(Venda venda, List<VendaItem> itens) {
        int pontos = venda.getPontosResgatados();
        if (pontos < 0) {
            throw new ValidacaoException("A quantidade de pontos resgatados não pode ser negativa.");
        }
        if (pontos == 0) {
            venda.setValorPontos(BigDecimal.ZERO);
            return;
        }
        if (fidelidade == null) {
            throw new ValidacaoException("O resgate de pontos não está disponível.");
        }
        if (venda.getClienteId() == null) {
            throw new ValidacaoException("Selecione o cliente para resgatar pontos.");
        }
        int saldo = fidelidade.saldo(venda.getClienteId());
        if (pontos > saldo) {
            throw new ValidacaoException("Saldo de pontos insuficiente. O cliente tem " + saldo + " ponto(s).");
        }
        BigDecimal valor = fidelidade.valorDosPontos(pontos);
        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidacaoException("O resgate de pontos não gera desconto com a configuração atual.");
        }
        BigDecimal desconto = venda.getDesconto() == null ? BigDecimal.ZERO : venda.getDesconto();
        BigDecimal somaItens = itens.stream()
                .map(VendaItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (desconto.add(valor).compareTo(somaItens) >= 0) {
            throw new ValidacaoException("O resgate de pontos não pode zerar o total da venda.");
        }
        venda.setValorPontos(valor);
    }

    private List<FidelidadeMovimento> movimentosDaVenda(Venda venda) {
        List<FidelidadeMovimento> movimentos = new ArrayList<>();
        if (fidelidade == null || venda.getClienteId() == null) {
            return movimentos;
        }
        if (venda.getPontosResgatados() > 0) {
            movimentos.add(fidelidade.debito(venda.getClienteId(), null, venda.getPontosResgatados(),
                    "Resgate de pontos na venda " + venda.getNumero()));
        }
        int credito = fidelidade.calcularCredito(venda.getTotal());
        venda.setPontosCreditados(credito);
        if (credito > 0) {
            movimentos.add(fidelidade.credito(venda.getClienteId(), null, credito,
                    "Venda " + venda.getNumero()));
        }
        return movimentos;
    }

    private Venda registrarComMovimentos(Venda venda, List<FidelidadeMovimento> movimentos) {
        try {
            return vendas.registrar(venda, movimentos);
        } catch (RepositorioException erro) {
            if (saldoInsuficiente(erro)) {
                throw new ValidacaoException("Saldo de pontos insuficiente para o resgate.");
            }
            throw erro;
        }
    }

    private boolean saldoInsuficiente(RepositorioException erro) {
        Throwable causa = erro.getCause();
        while (causa != null) {
            if (causa.getMessage() != null && causa.getMessage().contains("Saldo de pontos insuficiente")) {
                return true;
            }
            causa = causa.getCause();
        }
        return false;
    }

    private Map<Long, Produto> carregarProdutos(List<VendaItem> itens) {
        Map<Long, Produto> catalogo = new LinkedHashMap<>();
        for (VendaItem item : itens) {
            if (catalogo.containsKey(item.getProdutoId())) {
                continue;
            }
            Produto produto = produtos.buscarPorId(item.getProdutoId())
                    .orElseThrow(() -> new ValidacaoException(
                            "Produto " + item.getProdutoNome() + " não existe mais."));
            catalogo.put(produto.getId(), produto);
        }
        return catalogo;
    }

    private void aplicarPrecoVigente(List<VendaItem> itens, Map<Long, Produto> catalogo,
            LocalDateTime momento) {
        for (VendaItem item : itens) {
            Produto produto = catalogo.get(item.getProdutoId());
            item.setProdutoNome(produto.getNome());
            item.setPrecoUnitario(precoUnitario(produto, item.getQuantidade(), momento));
        }
    }

    private BigDecimal precoUnitario(Produto produto, int quantidade, LocalDateTime momento) {
        if (promocoes == null) {
            return produto.precoVigente(momento);
        }
        return promocoes.precoUnitarioVigente(produto, quantidade, momento);
    }

    private void validarEstoque(Venda venda, Map<Long, Produto> catalogo) {
        for (VendaItem item : venda.getItens()) {
            Produto produto = catalogo.get(item.getProdutoId());
            if (!produto.isAtivo()) {
                throw new ValidacaoException("O produto " + produto.getNome() + " está inativo.");
            }
            if (produto.getEstoque() < item.getQuantidade()) {
                throw new ValidacaoException("Estoque insuficiente para " + produto.getNome()
                        + ". Disponível: " + produto.getEstoque() + ".");
            }
        }
        venda.recalcularTotal();
    }

    private void finalizarPagamento(Venda venda) {
        BigDecimal total = venda.getTotal();
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidacaoException("O total da venda deve ser maior que zero.");
        }
        if (!venda.getPagamentos().isEmpty()) {
            finalizarPagamentoDividido(venda, total);
            return;
        }
        BigDecimal pago = venda.getValorPago();
        if (pago == null || pago.compareTo(BigDecimal.ZERO) == 0) {
            pago = total;
        }
        if (venda.getFormaPagamento() == FormaPagamento.DINHEIRO) {
            if (pago.compareTo(total) < 0) {
                throw new ValidacaoException("Valor pago é menor que o total da venda.");
            }
            venda.setValorPago(pago);
            venda.setTroco(pago.subtract(total));
        } else {
            venda.setValorPago(total);
            venda.setTroco(BigDecimal.ZERO);
        }
    }

    private void finalizarPagamentoDividido(Venda venda, BigDecimal total) {
        BigDecimal soma = BigDecimal.ZERO;
        BigDecimal emDinheiro = BigDecimal.ZERO;
        VendaPagamento principal = venda.getPagamentos().get(0);
        for (VendaPagamento pagamento : venda.getPagamentos()) {
            if (pagamento.getFormaPagamento() == null) {
                throw new ValidacaoException("Informe a forma de pagamento de cada parcela.");
            }
            if (pagamento.getValor() == null || pagamento.getValor().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidacaoException("Cada parcela de pagamento deve ter valor maior que zero.");
            }
            soma = soma.add(pagamento.getValor());
            if (pagamento.getFormaPagamento() == FormaPagamento.DINHEIRO) {
                emDinheiro = emDinheiro.add(pagamento.getValor());
            }
            if (pagamento.getValor().compareTo(principal.getValor()) > 0) {
                principal = pagamento;
            }
        }
        if (soma.compareTo(total) < 0) {
            throw new ValidacaoException("A soma dos pagamentos (" + soma.toPlainString()
                    + ") é menor que o total da venda (" + total.toPlainString() + ").");
        }
        BigDecimal excedente = soma.subtract(total);
        if (excedente.compareTo(BigDecimal.ZERO) > 0 && emDinheiro.compareTo(excedente) < 0) {
            throw new ValidacaoException("O valor excedente de " + excedente.toPlainString()
                    + " só é permitido quando estiver coberto por pagamento em dinheiro.");
        }
        venda.setFormaPagamento(principal.getFormaPagamento());
        venda.setValorPago(soma);
        venda.setTroco(excedente);
    }
}
