package com.portfolio.sistemaloja.repository;

import com.portfolio.sistemaloja.db.ConexaoFonte;
import com.portfolio.sistemaloja.model.ContagemInventario;
import com.portfolio.sistemaloja.model.ContagemItem;
import com.portfolio.sistemaloja.model.StatusContagem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Persistência das contagens de inventário.
 *
 * <p>Todas as operações de escrita recebem a {@link Connection} da transação em curso,
 * para que a contagem, os itens e o ajuste de estoque formem uma unidade atômica.
 */
public class ContagemInventarioRepository {

    private static final String SELECT_CABECALHO = """
            SELECT c.id, c.numero, c.status, c.usuario_id, c.observacao, c.motivo_cancelamento, c.data_hora, c.data_fechamento,
                   u.nome AS usuario_nome
              FROM contagem_inventario c
              JOIN usuarios u ON u.id = c.usuario_id""";

    private final ConexaoFonte fonte;
    private Consumer<Connection> aposTravaAbertura = conexao -> {
    };
    private Consumer<Connection> aposTravaCabecalho = conexao -> {
    };
    private Consumer<Connection> falharEmAjuste = conexao -> {
    };

    public ContagemInventarioRepository(ConexaoFonte fonte) {
        this.fonte = fonte;
    }

    public void aposTravaAberturaParaTeste(Consumer<Connection> hook) {
        this.aposTravaAbertura = hook;
    }

    public void aposTravaCabecalhoParaTeste(Consumer<Connection> hook) {
        this.aposTravaCabecalho = hook;
    }

    public void falharEmAjusteParaTeste(Consumer<Connection> hook) {
        this.falharEmAjuste = hook;
    }

    public long inserirCabecalho(long usuarioId, Connection conexao) throws SQLException {
        String sql = "INSERT INTO contagem_inventario (numero, status, usuario_id) VALUES (NULL, ?, ?)";
        long id;
        try (PreparedStatement ps = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, StatusContagem.ABERTA.name());
            ps.setLong(2, usuarioId);
            ps.executeUpdate();
            try (ResultSet chaves = ps.getGeneratedKeys()) {
                if (!chaves.next()) {
                    throw new SQLException("Não foi possível obter o identificador da contagem.");
                }
                id = chaves.getLong(1);
            }
        }
        definirNumero(id, conexao);
        return id;
    }

    private void definirNumero(long contagemId, Connection conexao) throws SQLException {
        String sql = "UPDATE contagem_inventario SET numero = ? WHERE id = ?";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setString(1, "INV-" + contagemId);
            ps.setLong(2, contagemId);
            ps.executeUpdate();
        }
    }

    public void inserirItem(long contagemId, long produtoId, int estoqueSistema, Connection conexao)
            throws SQLException {
        String sql = "INSERT INTO contagem_item (contagem_id, produto_id, estoque_sistema) VALUES (?,?,?)";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setLong(1, contagemId);
            ps.setLong(2, produtoId);
            ps.setInt(3, estoqueSistema);
            ps.executeUpdate();
        }
    }

    public void travarControle(Connection conexao) throws SQLException {
        String sql = "SELECT id FROM contagem_controle WHERE id = 1 FOR UPDATE";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            try (ResultSet resultado = ps.executeQuery()) {
                if (!resultado.next()) {
                    throw new SQLException("Linha sentinela de contagem ausente (seed da V25).");
                }
            }
        }
        aposTravaAbertura.accept(conexao);
    }

    public StatusContagem travarCabecalho(long contagemId, Connection conexao) throws SQLException {
        String sql = "SELECT status FROM contagem_inventario WHERE id = ? FOR UPDATE";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setLong(1, contagemId);
            try (ResultSet resultado = ps.executeQuery()) {
                if (!resultado.next()) {
                    return null;
                }
                StatusContagem status = mapearStatus(resultado.getString("status"));
                aposTravaCabecalho.accept(conexao);
                return status;
            }
        }
    }

    public ContagemInventario buscarPorId(long contagemId, Connection conexao) throws SQLException {
        String sql = SELECT_CABECALHO + " WHERE c.id = ?";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setLong(1, contagemId);
            try (ResultSet resultado = ps.executeQuery()) {
                return resultado.next() ? mapearCabecalho(resultado) : null;
            }
        }
    }

    public ContagemInventario buscarAberta(Connection conexao) throws SQLException {
        String sql = SELECT_CABECALHO + " WHERE c.status = ? ORDER BY c.data_hora DESC, c.id DESC LIMIT 1";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setString(1, StatusContagem.ABERTA.name());
            try (ResultSet resultado = ps.executeQuery()) {
                return resultado.next() ? mapearCabecalho(resultado) : null;
            }
        }
    }

    public List<ContagemItem> listarItens(long contagemId, Connection conexao) throws SQLException {
        return listarItens(contagemId, conexao, "ORDER BY p.nome, p.id");
    }

    /**
     * Itens para o fechamento, sempre na ordem de {@code produto_id}: é a ordem em que as
     * travas dos produtos serão adquiridas, e fixá-la reduz o risco de deadlock com vendas
     * concorrentes.
     */
    public List<ContagemItem> listarItensParaAjuste(long contagemId, Connection conexao) throws SQLException {
        return listarItens(contagemId, conexao, "ORDER BY ci.produto_id");
    }

    private List<ContagemItem> listarItens(long contagemId, Connection conexao, String ordenacao)
            throws SQLException {
        String sql = """
                SELECT ci.id, ci.contagem_id, ci.produto_id, ci.estoque_sistema, ci.quantidade_contada,
                       ci.delta_aplicado, ci.ajustado,
                       p.nome AS produto_nome, p.codigo_barras AS produto_codigo_barras,
                       p.estoque AS estoque_atual
                  FROM contagem_item ci
                  JOIN produtos p ON p.id = ci.produto_id
                 WHERE ci.contagem_id = ?
                """ + ordenacao;
        List<ContagemItem> itens = new ArrayList<>();
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setLong(1, contagemId);
            try (ResultSet resultado = ps.executeQuery()) {
                while (resultado.next()) {
                    itens.add(mapearItem(resultado));
                }
            }
        }
        return itens;
    }

    public void registrarQuantidadeContada(long itemId, int quantidade, Connection conexao) throws SQLException {
        String sql = """
                UPDATE contagem_item ci
                   SET quantidade_contada = ?
                 WHERE ci.id = ?
                   AND EXISTS (SELECT 1 FROM contagem_inventario c
                                WHERE c.id = ci.contagem_id AND c.status = 'ABERTA')""";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setInt(1, quantidade);
            ps.setLong(2, itemId);
            if (ps.executeUpdate() == 0) {
                throw new SQLException("O item informado não pertence a uma contagem aberta.");
            }
        }
    }

    public Long buscarItemId(long contagemId, long produtoId, Connection conexao) throws SQLException {
        String sql = "SELECT id FROM contagem_item WHERE contagem_id = ? AND produto_id = ?";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setLong(1, contagemId);
            ps.setLong(2, produtoId);
            try (ResultSet resultado = ps.executeQuery()) {
                return resultado.next() ? resultado.getLong("id") : null;
            }
        }
    }

    public void registrarAjuste(long itemId, int deltaAplicado, Connection conexao) throws SQLException {
        falharEmAjuste.accept(conexao);
        String sql = "UPDATE contagem_item SET delta_aplicado = ?, ajustado = TRUE WHERE id = ?";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setInt(1, deltaAplicado);
            ps.setLong(2, itemId);
            int linhas = ps.executeUpdate();
            if (linhas == 0) {
                throw new SQLException("Item da contagem não encontrado para registrar ajuste.");
            }
        }
    }

    public boolean fecharSeAberta(long contagemId, String observacao, Connection conexao) throws SQLException {
        String sql = "UPDATE contagem_inventario SET status = ?, observacao = ?, data_fechamento = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND status = ?";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setString(1, StatusContagem.FECHADA.name());
            ps.setString(2, observacao);
            ps.setLong(3, contagemId);
            ps.setString(4, StatusContagem.ABERTA.name());
            return ps.executeUpdate() == 1;
        }
    }

    public boolean cancelarSeAberta(long contagemId, String motivo, Connection conexao) throws SQLException {
        String sql = "UPDATE contagem_inventario SET status = ?, motivo_cancelamento = ?, data_fechamento = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND status = ?";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setString(1, StatusContagem.CANCELADA.name());
            ps.setString(2, motivo);
            ps.setLong(3, contagemId);
            ps.setString(4, StatusContagem.ABERTA.name());
            return ps.executeUpdate() == 1;
        }
    }

    public List<ContagemInventario> listarTodas() {
        String sql = SELECT_CABECALHO + " ORDER BY c.data_hora DESC, c.id DESC";
        List<ContagemInventario> contagens = new ArrayList<>();
        try (Connection conexao = fonte.getConnection();
             PreparedStatement ps = conexao.prepareStatement(sql);
             ResultSet resultado = ps.executeQuery()) {
            while (resultado.next()) {
                contagens.add(mapearCabecalho(resultado));
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao listar contagens de inventário", e);
        }
        return contagens;
    }

    private static StatusContagem mapearStatus(String nome) {
        StatusContagem status = StatusContagem.porNome(nome);
        if (status == null) {
            throw new RepositorioException("Status de contagem desconhecido: '" + nome + "'.");
        }
        return status;
    }

    private ContagemInventario mapearCabecalho(ResultSet resultado) throws SQLException {
        ContagemInventario contagem = new ContagemInventario();
        contagem.setId(resultado.getLong("id"));
        contagem.setNumero(resultado.getString("numero"));
        contagem.setStatus(mapearStatus(resultado.getString("status")));
        contagem.setUsuarioNome(resultado.getString("usuario_nome"));
        long usuarioId = resultado.getLong("usuario_id");
        contagem.setUsuarioId(resultado.wasNull() ? null : usuarioId);
        contagem.setObservacao(resultado.getString("observacao"));
        contagem.setMotivoCancelamento(resultado.getString("motivo_cancelamento"));
        Timestamp dataHora = resultado.getTimestamp("data_hora");
        contagem.setDataHora(dataHora == null ? null : dataHora.toLocalDateTime());
        Timestamp dataFechamento = resultado.getTimestamp("data_fechamento");
        contagem.setDataFechamento(dataFechamento == null ? null : dataFechamento.toLocalDateTime());
        return contagem;
    }

    private ContagemItem mapearItem(ResultSet resultado) throws SQLException {
        ContagemItem item = new ContagemItem();
        item.setId(resultado.getLong("id"));
        item.setContagemId(resultado.getLong("contagem_id"));
        item.setProdutoId(resultado.getLong("produto_id"));
        item.setProdutoNome(resultado.getString("produto_nome"));
        item.setProdutoCodigoBarras(resultado.getString("produto_codigo_barras"));
        item.setEstoqueSistema(resultado.getInt("estoque_sistema"));
        item.setEstoqueAtual(resultado.getInt("estoque_atual"));
        int quantidadeContada = resultado.getInt("quantidade_contada");
        item.setQuantidadeContada(resultado.wasNull() ? null : quantidadeContada);
        int deltaAplicado = resultado.getInt("delta_aplicado");
        item.setDeltaAplicado(resultado.wasNull() ? null : deltaAplicado);
        item.setAjustado(resultado.getBoolean("ajustado"));
        return item;
    }
}
