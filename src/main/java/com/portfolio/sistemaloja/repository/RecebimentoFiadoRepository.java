package com.portfolio.sistemaloja.repository;

import com.portfolio.sistemaloja.db.ConexaoFonte;
import com.portfolio.sistemaloja.model.RecebimentoFiado;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RecebimentoFiadoRepository {

    private static final String SELECT_BASE = """
            SELECT r.id, r.fiado_id, r.data_pagamento, r.valor, r.forma_pagamento,
                   r.usuario_id, r.observacao
              FROM recebimentos_fiado r""";

    private final ConexaoFonte fonte;

    public RecebimentoFiadoRepository(ConexaoFonte fonte) {
        this.fonte = fonte;
    }

    public RecebimentoFiado salvar(RecebimentoFiado recebimento) {
        String sql = "INSERT INTO recebimentos_fiado (fiado_id, data_pagamento, valor, "
                + "forma_pagamento, usuario_id, observacao) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparada.setLong(1, recebimento.fiadoId());
            preparada.setObject(2, recebimento.dataPagamento());
            preparada.setBigDecimal(3, recebimento.valor());
            preparada.setString(4, recebimento.formaPagamento());
            preparada.setLong(5, recebimento.usuarioId());
            preparada.setString(6, recebimento.observacao());
            preparada.executeUpdate();
            try (ResultSet chaves = preparada.getGeneratedKeys()) {
                if (chaves.next()) {
                    return new RecebimentoFiado(chaves.getLong(1), recebimento.fiadoId(),
                            recebimento.dataPagamento(), recebimento.valor(),
                            recebimento.formaPagamento(), recebimento.usuarioId(),
                            recebimento.observacao());
                }
            }
            return recebimento;
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao salvar recebimento de fiado", e);
        }
    }

    public List<RecebimentoFiado> listarPorFiado(long fiadoId) {
        String sql = SELECT_BASE + " WHERE r.fiado_id = ? ORDER BY r.data_pagamento DESC";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setLong(1, fiadoId);
            try (ResultSet resultado = preparada.executeQuery()) {
                List<RecebimentoFiado> lista = new ArrayList<>();
                while (resultado.next()) {
                    lista.add(mapear(resultado));
                }
                return lista;
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao listar recebimentos de fiado", e);
        }
    }

    private RecebimentoFiado mapear(ResultSet resultado) throws SQLException {
        return new RecebimentoFiado(
                resultado.getLong("id"),
                resultado.getLong("fiado_id"),
                resultado.getObject("data_pagamento", LocalDateTime.class),
                resultado.getBigDecimal("valor"),
                resultado.getString("forma_pagamento"),
                resultado.getLong("usuario_id"),
                resultado.getString("observacao")
        );
    }
}
