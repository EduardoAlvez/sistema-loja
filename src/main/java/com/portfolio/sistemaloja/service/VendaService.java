package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.model.FormaPagamento;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.StatusVenda;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.model.VendaItem;
import com.portfolio.sistemaloja.model.VendaPagamento;
import com.portfolio.sistemaloja.repository.ProdutoRepository;
import com.portfolio.sistemaloja.repository.UsuarioRepository;
import com.portfolio.sistemaloja.repository.VendaRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class VendaService {

    private static final Logger LOG = LogManager.getLogger(VendaService.class);

    private final VendaRepository vendas;
    private final ProdutoRepository produtos;
    private final UsuarioRepository usuarios;
    private final AuditoriaService auditoria;

    public VendaService(VendaRepository vendas, ProdutoRepository produtos, UsuarioRepository usuarios,
                        AuditoriaService auditoria) {
        this.vendas = vendas;
        this.produtos = produtos;
        this.usuarios = usuarios;
        this.auditoria = auditoria;
    }

    public Venda registrar(Venda venda) {
        if (venda.getFormaPagamento() == null) {
            throw new ValidacaoException("Selecione a forma de pagamento.");
        }
        List<VendaItem> itens = mesclar(venda.getItens());
        if (itens.isEmpty()) {
            throw new ValidacaoException("Adicione pelo menos um item à venda.");
        }
        validarDescontos(venda, itens);
        validarLimiteDescontoPorPerfil(venda, itens);
        venda.getItens().clear();
        itens.forEach(venda::adicionarItem);
        validarEstoque(venda);
        finalizarPagamento(venda);
        venda.setStatus(StatusVenda.CONCLUIDA);
        venda.setNumero(vendas.proximoNumero());
        Venda registrada = vendas.registrar(venda);
        LOG.info("Venda registrada: numero {} total {} forma {}",
                registrada.getNumero(), registrada.getTotal(), registrada.getFormaPagamento());
        auditoria.registrar("VENDA_REGISTRADA", "VENDA", registrada.getId(),
                "numero " + registrada.getNumero() + " total " + registrada.getTotal());
        return registrada;
    }

    public void cancelar(long vendaId) {
        vendas.cancelar(vendaId);
        LOG.warn("Venda {} cancelada", vendaId);
        auditoria.registrar("VENDA_CANCELADA", "VENDA", vendaId, null);
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

    private void validarLimiteDescontoPorPerfil(Venda venda, List<VendaItem> itens) {
        if (venda.getUsuarioId() == null) {
            return;
        }
        Usuario usuario = usuarios.buscarPorId(venda.getUsuarioId())
                .orElseThrow(() -> new ValidacaoException("Operador da venda não encontrado."));
        int percentual = usuario.getPerfil().getPercentualMaximoDesconto();
        BigDecimal bruto = itens.stream()
                .map(VendaItem::getValorBruto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal descontoTotal = venda.getDesconto().add(itens.stream()
                .map(VendaItem::getDesconto)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal limite = bruto.multiply(BigDecimal.valueOf(percentual))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        if (descontoTotal.compareTo(limite) > 0) {
            throw new ValidacaoException("O desconto total de " + descontoTotal.toPlainString()
                    + " excede o limite de " + percentual + "% do perfil "
                    + usuario.getPerfil().getDescricao() + ".");
        }
    }

    private void validarEstoque(Venda venda) {
        for (VendaItem item : venda.getItens()) {
            Produto produto = produtos.buscarPorId(item.getProdutoId())
                    .orElseThrow(() -> new ValidacaoException(
                            "Produto " + item.getProdutoNome() + " não existe mais."));
            if (!produto.isAtivo()) {
                throw new ValidacaoException("O produto " + produto.getNome() + " está inativo.");
            }
            if (produto.getEstoque() < item.getQuantidade()) {
                throw new ValidacaoException("Estoque insuficiente para " + produto.getNome()
                        + ". Disponível: " + produto.getEstoque() + ".");
            }
            item.setProdutoNome(produto.getNome());
            item.setPrecoUnitario(produto.getPrecoVenda());
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
