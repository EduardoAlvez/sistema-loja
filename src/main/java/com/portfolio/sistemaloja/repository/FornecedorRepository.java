package com.portfolio.sistemaloja.repository;

import com.portfolio.sistemaloja.db.ConexaoFonte;
import com.portfolio.sistemaloja.model.Fornecedor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FornecedorRepository {

    private final ConexaoFonte fonte;

    public FornecedorRepository(ConexaoFonte fonte) {
        this.fonte = fonte;
    }

    public List<Fornecedor> listar(String termo) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, nome, cnpj, telefone, email, endereco, ativo, criado_em
                  FROM fornecedores
                 WHERE 1=1""");
        if (termo != null && !termo.strip().isEmpty()) {
            sql.append(" AND (nome LIKE ? OR cnpj LIKE ? OR telefone LIKE ?)");
        }
        sql.append(" ORDER BY nome");
        List<Fornecedor> lista = new ArrayList<>();
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql.toString())) {
            int idx = 1;
            if (termo != null && !termo.strip().isEmpty()) {
                String filtro = "%" + termo.strip() + "%";
                preparada.setString(idx++, filtro);
                preparada.setString(idx++, filtro);
                preparada.setString(idx++, filtro);
            }
            try (ResultSet resultado = preparada.executeQuery()) {
                while (resultado.next()) {
                    lista.add(mapear(resultado));
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao listar fornecedores", e);
        }
        return lista;
    }

    public List<Fornecedor> listarAtivos() {
        String sql = """
                SELECT id, nome, cnpj, telefone, email, endereco, ativo, criado_em
                  FROM fornecedores
                 WHERE ativo = TRUE
                 ORDER BY nome""";
        List<Fornecedor> lista = new ArrayList<>();
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql);
             ResultSet resultado = preparada.executeQuery()) {
            while (resultado.next()) {
                lista.add(mapear(resultado));
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao listar fornecedores ativos", e);
        }
        return lista;
    }

    public Optional<Fornecedor> buscarPorId(long id) {
        String sql = """
                SELECT id, nome, cnpj, telefone, email, endereco, ativo, criado_em
                  FROM fornecedores
                 WHERE id = ?""";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setLong(1, id);
            try (ResultSet resultado = preparada.executeQuery()) {
                if (resultado.next()) {
                    return Optional.of(mapear(resultado));
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao buscar fornecedor", e);
        }
        return Optional.empty();
    }

    public boolean nomeExiste(String nome, Long idExcluido) {
        String sql = "SELECT COUNT(*) FROM fornecedores WHERE LOWER(nome) = LOWER(?) AND id <> ?";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setString(1, nome == null ? "" : nome.strip());
            preparada.setLong(2, idExcluido == null ? -1L : idExcluido);
            try (ResultSet resultado = preparada.executeQuery()) {
                return resultado.next() && resultado.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao verificar fornecedor", e);
        }
        return false;
    }

    public Fornecedor salvar(Fornecedor fornecedor) {
        String sql = fornecedor.getId() == null ? inserir() : atualizar();
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preencher(preparada, fornecedor);
            if (fornecedor.getId() != null) {
                preparada.setLong(8, fornecedor.getId());
            }
            preparada.executeUpdate();
            if (fornecedor.getId() == null) {
                try (ResultSet chaves = preparada.getGeneratedKeys()) {
                    if (chaves.next()) {
                        fornecedor.setId(chaves.getLong(1));
                    }
                }
            }
            return fornecedor;
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao salvar fornecedor", e);
        }
    }

    public void remover(long id) {
        String sql = "DELETE FROM fornecedores WHERE id = ?";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setLong(1, id);
            preparada.executeUpdate();
        } catch (SQLException e) {
            throw new RepositorioException("Não foi possível excluir o fornecedor (possível entrada vinculada)", e);
        }
    }

    private String inserir() {
        return "INSERT INTO fornecedores (nome, cnpj, telefone, email, endereco, ativo) VALUES (?, ?, ?, ?, ?, ?)";
    }

    private String atualizar() {
        return "UPDATE fornecedores SET nome = ?, cnpj = ?, telefone = ?, email = ?, endereco = ?, ativo = ? WHERE id = ?";
    }

    private void preencher(PreparedStatement preparada, Fornecedor fornecedor) throws SQLException {
        preparada.setString(1, fornecedor.getNome());
        if (fornecedor.getCnpj() == null || fornecedor.getCnpj().isBlank()) {
            preparada.setNull(2, Types.VARCHAR);
        } else {
            preparada.setString(2, fornecedor.getCnpj().strip());
        }
        if (fornecedor.getTelefone() == null || fornecedor.getTelefone().isBlank()) {
            preparada.setNull(3, Types.VARCHAR);
        } else {
            preparada.setString(3, fornecedor.getTelefone().strip());
        }
        if (fornecedor.getEmail() == null || fornecedor.getEmail().isBlank()) {
            preparada.setNull(4, Types.VARCHAR);
        } else {
            preparada.setString(4, fornecedor.getEmail().strip());
        }
        if (fornecedor.getEndereco() == null || fornecedor.getEndereco().isBlank()) {
            preparada.setNull(5, Types.VARCHAR);
        } else {
            preparada.setString(5, fornecedor.getEndereco().strip());
        }
        preparada.setBoolean(6, fornecedor.isAtivo());
    }

    private Fornecedor mapear(ResultSet resultado) throws SQLException {
        Timestamp criado = resultado.getTimestamp("criado_em");
        return new Fornecedor(
                resultado.getLong("id"),
                resultado.getString("nome"),
                resultado.getString("cnpj"),
                resultado.getString("telefone"),
                resultado.getString("email"),
                resultado.getString("endereco"),
                resultado.getBoolean("ativo"),
                criado == null ? null : criado.toLocalDateTime());
    }
}
