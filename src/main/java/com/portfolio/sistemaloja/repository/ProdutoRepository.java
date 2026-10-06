package com.portfolio.sistemaloja.repository;

import com.portfolio.sistemaloja.db.ConexaoFonte;
import com.portfolio.sistemaloja.model.Produto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProdutoRepository {

    private static final String SELECT_BASE = """
            SELECT p.id, p.codigo_barras, p.nome, p.categoria_id, c.nome AS categoria_nome,
                   p.preco_custo, p.preco_venda, p.estoque, p.ativo
              FROM produtos p
              LEFT JOIN categorias c ON c.id = p.categoria_id""";

    private final ConexaoFonte fonte;

    public ProdutoRepository(ConexaoFonte fonte) {
        this.fonte = fonte;
    }

    public List<Produto> listar(String termo) {
        String sql = SELECT_BASE
                + " WHERE (p.nome LIKE ? OR p.codigo_barras LIKE ?)"
                + " ORDER BY p.nome";
        List<Produto> produtos = new ArrayList<>();
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            String filtro = "%" + (termo == null ? "" : termo.strip()) + "%";
            preparada.setString(1, filtro);
            preparada.setString(2, filtro);
            try (ResultSet resultado = preparada.executeQuery()) {
                while (resultado.next()) {
                    produtos.add(mapear(resultado));
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao listar produtos", e);
        }
        return produtos;
    }

    public List<Produto> listarAtivos() {
        String sql = SELECT_BASE + " WHERE p.ativo = TRUE ORDER BY p.nome";
        List<Produto> produtos = new ArrayList<>();
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql);
             ResultSet resultado = preparada.executeQuery()) {
            while (resultado.next()) {
                produtos.add(mapear(resultado));
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao listar produtos ativos", e);
        }
        return produtos;
    }

    public Optional<Produto> buscarPorId(long id) {
        String sql = SELECT_BASE + " WHERE p.id = ?";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setLong(1, id);
            try (ResultSet resultado = preparada.executeQuery()) {
                if (resultado.next()) {
                    return Optional.of(mapear(resultado));
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao buscar produto", e);
        }
        return Optional.empty();
    }

    public Optional<Produto> buscarPorCodigoBarras(String codigo) {
        String sql = SELECT_BASE + " WHERE p.codigo_barras = ?";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setString(1, codigo);
            try (ResultSet resultado = preparada.executeQuery()) {
                if (resultado.next()) {
                    return Optional.of(mapear(resultado));
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao buscar produto por código", e);
        }
        return Optional.empty();
    }

    public boolean codigoExiste(String codigo, Long idExcluido) {
        String sql = "SELECT COUNT(*) FROM produtos WHERE codigo_barras = ? AND id <> ?";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setString(1, codigo);
            preparada.setLong(2, idExcluido == null ? -1L : idExcluido);
            try (ResultSet resultado = preparada.executeQuery()) {
                return resultado.next() && resultado.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao verificar código de barras", e);
        }
    }

    public Produto salvar(Produto produto) {
        String sql = produto.getId() == null ? insercao() : atualizacao();
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preencher(preparada, produto);
            if (produto.getId() != null) {
                preparada.setLong(8, produto.getId());
            }
            preparada.executeUpdate();
            if (produto.getId() == null) {
                try (ResultSet chaves = preparada.getGeneratedKeys()) {
                    if (chaves.next()) {
                        produto.setId(chaves.getLong(1));
                    }
                }
            }
            return produto;
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao salvar produto", e);
        }
    }

    public void remover(long id) {
        String sql = "DELETE FROM produtos WHERE id = ?";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setLong(1, id);
            preparada.executeUpdate();
        } catch (SQLException e) {
            throw new RepositorioException("Não foi possível excluir o produto (possível venda vinculada)", e);
        }
    }

    public boolean alterarEstoque(long produtoId, int quantidade, Connection conexao) throws SQLException {
        String sql = "UPDATE produtos SET estoque = estoque + ? WHERE id = ? AND estoque + ? >= 0";
        try (PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setInt(1, quantidade);
            preparada.setLong(2, produtoId);
            preparada.setInt(3, quantidade);
            return preparada.executeUpdate() == 1;
        }
    }

    private String insercao() {
        return "INSERT INTO produtos (codigo_barras, nome, categoria_id, preco_custo, preco_venda, estoque, ativo) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
    }

    private String atualizacao() {
        return "UPDATE produtos SET codigo_barras = ?, nome = ?, categoria_id = ?, preco_custo = ?, "
                + "preco_venda = ?, estoque = ?, ativo = ? WHERE id = ?";
    }

    private void preencher(PreparedStatement preparada, Produto produto) throws SQLException {
        preparada.setString(1, produto.getCodigoBarras());
        preparada.setString(2, produto.getNome());
        if (produto.getCategoriaId() == null) {
            preparada.setObject(3, null);
        } else {
            preparada.setLong(3, produto.getCategoriaId());
        }
        preparada.setBigDecimal(4, produto.getPrecoCusto());
        preparada.setBigDecimal(5, produto.getPrecoVenda());
        preparada.setInt(6, produto.getEstoque());
        preparada.setBoolean(7, produto.isAtivo());
    }

    private Produto mapear(ResultSet resultado) throws SQLException {
        long categoriaId = resultado.getLong("categoria_id");
        boolean semCategoria = resultado.wasNull();
        Produto produto = new Produto(
                resultado.getLong("id"),
                resultado.getString("codigo_barras"),
                resultado.getString("nome"),
                semCategoria ? null : categoriaId,
                resultado.getBigDecimal("preco_custo"),
                resultado.getBigDecimal("preco_venda"),
                resultado.getInt("estoque"),
                resultado.getBoolean("ativo"));
        produto.setCategoriaNome(resultado.getString("categoria_nome"));
        return produto;
    }
}
