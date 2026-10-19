package com.portfolio.sistemaloja.repository;

import com.portfolio.sistemaloja.db.ConexaoFonte;
import com.portfolio.sistemaloja.model.FormaPagamento;
import com.portfolio.sistemaloja.model.StatusVenda;
import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.model.VendaItem;
import com.portfolio.sistemaloja.model.VendaPagamento;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class VendaRepository {

    private static final String SELECT_BASE = """
            SELECT v.id, v.numero, v.data_hora, v.cliente_id, cl.nome AS cliente_nome,
                   v.usuario_id, us.nome AS usuario_nome, v.forma_pagamento, v.desconto, v.total,
                   v.valor_pago, v.troco, v.status
              FROM vendas v
              LEFT JOIN clientes cl ON cl.id = v.cliente_id
              LEFT JOIN usuarios us ON us.id = v.usuario_id""";

    private final ConexaoFonte fonte;

    public VendaRepository(ConexaoFonte fonte) {
        this.fonte = fonte;
    }

    public Venda registrar(Venda venda) {
        try (Connection conexao = fonte.getConnection()) {
            try {
                conexao.setAutoCommit(false);
                inserirVenda(venda, conexao);
                inserirItens(venda, conexao);
                inserirPagamentos(venda, conexao);
                baixarEstoque(venda, conexao);
                conexao.commit();
            } catch (SQLException e) {
                conexao.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao registrar a venda", e);
        }
        return buscarPorId(venda.getId()).orElse(venda);
    }

    private void inserirVenda(Venda venda, Connection conexao) throws SQLException {
        String sql = "INSERT INTO vendas (numero, data_hora, cliente_id, usuario_id, forma_pagamento, "
                + "desconto, total, valor_pago, troco, status) "
                + "VALUES (?, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement preparada = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparada.setString(1, venda.getNumero());
            if (venda.getClienteId() == null) {
                preparada.setObject(2, null);
            } else {
                preparada.setLong(2, venda.getClienteId());
            }
            if (venda.getUsuarioId() == null) {
                preparada.setObject(3, null);
            } else {
                preparada.setLong(3, venda.getUsuarioId());
            }
            preparada.setString(4, venda.getFormaPagamento().name());
            preparada.setBigDecimal(5, venda.getDesconto());
            preparada.setBigDecimal(6, venda.getTotal());
            preparada.setBigDecimal(7, venda.getValorPago());
            preparada.setBigDecimal(8, venda.getTroco());
            preparada.setString(9, venda.getStatus().name());
            preparada.executeUpdate();
            try (ResultSet chaves = preparada.getGeneratedKeys()) {
                if (chaves.next()) {
                    venda.setId(chaves.getLong(1));
                }
            }
        }
    }

    private void inserirItens(Venda venda, Connection conexao) throws SQLException {
        String sql = "INSERT INTO venda_itens (venda_id, produto_id, quantidade, preco_unitario, desconto, subtotal) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement preparada = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (VendaItem item : venda.getItens()) {
                preparada.setLong(1, venda.getId());
                preparada.setLong(2, item.getProdutoId());
                preparada.setInt(3, item.getQuantidade());
                preparada.setBigDecimal(4, item.getPrecoUnitario());
                preparada.setBigDecimal(5, item.getDesconto());
                preparada.setBigDecimal(6, item.getSubtotal());
                preparada.addBatch();
            }
            preparada.executeBatch();
        }
    }

    private void inserirPagamentos(Venda venda, Connection conexao) throws SQLException {
        if (venda.getPagamentos().isEmpty()) {
            return;
        }
        String sql = "INSERT INTO venda_pagamentos (venda_id, forma_pagamento, valor) VALUES (?, ?, ?)";
        try (PreparedStatement preparada = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (VendaPagamento pagamento : venda.getPagamentos()) {
                preparada.setLong(1, venda.getId());
                preparada.setString(2, pagamento.getFormaPagamento().name());
                preparada.setBigDecimal(3, pagamento.getValor());
                preparada.addBatch();
            }
            preparada.executeBatch();
        }
    }

    private void carregarPagamentos(Venda venda, Connection conexao) throws SQLException {
        String sql = "SELECT id, forma_pagamento, valor FROM venda_pagamentos WHERE venda_id = ? ORDER BY id";
        try (PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setLong(1, venda.getId());
            try (ResultSet resultado = preparada.executeQuery()) {
                while (resultado.next()) {
                    VendaPagamento pagamento = new VendaPagamento(
                            FormaPagamento.valueOf(resultado.getString("forma_pagamento")),
                            resultado.getBigDecimal("valor"));
                    pagamento.setId(resultado.getLong("id"));
                    pagamento.setVendaId(venda.getId());
                    venda.getPagamentos().add(pagamento);
                }
            }
        }
    }

    private void baixarEstoque(Venda venda, Connection conexao) throws SQLException {
        if (venda.getStatus() != StatusVenda.CONCLUIDA) {
            return;
        }
        ProdutoRepository produtos = new ProdutoRepository(() -> conexao);
        for (VendaItem item : venda.getItens()) {
            boolean alterado = produtos.alterarEstoque(item.getProdutoId(), -item.getQuantidade(), conexao);
            if (!alterado) {
                throw new SQLException("Estoque insuficiente para o produto " + item.getProdutoNome());
            }
        }
    }

    public void devolverEstoque(Venda venda, Connection conexao) throws SQLException {
        ProdutoRepository produtos = new ProdutoRepository(() -> conexao);
        for (VendaItem item : venda.getItens()) {
            produtos.alterarEstoque(item.getProdutoId(), item.getQuantidade(), conexao);
        }
    }

    public void cancelar(long vendaId) {
        try (Connection conexao = fonte.getConnection()) {
            try {
                conexao.setAutoCommit(false);
                Venda venda = buscarPorId(vendaId, conexao)
                        .orElseThrow(() -> new RepositorioException("Venda não encontrada", null));
                if (venda.getStatus() == StatusVenda.CANCELADA) {
                    conexao.rollback();
                    return;
                }
                devolverEstoque(venda, conexao);
                try (PreparedStatement preparada =
                             conexao.prepareStatement("UPDATE vendas SET status = ? WHERE id = ?")) {
                    preparada.setString(1, StatusVenda.CANCELADA.name());
                    preparada.setLong(2, vendaId);
                    preparada.executeUpdate();
                }
                conexao.commit();
            } catch (SQLException | RepositorioException e) {
                conexao.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao cancelar a venda", e);
        }
    }

    public Optional<Venda> buscarPorNumero(String numero) {
        String sql = "SELECT id FROM vendas WHERE numero = ?";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setString(1, numero);
            try (ResultSet resultado = preparada.executeQuery()) {
                if (resultado.next()) {
                    return buscarPorId(resultado.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao buscar a venda pelo número", e);
        }
        return Optional.empty();
    }

    public Optional<Venda> ultimaDoUsuario(long usuarioId) {
        String sql = "SELECT MAX(id) FROM vendas WHERE usuario_id = ?";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setLong(1, usuarioId);
            try (ResultSet resultado = preparada.executeQuery()) {
                if (resultado.next()) {
                    long id = resultado.getLong(1);
                    if (!resultado.wasNull()) {
                        return buscarPorId(id);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao buscar a última venda", e);
        }
        return Optional.empty();
    }

    public Optional<Venda> buscarPorId(long id) {
        try (Connection conexao = fonte.getConnection()) {
            return buscarPorId(id, conexao);
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao buscar venda", e);
        }
    }

    private Optional<Venda> buscarPorId(long id, Connection conexao) throws SQLException {
        String sql = SELECT_BASE + " WHERE v.id = ?";
        try (PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setLong(1, id);
            try (ResultSet resultado = preparada.executeQuery()) {
                if (resultado.next()) {
                    Venda venda = mapear(resultado);
                    carregarItens(venda, conexao);
                    carregarPagamentos(venda, conexao);
                    return Optional.of(venda);
                }
            }
        }
        return Optional.empty();
    }

    private void carregarItens(Venda venda, Connection conexao) throws SQLException {
        String sql = "SELECT i.id, i.produto_id, p.nome AS produto_nome, i.quantidade, i.preco_unitario, "
                + "i.desconto, i.subtotal "
                + "FROM venda_itens i JOIN produtos p ON p.id = i.produto_id WHERE i.venda_id = ?";
        try (PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setLong(1, venda.getId());
            try (ResultSet resultado = preparada.executeQuery()) {
                while (resultado.next()) {
                    VendaItem item = new VendaItem(
                            resultado.getLong("produto_id"),
                            resultado.getString("produto_nome"),
                            resultado.getInt("quantidade"),
                            resultado.getBigDecimal("preco_unitario"));
                    item.setId(resultado.getLong("id"));
                    item.setDesconto(resultado.getBigDecimal("desconto"));
                    item.setSubtotal(resultado.getBigDecimal("subtotal"));
                    venda.getItens().add(item);
                }
            }
        }
    }

    public List<Venda> listar(LocalDate inicio, LocalDate fim) {
        String sql = SELECT_BASE + " WHERE v.data_hora >= ? AND v.data_hora < ? ORDER BY v.data_hora DESC";
        List<Venda> vendas = new ArrayList<>();
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setObject(1, inicio);
            preparada.setObject(2, fim.plusDays(1));
            try (ResultSet resultado = preparada.executeQuery()) {
                while (resultado.next()) {
                    vendas.add(mapear(resultado));
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao listar vendas", e);
        }
        return vendas;
    }

    public String proximoNumero() {
        String sql = "SELECT COUNT(*) FROM vendas";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql);
             ResultSet resultado = preparada.executeQuery()) {
            long total = resultado.next() ? resultado.getLong(1) : 0L;
            return String.format("V%06d", total + 1);
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao gerar número da venda", e);
        }
    }

    private Venda mapear(ResultSet resultado) throws SQLException {
        Venda venda = new Venda();
        venda.setId(resultado.getLong("id"));
        venda.setNumero(resultado.getString("numero"));
        venda.setDataHora(resultado.getString("data_hora"));
        long clienteId = resultado.getLong("cliente_id");
        if (!resultado.wasNull()) {
            venda.setClienteId(clienteId);
            venda.setClienteNome(resultado.getString("cliente_nome"));
        }
        long usuarioId = resultado.getLong("usuario_id");
        if (!resultado.wasNull()) {
            venda.setUsuarioId(usuarioId);
            venda.setUsuarioNome(resultado.getString("usuario_nome"));
        }
        venda.setFormaPagamento(FormaPagamento.valueOf(resultado.getString("forma_pagamento")));
        venda.setDesconto(resultado.getBigDecimal("desconto"));
        venda.setTotal(resultado.getBigDecimal("total"));
        venda.setValorPago(resultado.getBigDecimal("valor_pago"));
        venda.setTroco(resultado.getBigDecimal("troco"));
        venda.setStatus(StatusVenda.valueOf(resultado.getString("status")));
        return venda;
    }

    public BigDecimal totalFaturado(LocalDate inicio, LocalDate fim) {
        String sql = "SELECT COALESCE(SUM(total), 0) FROM vendas WHERE status = 'CONCLUIDA' "
                + "AND data_hora >= ? AND data_hora < ?";
        return consultarValor(sql, inicio, fim);
    }

    public BigDecimal totalEmDinheiroApos(String dataHoraInicio) {
        return totaisPorFormaApos(dataHoraInicio).getOrDefault(FormaPagamento.DINHEIRO, BigDecimal.ZERO);
    }

    public Map<FormaPagamento, BigDecimal> totaisPorFormaApos(String dataHoraInicio) {
        String sql = "SELECT forma, SUM(valor) FROM ("
                + "SELECT v.forma_pagamento AS forma, v.total AS valor FROM vendas v "
                + "WHERE v.status = 'CONCLUIDA' AND v.data_hora >= ? "
                + "AND NOT EXISTS (SELECT 1 FROM venda_pagamentos p WHERE p.venda_id = v.id) "
                + "UNION ALL "
                + "SELECT p.forma_pagamento, p.valor FROM venda_pagamentos p "
                + "JOIN vendas v ON v.id = p.venda_id "
                + "WHERE v.status = 'CONCLUIDA' AND v.data_hora >= ?"
                + ") pagamentos GROUP BY forma";
        Map<FormaPagamento, BigDecimal> totais = new EnumMap<>(FormaPagamento.class);
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            java.sql.Timestamp inicio = java.sql.Timestamp.valueOf(dataHoraInicio.replace('T', ' '));
            preparada.setObject(1, inicio);
            preparada.setObject(2, inicio);
            try (ResultSet resultado = preparada.executeQuery()) {
                while (resultado.next()) {
                    totais.put(FormaPagamento.valueOf(resultado.getString(1)), resultado.getBigDecimal(2));
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao somar vendas por forma de pagamento", e);
        }
        return totais;
    }

    public int quantidadeApos(String dataHoraInicio) {
        String sql = "SELECT COUNT(*) FROM vendas WHERE status = 'CONCLUIDA' AND data_hora >= ?";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setObject(1, java.sql.Timestamp.valueOf(dataHoraInicio.replace('T', ' ')));
            try (ResultSet resultado = preparada.executeQuery()) {
                return resultado.next() ? resultado.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao contar vendas do turno", e);
        }
    }

    public int quantidadeVendas(LocalDate inicio, LocalDate fim) {
        String sql = "SELECT COUNT(*) FROM vendas WHERE status = 'CONCLUIDA' AND data_hora >= ? AND data_hora < ?";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setObject(1, inicio);
            preparada.setObject(2, fim.plusDays(1));
            try (ResultSet resultado = preparada.executeQuery()) {
                return resultado.next() ? resultado.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao contar vendas", e);
        }
    }

    private BigDecimal consultarValor(String sql, LocalDate inicio, LocalDate fim) {
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setObject(1, inicio);
            preparada.setObject(2, fim.plusDays(1));
            try (ResultSet resultado = preparada.executeQuery()) {
                return resultado.next() ? resultado.getBigDecimal(1) : BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao consultar totais", e);
        }
    }
}
