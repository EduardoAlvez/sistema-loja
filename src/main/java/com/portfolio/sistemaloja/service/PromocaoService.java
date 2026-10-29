package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.PromocaoProduto;
import com.portfolio.sistemaloja.repository.ProdutoRepository;
import com.portfolio.sistemaloja.repository.PromocaoProdutoRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Serviço para promoções por produto com vigência por data/horário e aplicação no carrinho.
 * <p>
 * Em caso de múltiplas promoções aplicáveis, prioriza o menor preço efetivo.
 */
public class PromocaoService {

    private final PromocaoProdutoRepository promocoes;
    private final ProdutoRepository produtos;
    private final AuditoriaService auditoria;
    private final Clock relogio;

    public PromocaoService(PromocaoProdutoRepository promocoes, ProdutoRepository produtos,
                           AuditoriaService auditoria, Clock relogio) {
        this.promocoes = Objects.requireNonNull(promocoes, "promocoes");
        this.produtos = Objects.requireNonNull(produtos, "produtos");
        this.auditoria = Objects.requireNonNull(auditoria, "auditoria");
        this.relogio = Objects.requireNonNull(relogio, "relogio");
    }

    public List<PromocaoProduto> listar() {
        return promocoes.listar();
    }

    public List<PromocaoProduto> listarAtivas() {
        return promocoes.listarAtivas();
    }

    public List<PromocaoProduto> listarPorProduto(Long produtoId) {
        return promocoes.listarPorProduto(produtoId);
    }

    public PromocaoProduto buscarPorId(Long id) {
        return promocoes.buscarPorId(id)
                .orElseThrow(() -> new ValidacaoException("Promoção não encontrada: " + id + "."));
    }

    public PromocaoProduto salvar(PromocaoProduto promocao) {
        normalizarPreco(promocao);
        validar(promocao);
        PromocaoProduto salva = promocoes.salvar(promocao);
        auditoria.registrar("PROMOCAO_SALVA", "PROMOCAO", salva.getId(),
                salva.getNome() + " - " + salva.getQuantidadeMinima() + " un. por "
                        + salva.getPrecoPromocional().toPlainString());
        return salva;
    }

    private void normalizarPreco(PromocaoProduto promocao) {
        if (promocao == null || promocao.getPrecoPromocional() == null) {
            return;
        }
        promocao.setPrecoPromocional(promocao.getPrecoPromocional().setScale(2, RoundingMode.HALF_UP));
    }

    public void remover(Long id) {
        promocoes.deletar(id);
        auditoria.registrar("PROMOCAO_REMOVIDA", "PROMOCAO", id, null);
    }

    public BigDecimal precoUnitarioVigente(Produto produto, int quantidade) {
        return precoUnitarioVigente(produto, quantidade, LocalDateTime.now(relogio));
    }

    public BigDecimal precoUnitarioVigente(Produto produto, int quantidade, LocalDateTime momento) {
        if (produto == null) {
            throw new ValidacaoException("Informe o produto para consultar o preço.");
        }
        if (quantidade < 1) {
            throw new ValidacaoException("A quantidade deve ser maior que zero.");
        }
        BigDecimal precoBase = produto.precoVigente(momento);
        BigDecimal precoPromocional = precoPromocionalVigente(produto.getId(), quantidade, momento);
        if (precoPromocional == null) {
            return precoBase;
        }
        return precoPromocional.compareTo(precoBase) < 0 ? precoPromocional : precoBase;
    }

    public BigDecimal precoPromocionalVigente(Long produtoId, int quantidade, LocalDateTime momento) {
        if (produtoId == null || quantidade < 1) {
            return null;
        }
        BigDecimal melhor = null;
        for (PromocaoProduto promocao : promocoes.listarPorProduto(produtoId)) {
            if (!vigente(promocao, quantidade, momento)) {
                continue;
            }
            if (melhor == null || promocao.getPrecoPromocional().compareTo(melhor) < 0) {
                melhor = promocao.getPrecoPromocional();
            }
        }
        return melhor;
    }

    public boolean vigente(PromocaoProduto promocao, int quantidade, LocalDateTime momento) {
        if (!dentroDaProgramacao(promocao, momento)) {
            return false;
        }
        if (promocao.getQuantidadeMinima() == null) {
            return false;
        }
        return quantidade >= promocao.getQuantidadeMinima();
    }

    public boolean dentroDaProgramacao(PromocaoProduto promocao) {
        return dentroDaProgramacao(promocao, LocalDateTime.now(relogio));
    }

    public boolean dentroDaProgramacao(PromocaoProduto promocao, LocalDateTime momento) {
        if (promocao == null || momento == null) {
            return false;
        }
        if (!promocao.isAtiva()) {
            return false;
        }
        LocalDate data = momento.toLocalDate();
        if (promocao.getDataInicio() != null && data.isBefore(promocao.getDataInicio())) {
            return false;
        }
        if (promocao.getDataFim() != null && data.isAfter(promocao.getDataFim())) {
            return false;
        }
        if (!promocao.getDiasSemana().isEmpty()
                && !promocao.getDiasSemana().contains(momento.getDayOfWeek())) {
            return false;
        }
        if (promocao.getHorarioInicio() != null && momento.toLocalTime().isBefore(promocao.getHorarioInicio())) {
            return false;
        }
        if (promocao.getHorarioFim() != null && momento.toLocalTime().isAfter(promocao.getHorarioFim())) {
            return false;
        }
        return true;
    }

    private void validar(PromocaoProduto promocao) {
        if (promocao == null) {
            throw new ValidacaoException("Informe a promoção.");
        }
        if (promocao.getNome() == null || promocao.getNome().isBlank()) {
            throw new ValidacaoException("Nome da promoção é obrigatório.");
        }
        if (promocao.getNome().length() > 100) {
            throw new ValidacaoException("Nome da promoção deve ter no máximo 100 caracteres.");
        }
        if (promocao.getProdutoId() == null) {
            throw new ValidacaoException("Produto é obrigatório.");
        }
        Produto produto = produtos.buscarPorId(promocao.getProdutoId())
                .orElseThrow(() -> new ValidacaoException("Produto não encontrado para a promoção."));
        if (promocao.getQuantidadeMinima() == null || promocao.getQuantidadeMinima() < 1) {
            throw new ValidacaoException("Quantidade mínima deve ser maior que zero.");
        }
        if (promocao.getPrecoPromocional() == null
                || promocao.getPrecoPromocional().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidacaoException("Preço promocional deve ser maior que zero.");
        }
        if (produto.getPrecoVenda() != null
                && promocao.getPrecoPromocional().compareTo(produto.getPrecoVenda()) >= 0) {
            throw new ValidacaoException("Preço promocional deve ser menor que o preço de venda.");
        }
        if (promocao.getDataInicio() != null && promocao.getDataFim() != null
                && promocao.getDataFim().isBefore(promocao.getDataInicio())) {
            throw new ValidacaoException("Fim da promoção não pode ser antes do início.");
        }
        boolean temInicio = promocao.getHorarioInicio() != null;
        boolean temFim = promocao.getHorarioFim() != null;
        if (temInicio != temFim) {
            throw new ValidacaoException("Informe o horário de início e de fim da promoção.");
        }
        if (temInicio && !promocao.getHorarioInicio().isBefore(promocao.getHorarioFim())) {
            throw new ValidacaoException("Horário inicial da promoção deve ser anterior ao final.");
        }
    }
}
