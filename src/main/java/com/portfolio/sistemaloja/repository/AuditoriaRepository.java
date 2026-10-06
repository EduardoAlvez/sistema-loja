package com.portfolio.sistemaloja.repository;

import com.portfolio.sistemaloja.db.ConexaoFonte;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AuditoriaRepository {

    private final ConexaoFonte fonte;

    public AuditoriaRepository(ConexaoFonte fonte) {
        this.fonte = fonte;
    }

    public void registrar(String usuarioLogin, String acao, String entidade, Long entidadeId, String detalhe) {
        String sql = "INSERT INTO auditoria (usuario_login, acao, entidade, entidade_id, detalhe) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setString(1, usuarioLogin);
            preparada.setString(2, acao);
            preparada.setString(3, entidade);
            preparada.setObject(4, entidadeId);
            preparada.setString(5, detalhe);
            preparada.executeUpdate();
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao registrar evento de auditoria", e);
        }
    }

    public List<EventoAuditoria> listar(LocalDate inicio, LocalDate fim) {
        String sql = """
                SELECT data_hora, usuario_login, acao, entidade, entidade_id, detalhe
                  FROM auditoria
                 WHERE data_hora >= ? AND data_hora < ?
                 ORDER BY data_hora DESC, id DESC""";
        List<EventoAuditoria> lista = new ArrayList<>();
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setObject(1, inicio);
            preparada.setObject(2, fim.plusDays(1));
            try (ResultSet resultado = preparada.executeQuery()) {
                while (resultado.next()) {
                    long entidadeId = resultado.getLong("entidade_id");
                    lista.add(new EventoAuditoria(
                            resultado.getString("data_hora"),
                            resultado.getString("usuario_login"),
                            resultado.getString("acao"),
                            resultado.getString("entidade"),
                            resultado.wasNull() ? null : entidadeId,
                            resultado.getString("detalhe")));
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao listar auditoria", e);
        }
        return lista;
    }
}
