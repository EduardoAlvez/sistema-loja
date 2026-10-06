package com.portfolio.sistemaloja.repository;

import com.portfolio.sistemaloja.db.ConexaoFonte;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RelatorioRepository {

    private final ConexaoFonte fonte;

    public RelatorioRepository(ConexaoFonte fonte) {
        this.fonte = fonte;
    }

    public List<ProdutoVendado> maisVendidos(LocalDate inicio, LocalDate fim, int limite) {
        String sql = """
                SELECT p.id, p.nome, SUM(i.quantidade) AS qtd, SUM(i.subtotal) AS total
                  FROM venda_itens i
                  JOIN vendas v ON v.id = i.venda_id
                  JOIN produtos p ON p.id = i.produto_id
                 WHERE v.status = 'CONCLUIDA' AND v.data_hora >= ? AND v.data_hora < ?
                 GROUP BY p.id, p.nome
                 ORDER BY qtd DESC
                 LIMIT ?""";
        List<ProdutoVendado> lista = new ArrayList<>();
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setObject(1, inicio);
            preparada.setObject(2, fim.plusDays(1));
            preparada.setInt(3, limite);
            try (ResultSet resultado = preparada.executeQuery()) {
                while (resultado.next()) {
                    lista.add(new ProdutoVendado(
                            resultado.getLong("id"),
                            resultado.getString("nome"),
                            resultado.getInt("qtd"),
                            resultado.getBigDecimal("total")));
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao gerar relatório de produtos vendidos", e);
        }
        return lista;
    }

    public List<ProdutoEstoque> estoqueBaixo(int limiteMinimo) {
        String sql = """
                SELECT id, nome, estoque, preco_venda
                  FROM produtos
                 WHERE ativo = TRUE AND estoque <= ?
                 ORDER BY estoque ASC""";
        List<ProdutoEstoque> lista = new ArrayList<>();
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setInt(1, limiteMinimo);
            try (ResultSet resultado = preparada.executeQuery()) {
                while (resultado.next()) {
                    lista.add(new ProdutoEstoque(
                            resultado.getLong("id"),
                            resultado.getString("nome"),
                            resultado.getInt("estoque"),
                            resultado.getBigDecimal("preco_venda")));
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao gerar relatório de estoque", e);
        }
        return lista;
    }

    public int contarClientesAtivos() {
        return contar("SELECT COUNT(*) FROM clientes WHERE ativo = TRUE");
    }

    public int contarProdutosAtivos() {
        return contar("SELECT COUNT(*) FROM produtos WHERE ativo = TRUE");
    }

    private int contar(String sql) {
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql);
             ResultSet resultado = preparada.executeQuery()) {
            return resultado.next() ? resultado.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao contar registros", e);
        }
    }
}
