package com.portfolio.sistemaloja.repository;

import com.portfolio.sistemaloja.db.ConexaoFonte;
import com.portfolio.sistemaloja.model.ContagemInventario;
import com.portfolio.sistemaloja.model.ContagemItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ContagemInventarioRepository {

    private final ConexaoFonte fonte;

    public ContagemInventarioRepository(ConexaoFonte fonte) {
        this.fonte = fonte;
    }

    public ContagemInventario criar(String numero, long usuarioId) {
        String sql = "INSERT INTO contagem_inventario (numero, status, usuario_id, iniciado_em) VALUES (?, 'ABERTA', ?, ?)";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement ps = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, numero);
            ps.setLong(2, usuarioId);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            ContagemInventario c = new ContagemInventario();
            c.setNumero(numero);
            c.setStatus(ContagemInventario.Status.ABERTA);
            c.setUsuarioId(usuarioId);
            c.setIniciadoEm(LocalDateTime.now());
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) c.setId(rs.getLong(1));
            }
            return c;
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao criar contagem", e);
        }
    }

    public void adicionarItem(long contagemId, ContagemItem item) {
        String sql = "INSERT INTO contagem_item (contagem_id, produto_id, estoque_sistema, quantidade_contada, diferenca, ajustado) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement ps = conexao.prepareStatement(sql)) {
            item.calcularDiferenca();
            ps.setLong(1, contagemId);
            ps.setLong(2, item.getProdutoId());
            ps.setInt(3, item.getEstoqueSistema());
            ps.setInt(4, item.getQuantidadeContada());
            ps.setInt(5, item.getDiferenca());
            ps.setBoolean(6, item.isAjustado());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao adicionar item à contagem", e);
        }
    }

    public void lancarContagem(long itemId, int quantidadeContada) {
        String sql = "UPDATE contagem_item SET quantidade_contada = ?, diferenca = (quantidade_contada - estoque_sistema), ajustado = FALSE WHERE id = ?";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setInt(1, quantidadeContada);
            ps.setLong(2, itemId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao lançar contagem", e);
        }
    }

    public List<ContagemItem> listarItens(long contagemId) {
        String sql = "SELECT id, contagem_id, produto_id, estoque_sistema, quantidade_contada, diferenca, ajustado FROM contagem_item WHERE contagem_id = ?";
        List<ContagemItem> lista = new ArrayList<>();
        try (Connection conexao = fonte.getConnection();
             PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setLong(1, contagemId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ContagemItem item = new ContagemItem();
                    item.setId(rs.getLong("id"));
                    item.setContagemId(rs.getLong("contagem_id"));
                    item.setProdutoId(rs.getLong("produto_id"));
                    item.setEstoqueSistema(rs.getInt("estoque_sistema"));
                    item.setQuantidadeContada(rs.getInt("quantidade_contada"));
                    item.setDiferenca(rs.getInt("diferenca"));
                    item.setAjustado(rs.getBoolean("ajustado"));
                    lista.add(item);
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao listar itens da contagem", e);
        }
        return lista;
    }

    public Optional<ContagemInventario> buscarAberta() {
        String sql = "SELECT id, numero, status, usuario_id, iniciado_em, fechado_em, observacao FROM contagem_inventario WHERE status = 'ABERTA' ORDER BY id DESC LIMIT 1";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement ps = conexao.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                ContagemInventario c = new ContagemInventario();
                c.setId(rs.getLong("id"));
                c.setNumero(rs.getString("numero"));
                c.setStatus(ContagemInventario.Status.valueOf(rs.getString("status")));
                c.setUsuarioId(rs.getLong("usuario_id"));
                Timestamp ini = rs.getTimestamp("iniciado_em");
                c.setIniciadoEm(ini == null ? null : ini.toLocalDateTime());
                Timestamp fim = rs.getTimestamp("fechado_em");
                c.setFechadoEm(fim == null ? null : fim.toLocalDateTime());
                c.setObservacao(rs.getString("observacao"));
                return Optional.of(c);
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao buscar contagem aberta", e);
        }
        return Optional.empty();
    }

    public void fechar(long contagemId) {
        String sql = "UPDATE contagem_inventario SET status = 'FECHADA', fechado_em = ? WHERE id = ? AND status = 'ABERTA'";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(2, contagemId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao fechar contagem", e);
        }
    }
}
