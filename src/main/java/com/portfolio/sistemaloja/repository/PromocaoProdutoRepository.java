package com.portfolio.sistemaloja.repository;

import com.portfolio.sistemaloja.db.ConexaoFonte;
import com.portfolio.sistemaloja.model.PromocaoProduto;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PromocaoProdutoRepository {

    private static final String SELECT_BASE = """
            SELECT p.id, p.produto_id, p.nome, p.quantidade_minima, p.preco_promocional,
                   p.ativa, p.data_inicio, p.data_fim, p.dias_semana,
                   p.horario_inicio, p.horario_fim,
                   prod.nome as produto_nome
            FROM promocao_produto p
            LEFT JOIN produtos prod ON prod.id = p.produto_id
        """;

    private final ConexaoFonte fonte;

    public PromocaoProdutoRepository(ConexaoFonte fonte) {
        this.fonte = fonte;
    }

    public List<PromocaoProduto> listar() {
        String sql = SELECT_BASE + " ORDER BY p.nome";
        List<PromocaoProduto> lista = new ArrayList<>();
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql);
             ResultSet resultado = preparada.executeQuery()) {
            while (resultado.next()) {
                lista.add(mapear(resultado));
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao listar promoções de produto", e);
        }
        return lista;
    }

    public List<PromocaoProduto> listarAtivas() {
        String sql = SELECT_BASE + " WHERE p.ativa = TRUE ORDER BY p.nome";
        List<PromocaoProduto> lista = new ArrayList<>();
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql);
             ResultSet resultado = preparada.executeQuery()) {
            while (resultado.next()) {
                lista.add(mapear(resultado));
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao listar promoções ativas", e);
        }
        return lista;
    }

    public Optional<PromocaoProduto> buscarPorId(Long id) {
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
            throw new RepositorioException("Falha ao buscar promoção de produto", e);
        }
        return Optional.empty();
    }

    public List<PromocaoProduto> listarPorProduto(Long produtoId) {
        String sql = SELECT_BASE + " WHERE p.produto_id = ? ORDER BY p.nome";
        List<PromocaoProduto> lista = new ArrayList<>();
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setLong(1, produtoId);
            try (ResultSet resultado = preparada.executeQuery()) {
                while (resultado.next()) {
                    lista.add(mapear(resultado));
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao listar promoções do produto", e);
        }
        return lista;
    }

    public PromocaoProduto salvar(PromocaoProduto promocao) {
        String sql;
        if (promocao.getId() == null) {
            sql = "INSERT INTO promocao_produto (produto_id, nome, quantidade_minima, preco_promocional, " +
                  "ativa, data_inicio, data_fim, dias_semana, horario_inicio, horario_fim) " +
                  "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        } else {
            sql = "UPDATE promocao_produto SET produto_id = ?, nome = ?, quantidade_minima = ?, " +
                  "preco_promocional = ?, ativa = ?, data_inicio = ?, data_fim = ?, " +
                  "dias_semana = ?, horario_inicio = ?, horario_fim = ? WHERE id = ?";
        }

        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            preparada.setLong(1, promocao.getProdutoId());
            preparada.setString(2, promocao.getNome());
            preparada.setInt(3, promocao.getQuantidadeMinima());
            preparada.setBigDecimal(4, promocao.getPrecoPromocional());
            preparada.setBoolean(5, promocao.getAtiva());
            preparada.setObject(6, promocao.getDataInicio());
            preparada.setObject(7, promocao.getDataFim());
            preparada.setString(8, promocao.getDiasSemana());
            preparada.setObject(9, promocao.getHorarioInicio());
            preparada.setObject(10, promocao.getHorarioFim());

            if (promocao.getId() != null) {
                preparada.setLong(11, promocao.getId());
            }

            preparada.executeUpdate();

            if (promocao.getId() == null) {
                try (ResultSet chaves = preparada.getGeneratedKeys()) {
                    if (chaves.next()) {
                        promocao.setId(chaves.getLong(1));
                    }
                }
            }
            return promocao;
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao salvar promoção de produto", e);
        }
    }

    public void deletar(Long id) {
        String sql = "DELETE FROM promocao_produto WHERE id = ?";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setLong(1, id);
            preparada.executeUpdate();
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao deletar promoção de produto", e);
        }
    }

    private PromocaoProduto mapear(ResultSet resultado) throws SQLException {
        PromocaoProduto promocao = new PromocaoProduto();
        promocao.setId(resultado.getLong("id"));
        promocao.setProdutoId(resultado.getLong("produto_id"));
        promocao.setNome(resultado.getString("nome"));
        promocao.setQuantidadeMinima(resultado.getInt("quantidade_minima"));
        promocao.setPrecoPromocional(resultado.getBigDecimal("preco_promocional"));
        promocao.setAtiva(resultado.getBoolean("ativa"));
        promocao.setDataInicio(resultado.getObject("data_inicio", LocalDate.class));
        promocao.setDataFim(resultado.getObject("data_fim", LocalDate.class));
        promocao.setDiasSemana(resultado.getString("dias_semana"));
        promocao.setHorarioInicio(resultado.getObject("horario_inicio", LocalTime.class));
        promocao.setHorarioFim(resultado.getObject("horario_fim", LocalTime.class));
        return promocao;
    }
}
