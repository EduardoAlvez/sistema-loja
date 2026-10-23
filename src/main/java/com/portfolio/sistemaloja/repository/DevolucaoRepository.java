package com.portfolio.sistemaloja.repository;

import com.portfolio.sistemaloja.db.ConexaoFonte;
import com.portfolio.sistemaloja.model.Devolucao;
import com.portfolio.sistemaloja.model.DevolucaoItem;
import com.portfolio.sistemaloja.model.StatusVenda;
import com.portfolio.sistemaloja.service.FidelidadeService;
import com.portfolio.sistemaloja.service.ValidacaoException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DevolucaoRepository {

    private final ConexaoFonte fonte;
    private final FidelidadeRepository fidelidade;
    private final VendaRepository vendas;
    private final FidelidadeService calculadoraEstorno;
    private Consumer<Connection> aposBloqueio = conexao -> {
    };

    public DevolucaoRepository(ConexaoFonte fonte, FidelidadeService calculadoraEstorno) {
        this.fonte = fonte;
        this.fidelidade = new FidelidadeRepository(fonte);
        this.vendas = new VendaRepository(fonte);
        this.calculadoraEstorno = calculadoraEstorno;
    }

    public void aposBloqueioParaTeste(Consumer<Connection> aposBloqueio) {
        this.aposBloqueio = aposBloqueio;
    }

    public Devolucao salvar(Devolucao devolucao) {
        int pontosEstornados = 0;
        try (Connection conexao = fonte.getConnection()) {
            try {
                conexao.setAutoCommit(false);
                StatusVenda status = vendas.bloquearVenda(devolucao.getVendaId(), conexao);
                if (status != StatusVenda.CONCLUIDA) {
                    throw new ValidacaoException("Somente vendas concluídas podem ser devolvidas.");
                }
                aposBloqueio.accept(conexao);
                validarQuantidades(devolucao, conexao);
                int pontos = calculadoraEstorno.calcularEstornoPendente(
                        devolucao.getVendaId(), devolucao.getValor(), conexao);
                inserirDevolucao(devolucao, conexao);
                for (DevolucaoItem item : devolucao.getItens()) {
                    inserirItem(devolucao.getId(), item, conexao);
                    devolverEstoque(item, conexao);
                }
                if (devolucao.isCreditoCliente()) {
                    creditarCliente(devolucao, conexao);
                }
                if (pontos > 0 && devolucao.getClienteId() != null) {
                    pontosEstornados = fidelidade.estornarCredito(conexao, devolucao.getClienteId(),
                            devolucao.getVendaId(), devolucao.getId(), pontos,
                            "Estorno de pontos da devolucao " + devolucao.getId());
                    registrarPontosEstornados(devolucao.getId(), pontosEstornados, conexao);
                }
                conexao.commit();
            } catch (SQLException e) {
                rollbackSilencioso(conexao, e);
                throw e;
            } catch (RuntimeException e) {
                rollbackSilencioso(conexao, e);
                throw e;
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao registrar a devolução", e);
        }
        return buscarPorId(devolucao.getId());
    }

    private void rollbackSilencioso(Connection conexao, Throwable causa) {
        try {
            conexao.rollback();
        } catch (SQLException falhaRollback) {
            causa.addSuppressed(falhaRollback);
        }
    }

    private void validarQuantidades(Devolucao devolucao, Connection conexao) throws SQLException {
        for (DevolucaoItem item : devolucao.getItens()) {
            if (item.getQuantidade() <= 0) {
                throw new ValidacaoException("Quantidade inválida para o item " + item.getProdutoNome() + ".");
            }
            Integer disponivel = unidadesDisponiveis(devolucao.getVendaId(), item.getVendaItemId(), conexao);
            if (disponivel == null) {
                throw new ValidacaoException("Item informado não pertence à venda.");
            }
            if (item.getQuantidade() > disponivel) {
                throw new ValidacaoException("Item " + item.getProdutoNome() + ": restam "
                        + Math.max(disponivel, 0) + " unidade(s) para devolver.");
            }
        }
    }

    private Integer unidadesDisponiveis(long vendaId, long vendaItemId, Connection conexao) throws SQLException {
        String sql = "SELECT i.quantidade - COALESCE(d.total, 0) AS disponivel "
                + "FROM venda_itens i "
                + "LEFT JOIN (SELECT venda_item_id, SUM(quantidade) AS total FROM devolucao_itens "
                + "GROUP BY venda_item_id) d ON d.venda_item_id = i.id "
                + "WHERE i.id = ? AND i.venda_id = ?";
        try (PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setLong(1, vendaItemId);
            preparada.setLong(2, vendaId);
            try (ResultSet resultado = preparada.executeQuery()) {
                return resultado.next() ? resultado.getInt("disponivel") : null;
            }
        }
    }

    public Devolucao buscarPorId(long id) {
        String sql = "SELECT d.id, d.venda_id, v.numero AS venda_numero, d.cliente_id, cl.nome AS cliente_nome, "
                + "d.usuario_id, us.nome AS usuario_nome, d.data_hora, d.valor, d.credito_cliente, d.observacao, "
                + "d.pontos_estornados "
                + "FROM devolucoes d "
                + "LEFT JOIN vendas v ON v.id = d.venda_id "
                + "LEFT JOIN clientes cl ON cl.id = d.cliente_id "
                + "LEFT JOIN usuarios us ON us.id = d.usuario_id "
                + "WHERE d.id = ?";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setLong(1, id);
            try (ResultSet resultado = preparada.executeQuery()) {
                if (!resultado.next()) {
                    throw new RepositorioException("Devolução não encontrada: " + id, null);
                }
                Devolucao devolucao = mapear(resultado);
                devolucao.getItens().addAll(carregarItens(devolucao.getId(), conexao));
                return devolucao;
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao buscar a devolução", e);
        }
    }

    public List<Devolucao> listarPorVenda(long vendaId) {
        String sql = "SELECT d.id, d.venda_id, v.numero AS venda_numero, d.cliente_id, cl.nome AS cliente_nome, "
                + "d.usuario_id, us.nome AS usuario_nome, d.data_hora, d.valor, d.credito_cliente, d.observacao, "
                + "d.pontos_estornados "
                + "FROM devolucoes d "
                + "LEFT JOIN vendas v ON v.id = d.venda_id "
                + "LEFT JOIN clientes cl ON cl.id = d.cliente_id "
                + "LEFT JOIN usuarios us ON us.id = d.usuario_id "
                + "WHERE d.venda_id = ? ORDER BY d.id";
        List<Devolucao> devolucoes = new ArrayList<>();
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setLong(1, vendaId);
            try (ResultSet resultado = preparada.executeQuery()) {
                while (resultado.next()) {
                    Devolucao devolucao = mapear(resultado);
                    devolucao.getItens().addAll(carregarItens(devolucao.getId(), conexao));
                    devolucoes.add(devolucao);
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao listar as devoluções da venda", e);
        }
        return devolucoes;
    }

    public int quantidadeDevolvida(long vendaItemId) {
        String sql = "SELECT COALESCE(SUM(quantidade), 0) FROM devolucao_itens WHERE venda_item_id = ?";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setLong(1, vendaItemId);
            try (ResultSet resultado = preparada.executeQuery()) {
                return resultado.next() ? resultado.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao somar as devoluções do item", e);
        }
    }

    private void inserirDevolucao(Devolucao devolucao, Connection conexao) throws SQLException {
        String sql = "INSERT INTO devolucoes (venda_id, cliente_id, usuario_id, valor, credito_cliente, "
                + "observacao) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement preparada = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparada.setLong(1, devolucao.getVendaId());
            if (devolucao.getClienteId() == null) {
                preparada.setObject(2, null);
            } else {
                preparada.setLong(2, devolucao.getClienteId());
            }
            if (devolucao.getUsuarioId() == null) {
                preparada.setObject(3, null);
            } else {
                preparada.setLong(3, devolucao.getUsuarioId());
            }
            preparada.setBigDecimal(4, devolucao.getValor());
            preparada.setBoolean(5, devolucao.isCreditoCliente());
            preparada.setString(6, devolucao.getObservacao());
            preparada.executeUpdate();
            try (ResultSet chaves = preparada.getGeneratedKeys()) {
                if (chaves.next()) {
                    devolucao.setId(chaves.getLong(1));
                }
            }
        }
    }

    private void inserirItem(long devolucaoId, DevolucaoItem item, Connection conexao) throws SQLException {
        String sql = "INSERT INTO devolucao_itens (devolucao_id, venda_item_id, produto_id, produto_nome, "
                + "quantidade, valor_unitario) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement preparada = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparada.setLong(1, devolucaoId);
            preparada.setLong(2, item.getVendaItemId());
            preparada.setLong(3, item.getProdutoId());
            preparada.setString(4, item.getProdutoNome());
            preparada.setInt(5, item.getQuantidade());
            preparada.setBigDecimal(6, item.getValorUnitario());
            preparada.executeUpdate();
            try (ResultSet chaves = preparada.getGeneratedKeys()) {
                if (chaves.next()) {
                    item.setId(chaves.getLong(1));
                }
            }
        }
    }

    private void devolverEstoque(DevolucaoItem item, Connection conexao) throws SQLException {
        String sql = "UPDATE produtos SET estoque = estoque + ? WHERE id = ?";
        try (PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setInt(1, item.getQuantidade());
            preparada.setLong(2, item.getProdutoId());
            preparada.executeUpdate();
        }
    }

    private void registrarPontosEstornados(long devolucaoId, int pontosEstornados, Connection conexao)
            throws SQLException {
        String sql = "UPDATE devolucoes SET pontos_estornados = ? WHERE id = ?";
        try (PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setInt(1, pontosEstornados);
            preparada.setLong(2, devolucaoId);
            preparada.executeUpdate();
        }
    }

    private void creditarCliente(Devolucao devolucao, Connection conexao) throws SQLException {
        String sql = "UPDATE clientes SET credito = credito + ? WHERE id = ?";
        try (PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setBigDecimal(1, devolucao.getValor());
            preparada.setLong(2, devolucao.getClienteId());
            preparada.executeUpdate();
        }
    }

    private List<DevolucaoItem> carregarItens(long devolucaoId, Connection conexao) throws SQLException {
        String sql = "SELECT id, venda_item_id, produto_id, produto_nome, quantidade, valor_unitario "
                + "FROM devolucao_itens WHERE devolucao_id = ? ORDER BY id";
        List<DevolucaoItem> itens = new ArrayList<>();
        try (PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setLong(1, devolucaoId);
            try (ResultSet resultado = preparada.executeQuery()) {
                while (resultado.next()) {
                    DevolucaoItem item = new DevolucaoItem();
                    item.setId(resultado.getLong("id"));
                    item.setVendaItemId(resultado.getLong("venda_item_id"));
                    item.setProdutoId(resultado.getLong("produto_id"));
                    item.setProdutoNome(resultado.getString("produto_nome"));
                    item.setQuantidade(resultado.getInt("quantidade"));
                    item.setValorUnitario(resultado.getBigDecimal("valor_unitario"));
                    itens.add(item);
                }
            }
        }
        return itens;
    }

    private Devolucao mapear(ResultSet resultado) throws SQLException {
        Devolucao devolucao = new Devolucao();
        devolucao.setId(resultado.getLong("id"));
        devolucao.setVendaId(resultado.getLong("venda_id"));
        devolucao.setVendaNumero(resultado.getString("venda_numero"));
        long clienteId = resultado.getLong("cliente_id");
        if (!resultado.wasNull()) {
            devolucao.setClienteId(clienteId);
            devolucao.setClienteNome(resultado.getString("cliente_nome"));
        }
        long usuarioId = resultado.getLong("usuario_id");
        if (!resultado.wasNull()) {
            devolucao.setUsuarioId(usuarioId);
            devolucao.setUsuarioNome(resultado.getString("usuario_nome"));
        }
        devolucao.setDataHora(resultado.getString("data_hora"));
        devolucao.setValor(resultado.getBigDecimal("valor"));
        devolucao.setCreditoCliente(resultado.getBoolean("credito_cliente"));
        devolucao.setPontosEstornados(resultado.getInt("pontos_estornados"));
        devolucao.setObservacao(resultado.getString("observacao"));
        return devolucao;
    }
}
