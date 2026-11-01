package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.Sessao;
import com.portfolio.sistemaloja.db.ConexaoFonte;
import com.portfolio.sistemaloja.model.MotivoPerda;
import com.portfolio.sistemaloja.model.PerdaEstoque;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.repository.PerdaEstoqueRepository;
import com.portfolio.sistemaloja.repository.ProdutoRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviço do inventário: registra perdas de estoque com motivo.
 *
 * <p>Módulo opcional: nenhuma venda ou fechamento de caixa depende dele.
 * A autorização usa o usuário da sessão corrente, de modo que a identidade do
 * responsável não possa ser informada pelo chamador.
 */
public class InventarioService {
    public static final int OBSERVACAO_MAX = 255;

    private final PerdaEstoqueRepository perdaRepo;
    private final ProdutoRepository produtoRepo;
    private final AuditoriaService auditoria;
    private final Sessao sessao;
    private final ConexaoFonte fonteConexao;

    public InventarioService(PerdaEstoqueRepository perdaRepo, ProdutoRepository produtoRepo,
                             AuditoriaService auditoria, Sessao sessao, ConexaoFonte fonteConexao) {
        this.perdaRepo = perdaRepo;
        this.produtoRepo = produtoRepo;
        this.auditoria = auditoria;
        this.sessao = sessao;
        this.fonteConexao = fonteConexao;
    }

    public List<PerdaEstoque> listarPerdas(long produtoId) {
        exigirGerenteOuAdmin();
        return perdaRepo.listarPorProduto(produtoId);
    }

    public void registrarPerda(long produtoId, int quantidade, String motivo, String observacao) {
        Usuario usuario = exigirGerenteOuAdmin();
        if (quantidade <= 0) {
            throw new ValidacaoException("Quantidade deve ser maior que zero.");
        }
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new ValidacaoException("Motivo da perda é obrigatório.");
        }
        MotivoPerda motivoPerda = MotivoPerda.porNome(motivo);
        if (motivoPerda == null) {
            throw new ValidacaoException("Motivo inválido. Utilize: " + String.join(", ", nomesValidos()) + ".");
        }
        String observacaoNormalizada = observacao == null ? null : observacao.trim();
        if (motivoPerda.exigeObservacao() && (observacaoNormalizada == null || observacaoNormalizada.isEmpty())) {
            throw new ValidacaoException("Informe a observação para perdas com motivo OUTROS.");
        }
        if (observacaoNormalizada != null && observacaoNormalizada.length() > OBSERVACAO_MAX) {
            throw new ValidacaoException("Observação deve ter no máximo " + OBSERVACAO_MAX + " caracteres.");
        }

        try (Connection conexao = fonteConexao.getConnection()) {
            conexao.setAutoCommit(false);
            try {
                Produto produto = produtoRepo.buscarPorId(produtoId, conexao);
                if (produto == null) {
                    throw new ValidacaoException("Produto não encontrado.");
                }
                if (produto.getEstoque() < quantidade) {
                    throw new ValidacaoException("Estoque insuficiente para registrar a perda.");
                }
                boolean alterou = produtoRepo.alterarEstoque(produtoId, -quantidade, conexao);
                if (!alterou) {
                    throw new ValidacaoException("Não foi possível atualizar o estoque.");
                }
                PerdaEstoque perda = new PerdaEstoque(produtoId, quantidade, motivoPerda.name(),
                        observacaoNormalizada, usuario.getId());
                perdaRepo.inserir(perda, conexao);
                auditoria.registrarComo(usuario.getLogin(), "REGISTRAR_PERDA_ESTOQUE", "PRODUTO", produtoId,
                        "qtd=" + quantidade + ";motivo=" + motivoPerda.name(), conexao);
                conexao.commit();
            } catch (RuntimeException e) {
                rollbackSilencioso(conexao, e);
                throw e;
            } catch (SQLException e) {
                rollbackSilencioso(conexao, e);
                throw new ValidacaoException("Erro ao registrar perda de estoque.", e);
            }
        } catch (SQLException e) {
            throw new ValidacaoException("Erro ao registrar perda de estoque.", e);
        }
    }

    private Usuario exigirGerenteOuAdmin() {
        Usuario usuario = sessao.getAtual();
        if (usuario == null) {
            throw new ValidacaoException("Nenhum usuário autenticado.");
        }
        if (!usuario.isAtivo()) {
            throw new ValidacaoException("Usuário inativo não pode registrar perdas.");
        }
        if (!usuario.isGerenteOuAdmin()) {
            throw new ValidacaoException("Apenas GERENTE/ADMIN podem registrar perdas.");
        }
        return usuario;
    }

    private static String nomesValidos() {
        List<String> nomes = new ArrayList<>();
        for (MotivoPerda motivo : MotivoPerda.values()) {
            nomes.add(motivo.name());
        }
        return String.join(", ", nomes);
    }

    private void rollbackSilencioso(Connection conexao, Exception causa) {
        try {
            conexao.rollback();
        } catch (SQLException rollback) {
            causa.addSuppressed(rollback);
        }
    }
}
