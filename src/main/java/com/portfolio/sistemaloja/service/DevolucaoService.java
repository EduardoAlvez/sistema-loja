package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.model.Devolucao;
import com.portfolio.sistemaloja.model.DevolucaoItem;
import com.portfolio.sistemaloja.model.StatusVenda;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.model.VendaItem;
import com.portfolio.sistemaloja.repository.DevolucaoRepository;
import com.portfolio.sistemaloja.repository.VendaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class DevolucaoService {

    private final DevolucaoRepository devolucoes;
    private final VendaRepository vendas;
    private final AuditoriaService auditoria;

    public DevolucaoService(DevolucaoRepository devolucoes, VendaRepository vendas, AuditoriaService auditoria) {
        this.devolucoes = devolucoes;
        this.vendas = vendas;
        this.auditoria = auditoria;
    }

    public Devolucao devolver(long vendaId, List<DevolucaoItem> itens, boolean creditoNoCliente,
                              String observacao, Usuario usuario) {
        Venda venda = vendas.buscarPorId(vendaId)
                .orElseThrow(() -> new ValidacaoException("Venda não encontrada."));
        if (venda.getStatus() != StatusVenda.CONCLUIDA) {
            throw new ValidacaoException("Somente vendas concluídas podem ser devolvidas.");
        }
        if (itens == null || itens.isEmpty()) {
            throw new ValidacaoException("Selecione ao menos um item para devolver.");
        }
        if (creditoNoCliente && venda.getClienteId() == null) {
            throw new ValidacaoException("Venda sem cliente identificado: informe o cliente para gerar crédito.");
        }

        Devolucao devolucao = new Devolucao();
        devolucao.setVendaId(venda.getId());
        devolucao.setClienteId(venda.getClienteId());
        devolucao.setUsuarioId(usuario == null ? venda.getUsuarioId() : usuario.getId());
        devolucao.setCreditoCliente(creditoNoCliente);
        devolucao.setObservacao(observacao);

        for (DevolucaoItem item : itens) {
            VendaItem vendido = localizarItem(venda, item.getVendaItemId());
            if (item.getQuantidade() <= 0) {
                throw new ValidacaoException("Quantidade inválida para o item " + vendido.getProdutoNome() + ".");
            }
            int disponivel = vendido.getQuantidade() - devolucoes.quantidadeDevolvida(vendido.getId());
            if (item.getQuantidade() > disponivel) {
                throw new ValidacaoException("Item " + vendido.getProdutoNome() + ": restam "
                        + Math.max(disponivel, 0) + " unidade(s) para devolver.");
            }
            item.setProdutoId(vendido.getProdutoId());
            item.setProdutoNome(vendido.getProdutoNome());
            item.setValorUnitario(vendido.getPrecoUnitario());
            devolucao.adicionarItem(item);
        }
        devolucao.recalcularValor();
        if (devolucao.getValor().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidacaoException("O valor da devolução deve ser maior que zero.");
        }

        Devolucao registrada = devolucoes.salvar(devolucao);
        auditoria.registrar("DEVOLUCAO_VENDA", "venda", venda.getId(), "Devolução de "
                + registrada.getQuantidadeItens() + " item(ns) no valor de R$ " + registrada.getValor()
                + (creditoNoCliente ? " creditada no cliente" : " sem crédito"));
        return registrada;
    }

    public int jaDevolvido(long vendaItemId) {
        return devolucoes.quantidadeDevolvida(vendaItemId);
    }

    public List<Devolucao> listarPorVenda(long vendaId) {
        return devolucoes.listarPorVenda(vendaId);
    }

    public Optional<Venda> buscarVendaPorNumero(String numero) {
        if (numero == null || numero.isBlank()) {
            return Optional.empty();
        }
        return vendas.buscarPorNumero(numero.strip().toUpperCase());
    }

    private VendaItem localizarItem(Venda venda, Long vendaItemId) {
        for (VendaItem item : venda.getItens()) {
            if (item.getId() != null && item.getId().equals(vendaItemId)) {
                return item;
            }
        }
        throw new ValidacaoException("Item informado não pertence à venda.");
    }
}
