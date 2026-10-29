package com.portfolio.sistemaloja.repository;

import com.portfolio.sistemaloja.db.ConexaoFonte;
import com.portfolio.sistemaloja.model.EntradaItem;
import com.portfolio.sistemaloja.model.EntradaMercadoria;
import com.portfolio.sistemaloja.model.Produto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositório para entrada de mercadoria.
 * <p>
 * Realiza a operação em transação: registra entrada, itens, atualiza estoque
 * e recalcula o <strong>custo médio ponderado</strong>:
 * <pre>custoMedioNovo = (estoqueAtual * custoMedioAtual + qtdEntrada * valorUnitario) / (estoqueAtual + qtdEntrada)</pre>
 */
public class EntradaMercadoriaRepository {

    private final ConexaoFonte fonte;
    private final ProdutoRepository produtos;

    public EntradaMercadoriaRepository(ConexaoFonte fonte, ProdutoRepository produtos) {
        this.fonte = fonte;
        this.produtos = produtos;
    }

    public List<EntradaMercadoria> listar(LocalDate inicio, LocalDate fim) {
        String sql = """
                SELECT e.id, e.fornecedor_id, f.nome AS fornecedor_nome, e.numero_nota, e.data_entrada,
                       e.usuario_id, u.nome AS usuario_nome, e.valor_total, e.observacao, e.criado_em
                  FROM entrada_mercadoria e
                  LEFT JOIN fornecedores f ON f.id = e.fornecedor_id
                  LEFT JOIN usuarios u ON u.id = e.usuario_id
                 WHERE e.data_entrada >= ? AND e.data_entrada < ?
                 ORDER BY e.data_entrada DESC, e.id DESC""";
        List<EntradaMercadoria> lista = new ArrayList<>();
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setObject(1, inicio.atStartOfDay());
            preparada.setObject(2, fim.plusDays(1).atStartOfDay());
            try (ResultSet resultado = preparada.executeQuery()) {
                while (resultado.next()) {
                    lista.add(mapearCabecalho(resultado));
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao listar entradas de mercadoria", e);
        }
        return lista;
    }

    public EntradaMercadoria buscarPorId(long id) {
        String sql = """
                SELECT e.id, e.fornecedor_id, f.nome AS fornecedor_nome, e.numero_nota, e.data_entrada,
                       e.usuario_id, u.nome AS usuario_nome, e.valor_total, e.observacao, e.criado_em
                  FROM entrada_mercadoria e
                  LEFT JOIN fornecedores f ON f.id = e.fornecedor_id
                  LEFT JOIN usuarios u ON u.id = e.usuario_id
                 WHERE e.id = ?""";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setLong(1, id);
            try (ResultSet resultado = preparada.executeQuery()) {
                if (!resultado.next()) {
                    return null;
                }
                EntradaMercadoria entrada = mapearCabecalho(resultado);
                entrada.getItens().addAll(listarItens(conexao, entrada.getId()));
                return entrada;
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao buscar entrada de mercadoria", e);
        }
    }

    public EntradaMercadoria registrar(EntradaMercadoria entrada) {
        if (entrada == null) {
            throw new RepositorioException("Entrada de mercadoria inválida.");
        }
        entrada.recalcularTotal();
        if (entrada.getDataEntrada() == null) {
            entrada.setDataEntrada(LocalDateTime.now());
        }
        try (Connection conexao = fonte.getConnection()) {
            conexao.setAutoCommit(false);
            try {
                long entradaId = inserirCabecalho(conexao, entrada);
                entrada.setId(entradaId);
                for (EntradaItem item : entrada.getItens()) {
                    if (item.getQuantidade() <= 0) {
                        throw new RepositorioException("Quantidade deve ser maior que zero.");
                    }
                    if (item.getValorUnitario() == null || item.getValorUnitario().signum() < 0) {
                        throw new RepositorioException("Valor unitário inválido.");
                    }
                    if (item.getProdutoId() == null) {
                        throw new RepositorioException("Produto inválido na entrada.");
                    }
                    item.recalcularSubtotal();
                    inserirItem(conexao, entradaId, item);
                    Produto produto = produtos.buscarPorId(item.getProdutoId(), conexao);
                    if (produto == null) {
                        throw new RepositorioException("Produto não encontrado.");
                    }
                    int estoqueAtual = produto.getEstoque();
                    BigDecimal custoAtual = produto.getPrecoCusto() == null ? BigDecimal.ZERO : produto.getPrecoCusto();
                    BigDecimal novoCusto = calcularCustoMedioPonderado(estoqueAtual, custoAtual,
                            item.getQuantidade(), item.getValorUnitario());
                    boolean estoqueAlterado = produtos.alterarEstoque(item.getProdutoId(), item.getQuantidade(), conexao);
                    if (!estoqueAlterado) {
                        throw new RepositorioException("Falha ao atualizar estoque do produto.");
                    }
                    boolean custoAlterado = produtos.atualizarCustoMedio(item.getProdutoId(), novoCusto, conexao);
                    if (!custoAlterado) {
                        throw new RepositorioException("Falha ao atualizar custo médio do produto.");
                    }
                }
                conexao.commit();
                return entrada;
            } catch (SQLException e) {
                conexao.rollback();
                throw new RepositorioException("Falha ao registrar entrada de mercadoria", e);
            } finally {
                conexao.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao registrar entrada de mercadoria", e);
        }
    }

    private long inserirCabecalho(Connection conexao, EntradaMercadoria entrada) throws SQLException {
        String sql = """
                INSERT INTO entrada_mercadoria (fornecedor_id, numero_nota, data_entrada, usuario_id, valor_total, observacao)
                VALUES (?, ?, ?, ?, ?, ?)""";
        try (PreparedStatement preparada = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (entrada.getFornecedorId() == null) {
                preparada.setNull(1, Types.BIGINT);
            } else {
                preparada.setLong(1, entrada.getFornecedorId());
            }
            if (entrada.getNumeroNota() == null || entrada.getNumeroNota().isBlank()) {
                preparada.setNull(2, Types.VARCHAR);
            } else {
                preparada.setString(2, entrada.getNumeroNota().strip());
            }
            preparada.setObject(3, entrada.getDataEntrada());
            if (entrada.getUsuarioId() == null) {
                preparada.setNull(4, Types.BIGINT);
            } else {
                preparada.setLong(4, entrada.getUsuarioId());
            }
            preparada.setBigDecimal(5, entrada.getValorTotal());
            if (entrada.getObservacao() == null || entrada.getObservacao().isBlank()) {
                preparada.setNull(6, Types.VARCHAR);
            } else {
                preparada.setString(6, entrada.getObservacao().strip());
            }
            preparada.executeUpdate();
            try (ResultSet chaves = preparada.getGeneratedKeys()) {
                if (chaves.next()) {
                    return chaves.getLong(1);
                }
            }
        }
        throw new SQLException("Não foi possível gerar o ID da entrada de mercadoria");
    }

    private void inserirItem(Connection conexao, long entradaId, EntradaItem item) throws SQLException {
        String sql = """
                INSERT INTO entrada_item (entrada_id, produto_id, quantidade, valor_unitario, subtotal)
                VALUES (?, ?, ?, ?, ?)""";
        try (PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setLong(1, entradaId);
            preparada.setLong(2, item.getProdutoId());
            preparada.setInt(3, item.getQuantidade());
            preparada.setBigDecimal(4, item.getValorUnitario());
            preparada.setBigDecimal(5, item.getSubtotal());
            preparada.executeUpdate();
        }
    }

    private List<EntradaItem> listarItens(Connection conexao, long entradaId) throws SQLException {
        String sql = """
                SELECT i.id, i.entrada_id, i.produto_id, p.nome AS produto_nome, i.quantidade, i.valor_unitario, i.subtotal
                  FROM entrada_item i
                  JOIN produtos p ON p.id = i.produto_id
                 WHERE i.entrada_id = ?
                 ORDER BY i.id""";
        List<EntradaItem> itens = new ArrayList<>();
        try (PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setLong(1, entradaId);
            try (ResultSet resultado = preparada.executeQuery()) {
                while (resultado.next()) {
                    itens.add(new EntradaItem(
                            resultado.getLong("id"),
                            resultado.getLong("entrada_id"),
                            resultado.getLong("produto_id"),
                            resultado.getString("produto_nome"),
                            resultado.getInt("quantidade"),
                            resultado.getBigDecimal("valor_unitario"),
                            resultado.getBigDecimal("subtotal")));
                }
            }
        }
        return itens;
    }

    static BigDecimal calcularCustoMedioPonderado(int estoqueAtual, BigDecimal custoAtual, int quantidade,
                                                BigDecimal valorUnitario) {
        if (quantidade <= 0) {
            return custoAtual == null ? BigDecimal.ZERO : custoAtual;
        }
        if (valorUnitario == null) {
            valorUnitario = BigDecimal.ZERO;
        }
        if (custoAtual == null) {
            custoAtual = BigDecimal.ZERO;
        }
        if (estoqueAtual <= 0) {
            return valorUnitario.setScale(6, RoundingMode.HALF_UP);
        }
        BigDecimal valorEstoque = custoAtual.multiply(BigDecimal.valueOf(estoqueAtual));
        BigDecimal valorEntrada = valorUnitario.multiply(BigDecimal.valueOf(quantidade));
        BigDecimal soma = valorEstoque.add(valorEntrada);
        BigDecimal total = BigDecimal.valueOf(estoqueAtual + quantidade);
        return soma.divide(total, 6, RoundingMode.HALF_UP);
    }

    private EntradaMercadoria mapearCabecalho(ResultSet resultado) throws SQLException {
        Timestamp data = resultado.getTimestamp("data_entrada");
        Timestamp criado = resultado.getTimestamp("criado_em");
        return new EntradaMercadoria(
                resultado.getLong("id"),
                resultado.getObject("fornecedor_id") == null ? null : resultado.getLong("fornecedor_id"),
                resultado.getString("fornecedor_nome"),
                resultado.getString("numero_nota"),
                data == null ? null : data.toLocalDateTime(),
                resultado.getObject("usuario_id") == null ? null : resultado.getLong("usuario_id"),
                resultado.getString("usuario_nome"),
                resultado.getBigDecimal("valor_total"),
                resultado.getString("observacao"),
                criado == null ? null : criado.toLocalDateTime());
    }
}
