package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.Sessao;
import com.portfolio.sistemaloja.db.ConexaoFonte;
import com.portfolio.sistemaloja.model.ContagemInventario;
import com.portfolio.sistemaloja.model.ContagemItem;
import com.portfolio.sistemaloja.model.MotivoPerda;
import com.portfolio.sistemaloja.model.PerdaEstoque;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.repository.ContagemInventarioRepository;
import com.portfolio.sistemaloja.repository.PerdaEstoqueRepository;
import com.portfolio.sistemaloja.repository.ProdutoRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Serviço do inventário: registra perdas de estoque com motivo e conduz as contagens
 * cíclicas.
 *
 * <p>Módulo opcional: nenhuma venda ou fechamento de caixa depende dele.
 * A autorização usa o usuário da sessão corrente, de modo que a identidade do
 * responsável não possa ser informada pelo chamador.
 *
 * <p>A contagem separa dois momentos de propósito. Abrir a contagem congela o estoque
 * do sistema como referência do operador; informar a contagem apenas anota o que foi
 * visto na prateleira. O estoque só muda quando a contagem é fechada, em uma transação
 * única que lê o estoque atual com trava de linha, aplica os ajustes e grava a auditoria.
 * Digitação errada, portanto, nunca corrompe o estoque.
 */
public class InventarioService {
    public static final int OBSERVACAO_MAX = 255;

    private final PerdaEstoqueRepository perdaRepo;
    private final ContagemInventarioRepository contagemRepo;
    private final ProdutoRepository produtoRepo;
    private final AuditoriaService auditoria;
    private final Sessao sessao;
    private final ConexaoFonte fonteConexao;

    public InventarioService(PerdaEstoqueRepository perdaRepo, ContagemInventarioRepository contagemRepo,
                             ProdutoRepository produtoRepo, AuditoriaService auditoria, Sessao sessao,
                             ConexaoFonte fonteConexao) {
        this.perdaRepo = perdaRepo;
        this.contagemRepo = contagemRepo;
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

    /**
     * Abre uma contagem sobre todos os produtos ativos.
     */
    public long abrirContagem() {
        List<Long> ids = new ArrayList<>();
        for (Produto produto : produtoRepo.listarAtivos()) {
            ids.add(produto.getId());
        }
        return abrirContagem(ids);
    }

    /**
     * Abre uma contagem sobre o lote de produtos informado, congelando o estoque do
     * sistema como referência do operador.
     */
    public long abrirContagem(List<Long> produtoIds) {
        Usuario usuario = exigirGerenteOuAdmin();
        if (produtoIds == null || produtoIds.isEmpty()) {
            throw new ValidacaoException("Selecione ao menos um produto para contar.");
        }
        List<Long> ids = new ArrayList<>(new LinkedHashSet<>(produtoIds));

        try (Connection conexao = fonteConexao.getConnection()) {
            conexao.setAutoCommit(false);
            try {
                ContagemInventario aberta = contagemRepo.buscarAberta(conexao);
                if (aberta != null) {
                    throw new ValidacaoException("Já existe uma contagem em aberto (" + aberta.getNumero()
                            + "). Feche-a antes de iniciar outra.");
                }
                long contagemId = contagemRepo.inserirCabecalho(usuario.getId(), conexao);
                for (Long produtoId : ids) {
                    Produto produto = produtoRepo.buscarPorId(produtoId, conexao);
                    if (produto == null) {
                        throw new ValidacaoException("Produto não encontrado.");
                    }
                    contagemRepo.inserirItem(contagemId, produto.getId(), produto.getEstoque(), conexao);
                }
                auditoria.registrarComo(usuario.getLogin(), "ABRIR_CONTAGEM_INVENTARIO", "CONTAGEM_INVENTARIO",
                        contagemId, "itens=" + ids.size(), conexao);
                conexao.commit();
                return contagemId;
            } catch (RuntimeException e) {
                rollbackSilencioso(conexao, e);
                throw e;
            } catch (SQLException e) {
                rollbackSilencioso(conexao, e);
                throw new ValidacaoException("Erro ao abrir a contagem de inventário.", e);
            }
        } catch (SQLException e) {
            throw new ValidacaoException("Erro ao abrir a contagem de inventário.", e);
        }
    }

    public ContagemInventario buscarContagemAberta() {
        exigirGerenteOuAdmin();
        try (Connection conexao = fonteConexao.getConnection()) {
            return contagemRepo.buscarAberta(conexao);
        } catch (SQLException e) {
            throw new ValidacaoException("Erro ao buscar a contagem em aberto.", e);
        }
    }

    public List<ContagemInventario> listarContagens() {
        exigirGerenteOuAdmin();
        return contagemRepo.listarTodas();
    }

    public List<ContagemItem> listarItens(long contagemId) {
        exigirGerenteOuAdmin();
        try (Connection conexao = fonteConexao.getConnection()) {
            return contagemRepo.listarItens(contagemId, conexao);
        } catch (SQLException e) {
            throw new ValidacaoException("Erro ao listar os itens da contagem.", e);
        }
    }

    /**
     * Anota a quantidade contada de um item. Não altera o estoque: o ajuste acontece
     * somente no fechamento.
     */
    public void informarContagem(long contagemId, long produtoId, int quantidade) {
        Usuario usuario = exigirGerenteOuAdmin();
        if (quantidade < 0) {
            throw new ValidacaoException("A quantidade contada não pode ser negativa.");
        }
        try (Connection conexao = fonteConexao.getConnection()) {
            conexao.setAutoCommit(false);
            try {
                ContagemInventario contagem = contagemRepo.buscarPorId(contagemId, conexao);
                exigirAberta(contagem);
                ContagemItem item = itemDaContagem(contagemId, produtoId, conexao);
                contagemRepo.registrarQuantidadeContada(item.getId(), quantidade, conexao);
                auditoria.registrarComo(usuario.getLogin(), "INFORMAR_CONTAGEM_INVENTARIO", "CONTAGEM_ITEM",
                        item.getId(), "contagem=" + contagem.getNumero() + ";produto=" + produtoId
                                + ";contada=" + quantidade, conexao);
                conexao.commit();
            } catch (RuntimeException e) {
                rollbackSilencioso(conexao, e);
                throw e;
            } catch (SQLException e) {
                rollbackSilencioso(conexao, e);
                throw new ValidacaoException("Erro ao informar a contagem.", e);
            }
        } catch (SQLException e) {
            throw new ValidacaoException("Erro ao informar a contagem.", e);
        }
    }

    /**
     * Fecha a contagem aplicando os ajustes de estoque dos itens contados.
     *
     * <p>A quantidade contada passa a ser o novo estoque do produto. O ajuste é calculado
     * sobre o estoque lido com trava de linha na mesma transação, de modo que uma venda
     * concorrente não possa desfazer o fechamento.
     */
    public ContagemInventario fecharContagem(long contagemId, String observacao) {
        Usuario usuario = exigirGerenteOuAdmin();
        String observacaoNormalizada = observacao == null ? null : observacao.trim();
        if (observacaoNormalizada != null && observacaoNormalizada.length() > OBSERVACAO_MAX) {
            throw new ValidacaoException("Observação deve ter no máximo " + OBSERVACAO_MAX + " caracteres.");
        }
        try (Connection conexao = fonteConexao.getConnection()) {
            conexao.setAutoCommit(false);
            try {
                ContagemInventario contagem = contagemRepo.buscarPorId(contagemId, conexao);
                exigirAberta(contagem);
                List<ContagemItem> itens = contagemRepo.listarItens(contagemId, conexao);

                int contados = 0;
                for (ContagemItem item : itens) {
                    if (item.foiContado()) {
                        contados++;
                    }
                }
                if (contados == 0) {
                    throw new ValidacaoException("Informe ao menos uma contagem antes de fechar.");
                }

                int ajustes = 0;
                for (ContagemItem item : itens) {
                    if (!item.foiContado()) {
                        continue;
                    }
                    Integer estoque = produtoRepo.estoqueParaAjuste(item.getProdutoId(), conexao);
                    if (estoque == null) {
                        throw new ValidacaoException("Produto não encontrado: " + item.getProdutoNome() + ".");
                    }
                    int deltaAplicado = item.getQuantidadeContada() - estoque;
                    if (deltaAplicado != 0) {
                        if (!produtoRepo.alterarEstoque(item.getProdutoId(), deltaAplicado, conexao)) {
                            throw new ValidacaoException("Não foi possível ajustar o estoque de "
                                    + item.getProdutoNome() + ".");
                        }
                        ajustes++;
                        auditoria.registrarComo(usuario.getLogin(), "AJUSTAR_ESTOQUE_CONTAGEM", "PRODUTO",
                                item.getProdutoId(), "contagem=" + contagem.getNumero() + ";antes=" + estoque
                                        + ";contada=" + item.getQuantidadeContada() + ";delta=" + deltaAplicado,
                                conexao);
                    }
                    contagemRepo.registrarAjuste(item.getId(), deltaAplicado, conexao);
                }

                contagemRepo.fechar(contagemId, observacaoNormalizada, conexao);
                auditoria.registrarComo(usuario.getLogin(), "FECHAR_CONTAGEM_INVENTARIO", "CONTAGEM_INVENTARIO",
                        contagemId, "numero=" + contagem.getNumero() + ";itens=" + contados
                                + ";ajustes=" + ajustes, conexao);
                conexao.commit();

                ContagemInventario fechada = contagemRepo.buscarPorId(contagemId, conexao);
                return fechada == null ? contagem : fechada;
            } catch (RuntimeException e) {
                rollbackSilencioso(conexao, e);
                throw e;
            } catch (SQLException e) {
                rollbackSilencioso(conexao, e);
                throw new ValidacaoException("Erro ao fechar a contagem de inventário.", e);
            }
        } catch (SQLException e) {
            throw new ValidacaoException("Erro ao fechar a contagem de inventário.", e);
        }
    }

    private ContagemItem itemDaContagem(long contagemId, long produtoId, Connection conexao) throws SQLException {
        for (ContagemItem item : contagemRepo.listarItens(contagemId, conexao)) {
            if (item.getProdutoId() == produtoId) {
                return item;
            }
        }
        throw new ValidacaoException("O produto informado não faz parte desta contagem.");
    }

    private void exigirAberta(ContagemInventario contagem) {
        if (contagem == null) {
            throw new ValidacaoException("Contagem de inventário não encontrada.");
        }
        if (!contagem.estaAberta()) {
            throw new ValidacaoException("A contagem " + contagem.getNumero() + " já está "
                    + contagem.getStatus().getDescricao().toLowerCase() + ".");
        }
    }

    private Usuario exigirGerenteOuAdmin() {
        Usuario usuario = sessao.getAtual();
        if (usuario == null) {
            throw new ValidacaoException("Nenhum usuário autenticado.");
        }
        if (!usuario.isAtivo()) {
            throw new ValidacaoException("Usuário inativo não pode acessar o inventário.");
        }
        if (!usuario.isGerenteOuAdmin()) {
            throw new ValidacaoException("Apenas GERENTE/ADMIN podem acessar o inventário.");
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
