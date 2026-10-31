package com.portfolio.sistemaloja.repository;

import com.portfolio.sistemaloja.db.ConexaoFonte;
import com.portfolio.sistemaloja.model.PerdaEstoque;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class PerdaEstoqueRepository {
    private final ConexaoFonte fonte;

    public PerdaEstoqueRepository(ConexaoFonte fonte) {
        this.fonte = fonte;
    }

    public void inserir(PerdaEstoque perda, Connection conexao) throws SQLException {
        String sql = "INSERT INTO perda_estoque (produto_id, quantidade, motivo, observacao, usuario_id, data_hora) "
                + "VALUES (?,?,?,?,?,CURRENT_TIMESTAMP)";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setLong(1, perda.getProdutoId());
            ps.setInt(2, perda.getQuantidade());
            ps.setString(3, perda.getMotivo());
            ps.setString(4, perda.getObservacao());
            ps.setLong(5, perda.getUsuarioId());
            ps.executeUpdate();
        }
    }

    public List<PerdaEstoque> listarPorProduto(long produtoId) {
        String sql = """
                SELECT id, produto_id, quantidade, motivo, observacao, usuario_id, data_hora
                  FROM perda_estoque
                 WHERE produto_id = ?
                 ORDER BY data_hora DESC, id DESC""";
        List<PerdaEstoque> lista = new ArrayList<>();
        try (Connection conexao = fonte.getConnection();
             PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setLong(1, produtoId);
            try (ResultSet resultado = ps.executeQuery()) {
                while (resultado.next()) {
                    lista.add(mapear(resultado));
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao listar perdas de estoque", e);
        }
        return lista;
    }

    private PerdaEstoque mapear(ResultSet resultado) throws SQLException {
        PerdaEstoque perda = new PerdaEstoque();
        perda.setId(resultado.getLong("id"));
        perda.setProdutoId(resultado.getLong("produto_id"));
        perda.setQuantidade(resultado.getInt("quantidade"));
        perda.setMotivo(resultado.getString("motivo"));
        perda.setObservacao(resultado.getString("observacao"));
        long usuarioId = resultado.getLong("usuario_id");
        perda.setUsuarioId(resultado.wasNull() ? null : usuarioId);
        Timestamp dataHora = resultado.getTimestamp("data_hora");
        perda.setDataHora(dataHora == null ? null : dataHora.toLocalDateTime());
        return perda;
    }
}
