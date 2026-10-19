package com.portfolio.sistemaloja.repository;

import com.portfolio.sistemaloja.db.ConexaoFonte;
import com.portfolio.sistemaloja.model.Caixa;
import com.portfolio.sistemaloja.model.StatusCaixa;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CaixaRepository {

    private static final String SELECT_BASE = """
            SELECT c.id, c.data_abertura, c.data_fechamento, c.usuario_abertura_id, ua.nome AS usuario_abertura_nome,
                   c.usuario_fechamento_id, uf.nome AS usuario_fechamento_nome, c.valor_abertura,
                   c.valor_fechamento, c.status, c.observacao
              FROM caixas c
              LEFT JOIN usuarios ua ON ua.id = c.usuario_abertura_id
              LEFT JOIN usuarios uf ON uf.id = c.usuario_fechamento_id""";

    private final ConexaoFonte fonte;

    public CaixaRepository(ConexaoFonte fonte) {
        this.fonte = fonte;
    }

    public Caixa abrir(Caixa caixa) {
        String sql = "INSERT INTO caixas (usuario_abertura_id, valor_abertura, status) VALUES (?, ?, ?)";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparada.setLong(1, caixa.getUsuarioAberturaId());
            preparada.setBigDecimal(2, caixa.getValorAbertura());
            preparada.setString(3, StatusCaixa.ABERTO.name());
            preparada.executeUpdate();
            try (ResultSet chaves = preparada.getGeneratedKeys()) {
                if (chaves.next()) {
                    caixa.setId(chaves.getLong(1));
                }
            }
            return caixa;
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao abrir o caixa", e);
        }
    }

    public Optional<Caixa> buscarAberto() {
        String sql = SELECT_BASE + " WHERE c.status = ? ORDER BY c.id DESC";
        List<Caixa> abertos = consultar(sql, StatusCaixa.ABERTO.name());
        return abertos.isEmpty() ? Optional.empty() : Optional.of(abertos.get(0));
    }

    public Optional<Caixa> buscarPorId(long id) {
        String sql = SELECT_BASE + " WHERE c.id = ?";
        List<Caixa> encontrados = consultar(sql, id);
        return encontrados.isEmpty() ? Optional.empty() : Optional.of(encontrados.get(0));
    }

    public List<Caixa> listar() {
        return consultar(SELECT_BASE + " ORDER BY c.id DESC", (Object[]) null);
    }

    private List<Caixa> consultar(String sql, Object... parametros) {
        List<Caixa> caixas = new ArrayList<>();
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            if (parametros != null) {
                for (int i = 0; i < parametros.length; i++) {
                    preparada.setObject(i + 1, parametros[i]);
                }
            }
            try (ResultSet resultado = preparada.executeQuery()) {
                while (resultado.next()) {
                    caixas.add(mapear(resultado));
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao consultar caixas", e);
        }
        return caixas;
    }

    private Caixa mapear(ResultSet resultado) throws SQLException {
        Caixa caixa = new Caixa();
        caixa.setId(resultado.getLong("id"));
        caixa.setDataAbertura(resultado.getString("data_abertura"));
        caixa.setDataFechamento(resultado.getString("data_fechamento"));
        caixa.setUsuarioAberturaId(resultado.getLong("usuario_abertura_id"));
        caixa.setUsuarioAberturaNome(resultado.getString("usuario_abertura_nome"));
        long usuarioFechamentoId = resultado.getLong("usuario_fechamento_id");
        if (!resultado.wasNull()) {
            caixa.setUsuarioFechamentoId(usuarioFechamentoId);
            caixa.setUsuarioFechamentoNome(resultado.getString("usuario_fechamento_nome"));
        }
        caixa.setValorAbertura(resultado.getBigDecimal("valor_abertura"));
        caixa.setValorFechamento(resultado.getBigDecimal("valor_fechamento"));
        caixa.setStatus(StatusCaixa.valueOf(resultado.getString("status")));
        caixa.setObservacao(resultado.getString("observacao"));
        return caixa;
    }
}
