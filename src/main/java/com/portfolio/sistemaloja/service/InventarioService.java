package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.model.ContagemInventario;
import com.portfolio.sistemaloja.model.ContagemItem;
import com.portfolio.sistemaloja.model.PerdidaEstoque;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.repository.ContagemInventarioRepository;
import com.portfolio.sistemaloja.repository.PerdidaEstoqueRepository;
import com.portfolio.sistemaloja.repository.ProdutoRepository;
import com.portfolio.sistemaloja.repository.RepositorioException;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;

public class InventarioService {

    private final ContagemInventarioRepository contagens;
    private final PerdaEstoqueRepository perdas;
    private final ProdutoRepository produtos;
    private final AuditoriaService auditoria;
    private final com.portfolio.sistemaloja.db.ConexaoFonte fonte;

    public InventarioService(ContagemInventarioRepository contagens, PerdaEstoqueRepository perdas,
                             ProdutoRepository produtos, AuditoriaService auditoria,
                             com.portfolio.sistemaloja.db.ConexaoFonte fonte) {
        this.contagens = contagens;
        this.perdas = perdas;
        this.produtos = produtos;
        this.auditoria = auditoria;
        this.fonte = fonte;
    }

    public ContagemInventario iniciarContagem(Usuario usuario) {
        if (usuario == null || (!usuario.isGerenteOuAdmin())) {
            throw new ValidacaoException("Somente gerente ou administrador pode iniciar inventário.");
        }
        if (contagens.buscarAberta().isPresent()) {
            throw new ValidacaoException("Já existe uma contagem aberta.");
        }
        String numero = "INV-" + System.currentTimeMillis();
        ContagemInventario c = contagens.criar(numero, usuario.getId());
        List<Produto> lista = produtos.listarTodos();
        for (Produto p : lista) {
            ContagemItem item = new ContagemItem();
            item.setProdutoId(p.getId());
            item.setEstoqueSistema(p.getEstoque());
            item.setQuantidadeContada(p.getEstoque());
            item.setAjustado(false);
            item.calcularDiferenca();
            contagens.adicionarItem(c.getId(), item);
        }
        auditoria.registrar("INVENTARIO_INICIADO", "INVENTARIO", c.getId(), "Contagem " + c.getNumero());
        return c;
    }

    public void lancarContagem(long itemId, int quantidadeContada, Usuario usuario) {
        if (usuario == null || (!usuario.isGerenteOuAdmin())) {
            throw new ValidacaoException("Somente gerente ou administrador pode lançar contagem.");
        }
        if (quantidadeContada < 0) {
            throw new ValidacaoException("Quantidade contada não pode ser negativa.");
        }
        contagens.lancarContagem(itemId, quantidadeContada);
        auditoria.registrar("INVENTARIO_LANCADO", "INVENTARIO_ITEM", itemId, "Quantidade contada " + quantidadeContada);
    }

    public void fecharContagem(Usuario usuario) {
        if (usuario == null || (!usuario.isGerenteOuAdmin())) {
            throw new ValidacaoException("Somente gerente ou administrador pode fechar inventário.");
        }
        ContagemInventario aberta = contagens.buscarAberta()
                .orElseThrow(() -> new ValidacaoException("Não há contagem aberta."));
        List<ContagemItem> itens = contagens.listarItens(aberta.getId());
        try (Connection conexao = fonte.getConnection()) {
            conexao.setAutoCommit(false);
            try {
                for (ContagemItem item : itens) {
                    if (!item.isAjustado() && item.getDiferenca() != 0) {
                        produtos.alterarEstoque(item.getProdutoId(), item.getDiferenca(), conexao);
                    }
                }
                contagens.fechar(aberta.getId());
                conexao.commit();
            } catch (Exception e) {
                conexao.rollback();
                throw e instanceof ValidacaoException ve ? ve : new RepositorioException("Falha ao fechar contagem", e);
            } finally {
                conexao.setAutoCommit(true);
            }
        } catch (RepositorioException e) {
            throw e;
        } catch (Exception e) {
            throw new RepositorioException("Falha ao fechar contagem", e);
        }
        auditoria.registrar("INVENTARIO_FECHADO", "INVENTARIO", aberta.getId(), "Contagem fechada com ajustes");
    }

    public void registrarPerda(long produtoId, int quantidade, PerdaEstoque.Motivo motivo, String observacao, Usuario usuario) {
        if (usuario == null || (!usuario.isGerenteOuAdmin())) {
            throw new ValidacaoException("Somente gerente ou administrador pode registrar perda.");
        }
        if (quantidade <= 0) {
            throw new ValidacaoException("Quantidade deve ser maior que zero.");
        }
        if (motivo == null) {
            throw new ValidacaoException("Informe o motivo da perda.");
        }
        try (Connection conexao = fonte.getConnection()) {
            conexao.setAutoCommit(false);
            try {
                Produto p = produtos.buscarPorId(produtoId, conexao);
                if (p.getEstoque() < quantidade) {
                    throw new ValidacaoException("Estoque insuficiente para registrar perda.");
                }
                produtos.alterarEstoque(produtoId, -quantidade, conexao);
                PerdaEstoque perda = new PerdaEstoque();
                perda.setProdutoId(produtoId);
                perda.setQuantidade(quantidade);
                perda.setMotivo(motivo);
                perda.setUsuarioId(usuario.getId());
                perda.setObservacao(observacao);
                perda.setDataHora(LocalDateTime.now());
                perdas.registrar(perda, conexao);
                conexao.commit();
            } catch (Exception e) {
                conexao.rollback();
                throw e instanceof ValidacaoException ve ? ve : new RepositorioException("Falha ao registrar perda", e);
            } finally {
                conexao.setAutoCommit(true);
            }
        } catch (RepositorioException e) {
            throw e;
        } catch (Exception e) {
            throw new RepositorioException("Falha ao registrar perda", e);
        }
        auditoria.registrar("PERDA_ESTOQUE", "PRODUTO", produtoId, "Perda " + quantidade + " (" + motivo + ")");
    }
}
