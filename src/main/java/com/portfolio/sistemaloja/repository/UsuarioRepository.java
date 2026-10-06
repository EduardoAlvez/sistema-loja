package com.portfolio.sistemaloja.repository;

import com.portfolio.sistemaloja.db.ConexaoFonte;
import com.portfolio.sistemaloja.model.PerfilUsuario;
import com.portfolio.sistemaloja.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UsuarioRepository {

    private static final String SELECT_BASE = """
            SELECT id, nome, login, senha_hash, sal, perfil, ativo FROM usuarios""";

    private final ConexaoFonte fonte;

    public UsuarioRepository(ConexaoFonte fonte) {
        this.fonte = fonte;
    }

    public Optional<Usuario> buscarPorLogin(String login) {
        String sql = SELECT_BASE + " WHERE login = ?";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setString(1, login);
            try (ResultSet resultado = preparada.executeQuery()) {
                if (resultado.next()) {
                    return Optional.of(mapear(resultado));
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao buscar usuário", e);
        }
        return Optional.empty();
    }

    public List<Usuario> listar() {
        String sql = SELECT_BASE + " ORDER BY nome";
        List<Usuario> usuarios = new ArrayList<>();
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql);
             ResultSet resultado = preparada.executeQuery()) {
            while (resultado.next()) {
                usuarios.add(mapear(resultado));
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao listar usuários", e);
        }
        return usuarios;
    }

    public boolean loginExiste(String login, Long idExcluido) {
        String sql = "SELECT COUNT(*) FROM usuarios WHERE login = ? AND id <> ?";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setString(1, login);
            preparada.setLong(2, idExcluido == null ? -1L : idExcluido);
            try (ResultSet resultado = preparada.executeQuery()) {
                return resultado.next() && resultado.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao verificar login", e);
        }
    }

    public Usuario salvar(Usuario usuario) {
        String sql;
        if (usuario.getId() == null) {
            sql = "INSERT INTO usuarios (nome, login, senha_hash, sal, perfil, ativo) VALUES (?, ?, ?, ?, ?, ?)";
        } else if (usuario.getSenhaHash() != null) {
            sql = "UPDATE usuarios SET nome = ?, login = ?, senha_hash = ?, sal = ?, perfil = ?, ativo = ? WHERE id = ?";
        } else {
            sql = "UPDATE usuarios SET nome = ?, login = ?, perfil = ?, ativo = ? WHERE id = ?";
        }
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (usuario.getId() == null || usuario.getSenhaHash() != null) {
                preparada.setString(1, usuario.getNome());
                preparada.setString(2, usuario.getLogin());
                preparada.setString(3, usuario.getSenhaHash());
                preparada.setString(4, usuario.getSal());
                preparada.setString(5, usuario.getPerfil().name());
                preparada.setBoolean(6, usuario.isAtivo());
                if (usuario.getId() != null) {
                    preparada.setLong(7, usuario.getId());
                }
            } else {
                preparada.setString(1, usuario.getNome());
                preparada.setString(2, usuario.getLogin());
                preparada.setString(3, usuario.getPerfil().name());
                preparada.setBoolean(4, usuario.isAtivo());
                preparada.setLong(5, usuario.getId());
            }
            preparada.executeUpdate();
            if (usuario.getId() == null) {
                try (ResultSet chaves = preparada.getGeneratedKeys()) {
                    if (chaves.next()) {
                        usuario.setId(chaves.getLong(1));
                    }
                }
            }
            return usuario;
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao salvar usuário", e);
        }
    }

    private Usuario mapear(ResultSet resultado) throws SQLException {
        Usuario usuario = new Usuario(
                resultado.getLong("id"),
                resultado.getString("nome"),
                resultado.getString("login"),
                PerfilUsuario.valueOf(resultado.getString("perfil")),
                resultado.getBoolean("ativo"));
        usuario.setSenhaHash(resultado.getString("senha_hash"));
        usuario.setSal(resultado.getString("sal"));
        return usuario;
    }
}
