package com.portfolio.sistemaloja.repository;

import com.portfolio.sistemaloja.db.ConexaoFonte;
import com.portfolio.sistemaloja.model.ExtratoFidelidade;
import com.portfolio.sistemaloja.model.FidelidadeMovimento;
import com.portfolio.sistemaloja.model.TipoMovimentoFidelidade;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FidelidadeRepository {

    private final ConexaoFonte fonte;

    public FidelidadeRepository(ConexaoFonte fonte) {
        this.fonte = fonte;
    }

    public void aplicarMovimentos(List<FidelidadeMovimento> movimentos) {
        try (Connection conexao = fonte.getConnection()) {
            aplicarMovimentos(conexao, movimentos);
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao registrar o movimento de fidelidade", e);
        }
    }

    public void aplicarMovimentos(Connection conexao, List<FidelidadeMovimento> movimentos) throws SQLException {
        aplicarMovimentos(conexao, movimentos, null, null);
    }

    public void aplicarMovimentos(Connection conexao, List<FidelidadeMovimento> movimentos, Long vendaContexto,
            Long devolucaoContexto) throws SQLException {
        for (FidelidadeMovimento movimento : movimentos) {
            inserirExtrato(movimento, conexao, vendaContexto, devolucaoContexto);
            atualizarSaldo(movimento, conexao);
        }
    }

    public int saldo(long clienteId) {
        String sql = "SELECT saldo_pontos FROM clientes WHERE id = ?";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setLong(1, clienteId);
            try (ResultSet resultado = preparada.executeQuery()) {
                return resultado.next() ? resultado.getInt("saldo_pontos") : 0;
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao consultar o saldo de pontos", e);
        }
    }

    public List<ExtratoFidelidade> extrato(long clienteId, int limite) {
        String sql = "SELECT data_hora, tipo, pontos, descricao, venda_id FROM fidelidade_extrato "
                + "WHERE cliente_id = ? ORDER BY id DESC";
        List<ExtratoFidelidade> movimentos = new ArrayList<>();
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setLong(1, clienteId);
            try (ResultSet resultado = preparada.executeQuery()) {
                while (resultado.next() && movimentos.size() < limite) {
                    Timestamp dataHora = resultado.getTimestamp("data_hora");
                    Long vendaId = resultado.getObject("venda_id", Long.class);
                    movimentos.add(new ExtratoFidelidade(
                            dataHora == null ? null : dataHora.toLocalDateTime(),
                            TipoMovimentoFidelidade.valueOf(resultado.getString("tipo")),
                            resultado.getInt("pontos"),
                            resultado.getString("descricao"),
                            vendaId));
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao consultar o extrato de pontos", e);
        }
        return movimentos;
    }

    public int pontosEstornados(Connection conexao, long vendaId) throws SQLException {
        String sql = "SELECT pontos_estornados FROM vendas WHERE id = ?";
        try (PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setLong(1, vendaId);
            try (ResultSet resultado = preparada.executeQuery()) {
                return resultado.next() ? resultado.getInt(1) : 0;
            }
        }
    }

    public int estornarCredito(Connection conexao, long clienteId, long vendaId, Long devolucaoId,
            int pontosSolicitados, String descricao) throws SQLException {
        if (pontosSolicitados <= 0) {
            return 0;
        }
        int pontosCreditados = pontosCreditados(conexao, vendaId);
        int jaEstornados = pontosEstornados(conexao, vendaId);
        int aEstornar = Math.min(pontosSolicitados, pontosCreditados - jaEstornados);
        if (aEstornar <= 0) {
            return 0;
        }
        String sql = "UPDATE vendas SET pontos_estornados = pontos_estornados + ? "
                + "WHERE id = ? AND pontos_estornados + ? <= pontos_creditados";
        try (PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setInt(1, aEstornar);
            preparada.setLong(2, vendaId);
            preparada.setInt(3, aEstornar);
            if (preparada.executeUpdate() == 0) {
                return 0;
            }
        }
        FidelidadeMovimento movimento = new FidelidadeMovimento(clienteId, vendaId, devolucaoId,
                TipoMovimentoFidelidade.ESTORNO_CREDITO, aEstornar, descricao);
        inserirExtrato(movimento, conexao, null, null);
        atualizarSaldo(movimento, conexao);
        return aEstornar;
    }

    private int pontosCreditados(Connection conexao, long vendaId) throws SQLException {
        String sql = "SELECT pontos_creditados FROM vendas WHERE id = ?";
        try (PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setLong(1, vendaId);
            try (ResultSet resultado = preparada.executeQuery()) {
                return resultado.next() ? resultado.getInt(1) : 0;
            }
        }
    }

    private void inserirExtrato(FidelidadeMovimento movimento, Connection conexao, Long vendaContexto,
            Long devolucaoContexto) throws SQLException {
        Long vendaId = movimento.vendaId() != null ? movimento.vendaId() : vendaContexto;
        Long devolucaoId = movimento.devolucaoId() != null ? movimento.devolucaoId() : devolucaoContexto;
        String sql = "INSERT INTO fidelidade_extrato (cliente_id, venda_id, devolucao_id, tipo, pontos, descricao) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setLong(1, movimento.clienteId());
            if (vendaId == null) {
                preparada.setObject(2, null);
            } else {
                preparada.setLong(2, vendaId);
            }
            if (devolucaoId == null) {
                preparada.setObject(3, null);
            } else {
                preparada.setLong(3, devolucaoId);
            }
            preparada.setString(4, movimento.tipo().name());
            preparada.setInt(5, movimento.pontos());
            preparada.setString(6, movimento.descricao());
            preparada.executeUpdate();
        }
    }

    private void atualizarSaldo(FidelidadeMovimento movimento, Connection conexao) throws SQLException {
        if (movimento.tipo() == TipoMovimentoFidelidade.DEBITO) {
            debitarComSaldoSuficiente(movimento, conexao);
            return;
        }
        if (movimento.tipo() == TipoMovimentoFidelidade.ESTORNO_CREDITO) {
            debitar(movimento, conexao);
            return;
        }
        String sql = "UPDATE clientes SET saldo_pontos = saldo_pontos + ? WHERE id = ?";
        try (PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setInt(1, movimento.pontos());
            preparada.setLong(2, movimento.clienteId());
            preparada.executeUpdate();
        }
    }

    private void debitar(FidelidadeMovimento movimento, Connection conexao) throws SQLException {
        String sql = "UPDATE clientes SET saldo_pontos = saldo_pontos - ? WHERE id = ?";
        try (PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setInt(1, movimento.pontos());
            preparada.setLong(2, movimento.clienteId());
            preparada.executeUpdate();
        }
    }

    private void debitarComSaldoSuficiente(FidelidadeMovimento movimento, Connection conexao) throws SQLException {
        String sql = "UPDATE clientes SET saldo_pontos = saldo_pontos - ? WHERE id = ? AND saldo_pontos >= ?";
        try (PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setInt(1, movimento.pontos());
            preparada.setLong(2, movimento.clienteId());
            preparada.setInt(3, movimento.pontos());
            if (preparada.executeUpdate() == 0) {
                throw new SQLException("Saldo de pontos insuficiente para o resgate.");
            }
        }
    }
}
