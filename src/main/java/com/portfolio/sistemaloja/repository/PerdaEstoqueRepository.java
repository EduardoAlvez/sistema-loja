package com.portfolio.sistemaloja.repository;

import com.portfolio.sistemaloja.db.FonteConexao;
import com.portfolio.sistemaloja.model.PerdaEstoque;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class PerdaEstoqueRepository {
    private final FonteConexao fonteConexao;

    public PerdaEstoqueRepository(FonteConexao fonteConexao) {
        this.fonteConexao = fonteConexao;
    }

    public void inserir(PerdaEstoque perda, Connection conexao) throws SQLException {
        String sql = "INSERT INTO perda_estoque (produto_id, quantidade, motivo, observacao, usuario_id, data_hora) VALUES (?,?,?,?,?,CURRENT_TIMESTAMP)";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setLong(1, perda.getProdutoId());
            ps.setInt(2, perda.getQuantidade());
            ps.setString(3, perda.getMotivo());
            ps.setString(4, perda.getObservacao());
            if (perda.getUsuarioId() == null) {
                ps.setObject(5, null);
            } else {
                ps.setLong(5, perda.getUsuarioId());
            }
            ps.executeUpdate();
        }
    }
}
