package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.Sessao;
import com.portfolio.sistemaloja.db.ConexaoFonte;
import com.portfolio.sistemaloja.model.ContagemInventario;
import com.portfolio.sistemaloja.model.ContagemItem;
import com.portfolio.sistemaloja.model.MotivoPerda;
import com.portfolio.sistemaloja.model.PerdaEstoque;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.StatusContagem;
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
        exigirGerenteOuAdmin();
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
                contagemRepo.travarControle(conexao);
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
                    if (!produto.isAtivo()) {
                        throw new ValidacaoException("Produto inativo não pode ser contado: "
                                + produto.getNome() + ".");
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
                StatusContagem status = contagemRepo.travarCabecalho(contagemId, conexao);
                exigirAberta(status, contagemId);
                ContagemInventario contagem = contagemRepo.buscarPorId(contagemId, conexao);
                Long itemId = contagemRepo.buscarItemId(contagemId, produtoId, conexao);
                if (itemId == null) {
                    throw new ValidacaoException("O produto informado não faz parte desta contagem.");
                }
                contagemRepo.registrarQuantidadeContada(itemId, quantidade, conexao);
                auditoria.registrarComo(usuario.getLogin(), "INFORMAR_CONTAGEM_INVENTARIO", "CONTAGEM_ITEM",
                        itemId, "contagem=" + contagem.getNumero() + ";produto=" + produtoId
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
        ContagemInventario base = null;
        try (Connection conexao = fonteConexao.getConnection()) {
            conexao.setAutoCommit(false);
            try {
                StatusContagem status = contagemRepo.travarCabecalho(contagemId, conexao);
                exigirAberta(status, contagemId);
                ContagemInventario contagem = contagemRepo.buscarPorId(contagemId, conexao);
                base = contagem;
                List<ContagemItem> itens = contagemRepo.listarItensParaAjuste(contagemId, conexao);

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

                if (!contagemRepo.fecharSeAberta(contagemId, observacaoNormalizada, conexao)) {
                    throw new ValidacaoException("A contagem " + contagem.getNumero() + " já foi fechada ou cancelada.");
                }
                auditoria.registrarComo(usuario.getLogin(), "FECHAR_CONTAGEM_INVENTARIO", "CONTAGEM_INVENTARIO",
                        contagemId, "numero=" + contagem.getNumero() + ";itens=" + contados
                                + ";ajustes=" + ajustes, conexao);
                conexao.commit();
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
        return lerCabecalhoAposFechamento(base);
    }

    /**
     * Le o cabecalho ja fechado fora da transacao: se essa leitura final falhar, o
     * fechamento (que ja comitou) nao pode ser reportado como erro — devolve o estado
     * conhecido em memoria.
     */
    private ContagemInventario lerCabecalhoAposFechamento(ContagemInventario base) {
        try (Connection conexao = fonteConexao.getConnection()) {
            ContagemInventario fechada = contagemRepo.buscarPorId(base.getId(), conexao);
            if (fechada != null) {
                return fechada;
            }
        } catch (SQLException | RuntimeException e) {
            // o fechamento ja comitou; a falha aqui e apenas de leitura
        }
        base.setStatus(StatusContagem.FECHADA);
        return base;
    }

    /**
     * Cancela a contagem em aberto sem alterar o estoque.
     *
     * <p>O cancelamento nunca gera ajustes: os itens contados são simplesmente ignorados
     * e a contagem fica em estado CANCELADA com motivo registrado.
     */
    public ContagemInventario cancelarContagem(long contagemId, String motivo) {
        Usuario usuario = exigirGerenteOuAdmin();
        String motivoNormalizado = motivo == null ? null : motivo.trim();
        if (motivoNormalizado == null || motivoNormalizado.isEmpty()) {
            throw new ValidacaoException("Informe o motivo do cancelamento.");
        }
        if (motivoNormalizado.length() > OBSERVACAO_MAX) {
            throw new ValidacaoException("Motivo do cancelamento deve ter no máximo " + OBSERVACAO_MAX + " caracteres.");
        }
        ContagemInventario base = null;
        try (Connection conexao = fonteConexao.getConnection()) {
            conexao.setAutoCommit(false);
            try {
                StatusContagem status = contagemRepo.travarCabecalho(contagemId, conexao);
                exigirAberta(status, contagemId);
                ContagemInventario contagem = contagemRepo.buscarPorId(contagemId, conexao);
                base = contagem;

                if (!contagemRepo.cancelarSeAberta(contagemId, motivoNormalizado, conexao)) {
                    throw new ValidacaoException("A contagem " + contagem.getNumero() + " já foi fechada ou cancelada.");
                }
                auditoria.registrarComo(usuario.getLogin(), "CANCELAR_CONTAGEM_INVENTARIO", "CONTAGEM_INVENTARIO",
                        contagemId, "numero=" + contagem.getNumero() + ";motivo=" + motivoNormalizado, conexao);
                conexao.commit();
            } catch (RuntimeException e) {
                rollbackSilencioso(conexao, e);
                throw e;
            } catch (SQLException e) {
                rollbackSilencioso(conexao, e);
                throw new ValidacaoException("Erro ao cancelar a contagem de inventário.", e);
            }
        } catch (SQLException e) {
            throw new ValidacaoException("Erro ao cancelar a contagem de inventário.", e);
        }
        try (Connection leitura = fonteConexao.getConnection()) {
            ContagemInventario cancelada = contagemRepo.buscarPorId(base.getId(), leitura);
            if (cancelada != null) {
                return cancelada;
            }
        } catch (SQLException | RuntimeException e) {
            // o cancelamento ja comitou; a falha aqui e apenas de leitura
        }
        base.setStatus(StatusContagem.CANCELADA);
        base.setMotivoCancelamento(motivoNormalizado);
        return base;
    }

    /**
     * Guarda do status lido **sob a trava de linha**: recusa qualquer coisa que não seja
     * ABERTA sem depender de leitura anterior à trava.
     */
    private void exigirAberta(StatusContagem status, long contagemId) {
        if (status == null) {
            throw new ValidacaoException("Contagem de inventário não encontrada.");
        }
        if (status != StatusContagem.ABERTA) {
            throw new ValidacaoException("A contagem informada já está "
                    + status.getDescricao().toLowerCase(java.util.Locale.ROOT) + ".");
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
