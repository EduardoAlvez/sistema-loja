package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.db.ConexaoFonte;
import com.portfolio.sistemaloja.model.PerdaEstoque;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.repository.PerdaEstoqueRepository;
import com.portfolio.sistemaloja.repository.ProdutoRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InventarioService {
    private static final Set<String> MOTIVOS_VALIDOS = new HashSet<>(
            Arrays.asList("QUEBRA", "VENCIMENTO", "ROUBO", "AVARIA", "OUTROS")
    );

    private final PerdaEstoqueRepository perdaRepo;
    private final ProdutoRepository produtoRepo;
    private final AuditoriaService auditoria;
    private final ConexaoFonte fonteConexao;

    public InventarioService(PerdaEstoqueRepository perdaRepo, ProdutoRepository produtoRepo,
                             AuditoriaService auditoria, ConexaoFonte fonteConexao) {
        this.perdaRepo = perdaRepo;
        this.produtoRepo = produtoRepo;
        this.auditoria = auditoria;
        this.fonteConexao = fonteConexao;
    }

    public List<PerdaEstoque> listarPerdas(long produtoId) {
        return perdaRepo.listarPorProduto(produtoId);
    }

    public void registrarPerda(Usuario usuario, long produtoId, int quantidade, String motivo, String observacao) {
        if (usuario == null) {
            throw new ValidacaoException("Usuário não informado.");
        }
        if (!usuario.isGerenteOuAdmin()) {
            throw new ValidacaoException("Apenas GERENTE/ADMIN podem registrar perdas.");
        }
        if (usuario.getId() == null) {
            throw new ValidacaoException("Usuário sem identificador válido para auditoria.");
        }
        if (quantidade <= 0) {
            throw new ValidacaoException("Quantidade deve ser maior que zero.");
        }
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new ValidacaoException("Motivo da perda é obrigatório.");
        }
        String motivoNormalizado = motivo.trim().toUpperCase();
        if (!MOTIVOS_VALIDOS.contains(motivoNormalizado)) {
            throw new ValidacaoException("Motivo inválido. Utilize: QUEBRA, VENCIMENTO, ROUBO, AVARIA ou OUTROS.");
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
                PerdaEstoque perda = new PerdaEstoque(produtoId, quantidade, motivoNormalizado,
                        observacao == null ? null : observacao.trim(), usuario.getId());
                perdaRepo.inserir(perda, conexao);
                auditoria.registrarComo(usuario.getLogin(), "REGISTRAR_PERDA_ESTOQUE", "PRODUTO", produtoId,
                        "qtd=" + quantidade + ";motivo=" + motivoNormalizado);
                conexao.commit();
            } catch (Exception e) {
                conexao.rollback();
                if (e instanceof ValidacaoException) {
                    throw e;
                }
                throw new ValidacaoException("Erro ao registrar perda de estoque: " + e.getMessage());
            } finally {
                conexao.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new ValidacaoException("Erro ao registrar perda de estoque: " + e.getMessage());
        }
    }
}
