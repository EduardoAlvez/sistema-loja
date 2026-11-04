package com.portfolio.sistemaloja.repository;

import com.portfolio.sistemaloja.db.ConexaoFonte;
import com.portfolio.sistemaloja.model.Fiado;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FiadoRepository {

    private static final String SELECT_BASE = """
            SELECT f.id, f.venda_id, f.cliente_id, f.valor_total, f.valor_pago, f.valor_devedor,
                   f.juros_aplicados, f.ultimo_calculo_juros, f.data_vencimento, f.status, f.observacao
              FROM fiados f""";

    private final ConexaoFonte fonte;
    private java.util.function.Consumer<Connection> falharAoSalvar = conexao -> {
    };

    public FiadoRepository(ConexaoFonte fonte) {
        this.fonte = fonte;
    }

    public void falharAoSalvarParaTeste(java.util.function.Consumer<Connection> hook) {
        this.falharAoSalvar = hook;
    }

    public Fiado salvar(Fiado fiado) {
        if (fiado.id() == null) {
            try (Connection conexao = fonte.getConnection()) {
                return inserir(fiado, conexao);
            } catch (SQLException e) {
                throw new RepositorioException("Falha ao salvar fiado", e);
            }
        }
        return atualizar(fiado);
    }

    public Fiado salvar(Fiado fiado, Connection conexao) throws SQLException {
        return fiado.id() == null ? inserir(fiado, conexao) : atualizar(fiado, conexao);
    }

    private Fiado inserir(Fiado fiado, Connection conexao) throws SQLException {
        String sql = "INSERT INTO fiados (venda_id, cliente_id, valor_total, valor_pago, valor_devedor, "
                + "juros_aplicados, ultimo_calculo_juros, data_vencimento, status, observacao) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        falharAoSalvar.accept(conexao);
        try (PreparedStatement preparada = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparada.setLong(1, fiado.vendaId());
            preparada.setLong(2, fiado.clienteId());
            preparada.setBigDecimal(3, fiado.valorTotal());
            preparada.setBigDecimal(4, fiado.valorPago());
            preparada.setBigDecimal(5, fiado.valorDevedor());
            preparada.setBigDecimal(6, fiado.jurosAplicados());
            preparada.setObject(7, fiado.ultimoCalculoJuros());
            preparada.setObject(8, fiado.dataVencimento());
            preparada.setString(9, fiado.status());
            preparada.setString(10, fiado.observacao());
            preparada.executeUpdate();
            try (ResultSet chaves = preparada.getGeneratedKeys()) {
                if (chaves.next()) {
                    return new Fiado(chaves.getLong(1), fiado.vendaId(), fiado.clienteId(),
                            fiado.valorTotal(), fiado.valorPago(), fiado.valorDevedor(),
                            fiado.jurosAplicados(), fiado.dataVencimento(), fiado.status(),
                            fiado.observacao(), fiado.ultimoCalculoJuros());
                }
            }
            return fiado;
        }
    }

    private Fiado atualizar(Fiado fiado) {
        try (Connection conexao = fonte.getConnection()) {
            return atualizar(fiado, conexao);
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao salvar fiado", e);
        }
    }

    private Fiado atualizar(Fiado fiado, Connection conexao) throws SQLException {
        String sql = "UPDATE fiados SET venda_id = ?, cliente_id = ?, valor_total = ?, valor_pago = ?, "
                + "valor_devedor = ?, juros_aplicados = ?, ultimo_calculo_juros = ?, "
                + "data_vencimento = ?, status = ?, observacao = ? WHERE id = ?";
        try (PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setLong(1, fiado.vendaId());
            preparada.setLong(2, fiado.clienteId());
            preparada.setBigDecimal(3, fiado.valorTotal());
            preparada.setBigDecimal(4, fiado.valorPago());
            preparada.setBigDecimal(5, fiado.valorDevedor());
            preparada.setBigDecimal(6, fiado.jurosAplicados());
            preparada.setObject(7, fiado.ultimoCalculoJuros());
            preparada.setObject(8, fiado.dataVencimento());
            preparada.setString(9, fiado.status());
            preparada.setString(10, fiado.observacao());
            preparada.setLong(11, fiado.id());
            preparada.executeUpdate();
            return fiado;
        }
    }

    public Optional<Fiado> buscarPorId(long id) {
        String sql = SELECT_BASE + " WHERE f.id = ?";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setLong(1, id);
            try (ResultSet resultado = preparada.executeQuery()) {
                if (resultado.next()) {
                    return Optional.of(mapear(resultado));
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao buscar fiado", e);
        }
        return Optional.empty();
    }

    /**
     * Fila de cobranca: tudo que ainda e devedor (issue #44). QUITADO fica fora;
     * INADIMPLENTE so passa a existir no banco com a marcação da issue #43.
     */
    public List<Fiado> listarPendentes() {
        return consultar(SELECT_BASE + " WHERE f.status IN ('ABERTO', 'PARCIAL', 'INADIMPLENTE') "
                + "ORDER BY f.data_vencimento");
    }

    public List<Fiado> listarVencidos(LocalDate dataReferencia) {
        String sql = SELECT_BASE + " WHERE f.status IN ('ABERTO', 'PARCIAL', 'INADIMPLENTE') AND f.data_vencimento < ? "
                + "ORDER BY f.data_vencimento";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setObject(1, dataReferencia);
            try (ResultSet resultado = preparada.executeQuery()) {
                List<Fiado> lista = new ArrayList<>();
                while (resultado.next()) {
                    lista.add(mapear(resultado));
                }
                return lista;
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao listar fiados vencidos", e);
        }
    }

    public List<Fiado> listarPorCliente(long clienteId) {
        String sql = SELECT_BASE + " WHERE f.cliente_id = ? ORDER BY f.data_vencimento";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setLong(1, clienteId);
            try (ResultSet resultado = preparada.executeQuery()) {
                List<Fiado> lista = new ArrayList<>();
                while (resultado.next()) {
                    lista.add(mapear(resultado));
                }
                return lista;
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao listar fiados por cliente", e);
        }
    }

    /**
     * Marca como INADIMPLENTE os fiados ainda nao pagos com vencimento anterior a
     * dataReferencia. O SELECT...FOR UPDATE (mesmo mecanismo de bloquearVenda) e o
     * guard do UPDATE juntos garantem que cada fiado e marcado — e auditado — uma
     * unica vez: um segundo concorrente bloqueia no SELECT e, ao retomar, nao encontra
     * mais nada. Retorna os ids efetivamente marcados, para o chamador auditar.
     */
    public List<Long> marcarVencidos(LocalDate dataReferencia, Connection conexao) throws SQLException {
        List<Long> ids = new ArrayList<>();
        String select = "SELECT id FROM fiados WHERE status IN ('ABERTO', 'PARCIAL') "
                + "AND data_vencimento < ? FOR UPDATE";
        try (PreparedStatement preparada = conexao.prepareStatement(select)) {
            preparada.setObject(1, dataReferencia);
            try (ResultSet resultado = preparada.executeQuery()) {
                while (resultado.next()) {
                    ids.add(resultado.getLong(1));
                }
            }
        }
        if (ids.isEmpty()) {
            return ids;
        }
        String update = "UPDATE fiados SET status = 'INADIMPLENTE' WHERE status IN ('ABERTO', 'PARCIAL') "
                + "AND data_vencimento < ?";
        try (PreparedStatement preparada = conexao.prepareStatement(update)) {
            preparada.setObject(1, dataReferencia);
            preparada.executeUpdate();
        }
        return ids;
    }

    public BigDecimal saldoDevedorTotalPorCliente(long clienteId) {
        String sql = "SELECT COALESCE(SUM(valor_devedor), 0) FROM fiados WHERE cliente_id = ? "
                + "AND status IN ('ABERTO', 'PARCIAL', 'INADIMPLENTE')";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setLong(1, clienteId);
            try (ResultSet resultado = preparada.executeQuery()) {
                if (resultado.next()) {
                    return resultado.getBigDecimal(1);
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao calcular saldo devedor por cliente", e);
        }
        return BigDecimal.ZERO;
    }

    private List<Fiado> consultar(String sql) {
        List<Fiado> fiados = new ArrayList<>();
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql);
             ResultSet resultado = preparada.executeQuery()) {
            while (resultado.next()) {
                fiados.add(mapear(resultado));
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao consultar fiados", e);
        }
        return fiados;
    }

    private Fiado mapear(ResultSet resultado) throws SQLException {
        return new Fiado(
                resultado.getLong("id"),
                resultado.getLong("venda_id"),
                resultado.getLong("cliente_id"),
                resultado.getBigDecimal("valor_total"),
                resultado.getBigDecimal("valor_pago"),
                resultado.getBigDecimal("valor_devedor"),
                resultado.getBigDecimal("juros_aplicados"),
                resultado.getObject("data_vencimento", LocalDate.class),
                resultado.getString("status"),
                resultado.getString("observacao"),
                resultado.getObject("ultimo_calculo_juros", LocalDateTime.class)
        );
    }
}
