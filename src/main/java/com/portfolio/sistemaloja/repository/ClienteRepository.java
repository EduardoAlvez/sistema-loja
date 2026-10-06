package com.portfolio.sistemaloja.repository;

import com.portfolio.sistemaloja.db.ConexaoFonte;
import com.portfolio.sistemaloja.model.Cliente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClienteRepository {

    private static final String SELECT_BASE = """
            SELECT id, nome, cpf, telefone, email, ativo FROM clientes""";

    private final ConexaoFonte fonte;

    public ClienteRepository(ConexaoFonte fonte) {
        this.fonte = fonte;
    }

    public List<Cliente> listar(String termo) {
        String sql = SELECT_BASE + " WHERE nome LIKE ? OR cpf LIKE ? ORDER BY nome";
        List<Cliente> clientes = new ArrayList<>();
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            String filtro = "%" + (termo == null ? "" : termo.strip()) + "%";
            preparada.setString(1, filtro);
            preparada.setString(2, filtro);
            try (ResultSet resultado = preparada.executeQuery()) {
                while (resultado.next()) {
                    clientes.add(mapear(resultado));
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao listar clientes", e);
        }
        return clientes;
    }

    public List<Cliente> listarAtivos() {
        String sql = SELECT_BASE + " WHERE ativo = TRUE ORDER BY nome";
        List<Cliente> clientes = new ArrayList<>();
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql);
             ResultSet resultado = preparada.executeQuery()) {
            while (resultado.next()) {
                clientes.add(mapear(resultado));
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao listar clientes ativos", e);
        }
        return clientes;
    }

    public Optional<Cliente> buscarPorId(long id) {
        String sql = SELECT_BASE + " WHERE id = ?";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setLong(1, id);
            try (ResultSet resultado = preparada.executeQuery()) {
                if (resultado.next()) {
                    return Optional.of(mapear(resultado));
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao buscar cliente", e);
        }
        return Optional.empty();
    }

    public boolean cpfExiste(String cpf, Long idExcluido) {
        String sql = "SELECT COUNT(*) FROM clientes WHERE cpf = ? AND id <> ?";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setString(1, cpf);
            preparada.setLong(2, idExcluido == null ? -1L : idExcluido);
            try (ResultSet resultado = preparada.executeQuery()) {
                return resultado.next() && resultado.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao verificar CPF", e);
        }
    }

    public Cliente salvar(Cliente cliente) {
        String sql = cliente.getId() == null
                ? "INSERT INTO clientes (nome, cpf, telefone, email, ativo) VALUES (?, ?, ?, ?, ?)"
                : "UPDATE clientes SET nome = ?, cpf = ?, telefone = ?, email = ?, ativo = ? WHERE id = ?";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparada.setString(1, cliente.getNome());
            preparada.setString(2, cliente.getCpf());
            preparada.setString(3, cliente.getTelefone());
            preparada.setString(4, cliente.getEmail());
            preparada.setBoolean(5, cliente.isAtivo());
            if (cliente.getId() != null) {
                preparada.setLong(6, cliente.getId());
            }
            preparada.executeUpdate();
            if (cliente.getId() == null) {
                try (ResultSet chaves = preparada.getGeneratedKeys()) {
                    if (chaves.next()) {
                        cliente.setId(chaves.getLong(1));
                    }
                }
            }
            return cliente;
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao salvar cliente", e);
        }
    }

    public void remover(long id) {
        String sql = "DELETE FROM clientes WHERE id = ?";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setLong(1, id);
            preparada.executeUpdate();
        } catch (SQLException e) {
            throw new RepositorioException("Não foi possível excluir o cliente (possível venda vinculada)", e);
        }
    }

    private Cliente mapear(ResultSet resultado) throws SQLException {
        return new Cliente(
                resultado.getLong("id"),
                resultado.getString("nome"),
                resultado.getString("cpf"),
                resultado.getString("telefone"),
                resultado.getString("email"),
                resultado.getBoolean("ativo"));
    }
}
