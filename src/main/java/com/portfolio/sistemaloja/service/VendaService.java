package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.model.FormaPagamento;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.StatusVenda;
import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.model.VendaItem;
import com.portfolio.sistemaloja.repository.ProdutoRepository;
import com.portfolio.sistemaloja.repository.VendaRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VendaService {

    private static final Logger LOG = LogManager.getLogger(VendaService.class);

    private final VendaRepository vendas;
    private final ProdutoRepository produtos;
    private final AuditoriaService auditoria;

    public VendaService(VendaRepository vendas, ProdutoRepository produtos, AuditoriaService auditoria) {
        this.vendas = vendas;
        this.produtos = produtos;
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
                existente.setQuantidade(existente.getQuantidade() + item.getQuantidade());
            }
        }
        return new ArrayList<>(porProduto.values());
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
}
