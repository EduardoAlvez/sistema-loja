package com.portfolio.sistemaloja.repository;

import com.portfolio.sistemaloja.db.ConexaoFonte;
import com.portfolio.sistemaloja.model.Caixa;
import com.portfolio.sistemaloja.model.CaixaLancamento;
import com.portfolio.sistemaloja.model.StatusCaixa;
import com.portfolio.sistemaloja.model.TipoLancamentoCaixa;

import java.math.BigDecimal;
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
            return buscarPorId(caixa.getId()).orElse(caixa);
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao abrir o caixa", e);
        }
    }

    public Optional<Caixa> buscarAberto() {
        String sql = SELECT_BASE + " WHERE c.status = ? ORDER BY c.id DESC";
        List<Caixa> abertos = consultar(sql, StatusCaixa.ABERTO.name());
        return abertos.isEmpty() ? Optional.empty() : Optional.of(abertos.get(0));
    }

    public Caixa fechar(long id, BigDecimal valorFechamento, Long usuarioFechamentoId, String observacao) {
        String sql = "UPDATE caixas SET data_fechamento = CURRENT_TIMESTAMP, usuario_fechamento_id = ?, "
                + "valor_fechamento = ?, status = ?, observacao = ? WHERE id = ? AND status = ?";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setLong(1, usuarioFechamentoId);
            preparada.setBigDecimal(2, valorFechamento);
            preparada.setString(3, StatusCaixa.FECHADO.name());
            preparada.setString(4, observacao);
            preparada.setLong(5, id);
            preparada.setString(6, StatusCaixa.ABERTO.name());
            if (preparada.executeUpdate() == 0) {
                throw new RepositorioException("O caixa já está fechado", null);
            }
            return buscarPorId(id)
                    .orElseThrow(() -> new RepositorioException("Caixa não encontrado", null));
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao fechar o caixa", e);
        }
    }

    public Optional<Caixa> buscarPorId(long id) {
        String sql = SELECT_BASE + " WHERE c.id = ?";
        List<Caixa> encontrados = consultar(sql, id);
        return encontrados.isEmpty() ? Optional.empty() : Optional.of(encontrados.get(0));
    }

    public List<Caixa> listar() {
        return consultar(SELECT_BASE + " ORDER BY c.id DESC", (Object[]) null);
    }

    public CaixaLancamento registrarLancamento(CaixaLancamento lancamento) {
        String sql = "INSERT INTO caixa_lancamentos (caixa_id, tipo, valor, usuario_id, observacao) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparada.setLong(1, lancamento.getCaixaId());
            preparada.setString(2, lancamento.getTipo().name());
            preparada.setBigDecimal(3, lancamento.getValor());
            if (lancamento.getUsuarioId() == null) {
                preparada.setObject(4, null);
            } else {
                preparada.setLong(4, lancamento.getUsuarioId());
            }
            preparada.setString(5, lancamento.getObservacao());
            preparada.executeUpdate();
            try (ResultSet chaves = preparada.getGeneratedKeys()) {
                if (chaves.next()) {
                    lancamento.setId(chaves.getLong(1));
                }
            }
            return lancamento;
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao registrar lançamento do caixa", e);
        }
    }

    public List<CaixaLancamento> listarLancamentos(long caixaId) {
        String sql = """
                SELECT l.id, l.caixa_id, l.data_hora, l.tipo, l.valor, l.usuario_id, u.nome AS usuario_nome,
                       l.observacao
                  FROM caixa_lancamentos l
                  LEFT JOIN usuarios u ON u.id = l.usuario_id
                 WHERE l.caixa_id = ?
                 ORDER BY l.id""";
        List<CaixaLancamento> lancamentos = new ArrayList<>();
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setLong(1, caixaId);
            try (ResultSet resultado = preparada.executeQuery()) {
                while (resultado.next()) {
                    lancamentos.add(mapearLancamento(resultado));
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao listar lançamentos do caixa", e);
        }
        return lancamentos;
    }

    private CaixaLancamento mapearLancamento(ResultSet resultado) throws SQLException {
        CaixaLancamento lancamento = new CaixaLancamento();
        lancamento.setId(resultado.getLong("id"));
        lancamento.setCaixaId(resultado.getLong("caixa_id"));
        lancamento.setDataHora(resultado.getString("data_hora"));
        lancamento.setTipo(TipoLancamentoCaixa.valueOf(resultado.getString("tipo")));
        lancamento.setValor(resultado.getBigDecimal("valor"));
        long usuarioId = resultado.getLong("usuario_id");
        if (!resultado.wasNull()) {
            lancamento.setUsuarioId(usuarioId);
            lancamento.setUsuarioNome(resultado.getString("usuario_nome"));
        }
        lancamento.setObservacao(resultado.getString("observacao"));
        return lancamento;
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
