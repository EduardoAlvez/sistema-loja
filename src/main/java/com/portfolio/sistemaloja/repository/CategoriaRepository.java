package com.portfolio.sistemaloja.repository;

import com.portfolio.sistemaloja.db.ConexaoFonte;
import com.portfolio.sistemaloja.model.Categoria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CategoriaRepository {

    private final ConexaoFonte fonte;

    public CategoriaRepository(ConexaoFonte fonte) {
        this.fonte = fonte;
    }

    public List<Categoria> listar() {
        String sql = "SELECT id, nome FROM categorias ORDER BY nome";
        List<Categoria> categorias = new ArrayList<>();
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql);
             ResultSet resultado = preparada.executeQuery()) {
            while (resultado.next()) {
                categorias.add(mapear(resultado));
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao listar categorias", e);
        }
        return categorias;
    }

    public Categoria salvar(Categoria categoria) {
        String sql = categoria.getId() == null
                ? "INSERT INTO categorias (nome) VALUES (?)"
                : "UPDATE categorias SET nome = ? WHERE id = ?";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparada.setString(1, categoria.getNome());
            if (categoria.getId() != null) {
                preparada.setLong(2, categoria.getId());
            }
            preparada.executeUpdate();
            if (categoria.getId() == null) {
                try (ResultSet chaves = preparada.getGeneratedKeys()) {
                    if (chaves.next()) {
                        categoria.setId(chaves.getLong(1));
                    }
                }
            }
            return categoria;
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao salvar categoria", e);
        }
    }

    public Optional<Categoria> buscarPorNome(String nome) {
        String sql = "SELECT id, nome FROM categorias WHERE nome = ?";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setString(1, nome);
            try (ResultSet resultado = preparada.executeQuery()) {
                if (resultado.next()) {
                    return Optional.of(mapear(resultado));
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao buscar categoria", e);
        }
        return Optional.empty();
    }

    private Categoria mapear(ResultSet resultado) throws SQLException {
        return new Categoria(resultado.getLong("id"), resultado.getString("nome"));
    }
}
