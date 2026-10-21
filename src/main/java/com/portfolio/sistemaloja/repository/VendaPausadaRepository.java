package com.portfolio.sistemaloja.repository;

import com.portfolio.sistemaloja.db.ConexaoFonte;
import com.portfolio.sistemaloja.model.FormaPagamento;
import com.portfolio.sistemaloja.model.VendaItem;
import com.portfolio.sistemaloja.model.VendaPagamento;
import com.portfolio.sistemaloja.model.VendaPausada;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VendaPausadaRepository {

    private static final String SELECT_BASE = """
            SELECT p.id, p.data_hora, p.cliente_id, cl.nome AS cliente_nome, p.usuario_id,
                   us.nome AS usuario_nome, p.forma_pagamento, p.desconto, p.total, p.observacao,
                   p.pontos_resgatados, p.valor_pontos
              FROM vendas_pausadas p
              LEFT JOIN clientes cl ON cl.id = p.cliente_id
              LEFT JOIN usuarios us ON us.id = p.usuario_id""";

    private final ConexaoFonte fonte;

    public VendaPausadaRepository(ConexaoFonte fonte) {
        this.fonte = fonte;
    }

    public VendaPausada salvar(VendaPausada pausada) {
        try (Connection conexao = fonte.getConnection()) {
            try {
                conexao.setAutoCommit(false);
                inserir(pausada, conexao);
                inserirItens(pausada, conexao);
                inserirPagamentos(pausada, conexao);
                conexao.commit();
                return buscarPorId(pausada.getId()).orElse(pausada);
            } catch (SQLException e) {
                conexao.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao pausar a venda", e);
        }
    }

    public List<VendaPausada> listar() {
        List<VendaPausada> pausadas = new ArrayList<>();
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada =
                     conexao.prepareStatement(SELECT_BASE + " ORDER BY p.id")) {
            try (ResultSet resultado = preparada.executeQuery()) {
                while (resultado.next()) {
                    VendaPausada pausada = mapear(resultado);
                    carregarItens(pausada, conexao);
                    carregarPagamentos(pausada, conexao);
                    pausadas.add(pausada);
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao listar vendas pausadas", e);
        }
        return pausadas;
    }

    public Optional<VendaPausada> buscarPorId(long id) {
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(SELECT_BASE + " WHERE p.id = ?")) {
            preparada.setLong(1, id);
            try (ResultSet resultado = preparada.executeQuery()) {
                if (resultado.next()) {
                    VendaPausada pausada = mapear(resultado);
                    carregarItens(pausada, conexao);
                    carregarPagamentos(pausada, conexao);
                    return Optional.of(pausada);
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao buscar venda pausada", e);
        }
        return Optional.empty();
    }

    public void remover(long id) {
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada =
                     conexao.prepareStatement("DELETE FROM vendas_pausadas WHERE id = ?")) {
            preparada.setLong(1, id);
            preparada.executeUpdate();
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao remover venda pausada", e);
        }
    }

    private void inserir(VendaPausada pausada, Connection conexao) throws SQLException {
        String sql = "INSERT INTO vendas_pausadas (cliente_id, usuario_id, forma_pagamento, desconto, total, "
                + "observacao, pontos_resgatados, valor_pontos) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement preparada = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (pausada.getClienteId() == null) {
                preparada.setObject(1, null);
            } else {
                preparada.setLong(1, pausada.getClienteId());
            }
            if (pausada.getUsuarioId() == null) {
                preparada.setObject(2, null);
            } else {
                preparada.setLong(2, pausada.getUsuarioId());
            }
            if (pausada.getFormaPagamento() == null) {
                preparada.setObject(3, null);
            } else {
                preparada.setString(3, pausada.getFormaPagamento().name());
            }
            preparada.setBigDecimal(4, pausada.getDesconto());
            preparada.setBigDecimal(5, pausada.getTotal());
            preparada.setString(6, pausada.getObservacao());
            preparada.setInt(7, pausada.getPontosResgatados());
            preparada.setBigDecimal(8, pausada.getValorPontos());
            preparada.executeUpdate();
            try (ResultSet chaves = preparada.getGeneratedKeys()) {
                if (chaves.next()) {
                    pausada.setId(chaves.getLong(1));
                }
            }
        }
    }

    private void inserirItens(VendaPausada pausada, Connection conexao) throws SQLException {
        String sql = "INSERT INTO venda_pausada_itens (pausada_id, produto_id, quantidade, preco_unitario, desconto) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement preparada = conexao.prepareStatement(sql)) {
            for (VendaItem item : pausada.getItens()) {
                preparada.setLong(1, pausada.getId());
                preparada.setLong(2, item.getProdutoId());
                preparada.setInt(3, item.getQuantidade());
                preparada.setBigDecimal(4, item.getPrecoUnitario());
                preparada.setBigDecimal(5, item.getDesconto());
                preparada.addBatch();
            }
            preparada.executeBatch();
        }
    }

    private void inserirPagamentos(VendaPausada pausada, Connection conexao) throws SQLException {
        if (pausada.getPagamentos().isEmpty()) {
            return;
        }
        String sql = "INSERT INTO venda_pausada_pagamentos (pausada_id, forma_pagamento, valor) VALUES (?, ?, ?)";
        try (PreparedStatement preparada = conexao.prepareStatement(sql)) {
            for (VendaPagamento pagamento : pausada.getPagamentos()) {
                preparada.setLong(1, pausada.getId());
                preparada.setString(2, pagamento.getFormaPagamento().name());
                preparada.setBigDecimal(3, pagamento.getValor());
                preparada.addBatch();
            }
            preparada.executeBatch();
        }
    }

    private void carregarItens(VendaPausada pausada, Connection conexao) throws SQLException {
        String sql = "SELECT i.id, i.produto_id, p.nome AS produto_nome, i.quantidade, i.preco_unitario, i.desconto "
                + "FROM venda_pausada_itens i JOIN produtos p ON p.id = i.produto_id WHERE i.pausada_id = ?";
        try (PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setLong(1, pausada.getId());
            try (ResultSet resultado = preparada.executeQuery()) {
                while (resultado.next()) {
                    VendaItem item = new VendaItem(
                            resultado.getLong("produto_id"),
                            resultado.getString("produto_nome"),
                            resultado.getInt("quantidade"),
                            resultado.getBigDecimal("preco_unitario"));
                    item.setId(resultado.getLong("id"));
                    item.setDesconto(resultado.getBigDecimal("desconto"));
                    pausada.getItens().add(item);
                }
            }
        }
    }

    private void carregarPagamentos(VendaPausada pausada, Connection conexao) throws SQLException {
        String sql = "SELECT id, forma_pagamento, valor FROM venda_pausada_pagamentos WHERE pausada_id = ? ORDER BY id";
        try (PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setLong(1, pausada.getId());
            try (ResultSet resultado = preparada.executeQuery()) {
                while (resultado.next()) {
                    VendaPagamento pagamento = new VendaPagamento(
                            FormaPagamento.valueOf(resultado.getString("forma_pagamento")),
                            resultado.getBigDecimal("valor"));
                    pagamento.setId(resultado.getLong("id"));
                    pausada.getPagamentos().add(pagamento);
                }
            }
        }
    }

    private VendaPausada mapear(ResultSet resultado) throws SQLException {
        VendaPausada pausada = new VendaPausada();
        pausada.setId(resultado.getLong("id"));
        pausada.setDataHora(resultado.getString("data_hora"));
        long clienteId = resultado.getLong("cliente_id");
        if (!resultado.wasNull()) {
            pausada.setClienteId(clienteId);
            pausada.setClienteNome(resultado.getString("cliente_nome"));
        }
        long usuarioId = resultado.getLong("usuario_id");
        if (!resultado.wasNull()) {
            pausada.setUsuarioId(usuarioId);
            pausada.setUsuarioNome(resultado.getString("usuario_nome"));
        }
        BigDecimal desconto = resultado.getBigDecimal("desconto");
        pausada.setDesconto(desconto == null ? BigDecimal.ZERO : desconto);
        String forma = resultado.getString("forma_pagamento");
        if (forma != null) {
            pausada.setFormaPagamento(FormaPagamento.valueOf(forma));
        }
        pausada.setTotal(resultado.getBigDecimal("total"));
        pausada.setPontosResgatados(resultado.getInt("pontos_resgatados"));
        BigDecimal valorPontos = resultado.getBigDecimal("valor_pontos");
        pausada.setValorPontos(valorPontos == null ? BigDecimal.ZERO : valorPontos);
        pausada.setObservacao(resultado.getString("observacao"));
        return pausada;
    }
}
