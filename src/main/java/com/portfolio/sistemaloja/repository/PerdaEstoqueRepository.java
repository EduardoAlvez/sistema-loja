package com.portfolio.sistemaloja.repository;

import com.portfolio.sistemaloja.db.ConexaoFonte;
import com.portfolio.sistemaloja.model.PerdidaEstoque;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class PerdaEstoqueRepository {

    private final ConexaoFonte fonte;

    public PerdaEstoqueRepository(ConexaoFonte fonte) {
        this.fonte = fonte;
    }

    public PerdaEstoque registrar(PerdaEstoque perda, Connection conexao) throws SQLException {
        String sql = "INSERT INTO perda_estoque (produto_id, quantidade, motivo, usuario_id, observacao, data_hora) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, perda.getProdutoId());
            ps.setInt(2, perda.getQuantidade());
            ps.setString(3, perda.getMotivo() == null ? "OUTRO" : perda.getMotivo().name());
            ps.setLong(4, perda.getUsuarioId());
            ps.setString(5, perda.getObservacao());
            ps.setTimestamp(6, Timestamp.valueOf(perda.getDataHora() == null ? LocalDateTime.now() : perda.getDataHora()));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) perda.setId(rs.getLong(1));
            }
            return perda;
        }
    }
}
