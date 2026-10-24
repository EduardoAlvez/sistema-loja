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

    public FiadoRepository(ConexaoFonte fonte) {
        this.fonte = fonte;
    }

    public Fiado salvar(Fiado fiado) {
        String sql;
        if (fiado.id() == null) {
            sql = "INSERT INTO fiados (venda_id, cliente_id, valor_total, valor_pago, valor_devedor, "
                    + "juros_aplicados, ultimo_calculo_juros, data_vencimento, status, observacao) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        } else {
            sql = "UPDATE fiados SET venda_id = ?, cliente_id = ?, valor_total = ?, valor_pago = ?, "
                    + "valor_devedor = ?, juros_aplicados = ?, ultimo_calculo_juros = ?, "
                    + "data_vencimento = ?, status = ?, observacao = ? WHERE id = ?";
        }
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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
            if (fiado.id() != null) {
                preparada.setLong(11, fiado.id());
            }
            preparada.executeUpdate();
            if (fiado.id() == null) {
                try (ResultSet chaves = preparada.getGeneratedKeys()) {
                    if (chaves.next()) {
                        fiado = new Fiado(chaves.getLong(1), fiado.vendaId(), fiado.clienteId(),
                                fiado.valorTotal(), fiado.valorPago(), fiado.valorDevedor(),
                                fiado.jurosAplicados(), fiado.dataVencimento(), fiado.status(),
                                fiado.observacao(), fiado.ultimoCalculoJuros());
                    }
                }
            }
            return fiado;
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao salvar fiado", e);
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

    public List<Fiado> listarAbertos() {
        return consultar(SELECT_BASE + " WHERE f.status = 'ABERTO' ORDER BY f.data_vencimento");
    }

    public List<Fiado> listarVencidos(LocalDate dataReferencia) {
        String sql = SELECT_BASE + " WHERE f.status IN ('ABERTO', 'PARCIAL') AND f.data_vencimento < ? "
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

    public BigDecimal saldoDevedorTotalPorCliente(long clienteId) {
        String sql = "SELECT COALESCE(SUM(valor_devedor), 0) FROM fiados WHERE cliente_id = ? "
                + "AND status IN ('ABERTO', 'PARCIAL')";
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
