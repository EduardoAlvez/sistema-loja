package com.portfolio.sistemaloja.repository;

import com.portfolio.sistemaloja.db.ConexaoFonte;
import com.portfolio.sistemaloja.model.Produto;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Types;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ProdutoRepository {

    private static final String SELECT_BASE = """
            SELECT p.id, p.codigo_barras, p.nome, p.categoria_id, c.nome AS categoria_nome,
                   p.preco_custo, p.preco_venda, p.estoque, p.estoque_minimo, p.ativo,
                   p.preco_promocional, p.promo_inicio, p.promo_fim, p.promo_dias,
                   p.promo_hora_inicio, p.promo_hora_fim
              FROM produtos p
              LEFT JOIN categorias c ON c.id = p.categoria_id""";

    private final ConexaoFonte fonte;

    public ProdutoRepository(ConexaoFonte fonte) {
        this.fonte = fonte;
    }

    public List<Produto> listar(String termo) {
        return listar(termo, null);
    }

    public List<Produto> listar(String termo, Long categoriaId) {
        StringBuilder sql = new StringBuilder(SELECT_BASE)
                .append(" WHERE (p.nome LIKE ? OR p.codigo_barras LIKE ?)");
        if (categoriaId != null) {
            sql.append(" AND p.categoria_id = ?");
        }
        sql.append(" ORDER BY p.nome");
        List<Produto> produtos = new ArrayList<>();
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql.toString())) {
            String filtro = "%" + (termo == null ? "" : termo.strip()) + "%";
            preparada.setString(1, filtro);
            preparada.setString(2, filtro);
            if (categoriaId != null) {
                preparada.setLong(3, categoriaId);
            }
            try (ResultSet resultado = preparada.executeQuery()) {
                while (resultado.next()) {
                    produtos.add(mapear(resultado));
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao listar produtos", e);
        }
        return produtos;
    }

    public List<Produto> listarComEstoqueBaixo() {
        String sql = SELECT_BASE
                + " WHERE p.ativo = TRUE AND p.estoque <= p.estoque_minimo"
                + " ORDER BY p.estoque, p.nome";
        List<Produto> produtos = new ArrayList<>();
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql);
             ResultSet resultado = preparada.executeQuery()) {
            while (resultado.next()) {
                produtos.add(mapear(resultado));
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao listar produtos com estoque baixo", e);
        }
        return produtos;
    }

    public List<Produto> listarAtivos() {
        String sql = SELECT_BASE + " WHERE p.ativo = TRUE ORDER BY p.nome";
        List<Produto> produtos = new ArrayList<>();
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql);
             ResultSet resultado = preparada.executeQuery()) {
            while (resultado.next()) {
                produtos.add(mapear(resultado));
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao listar produtos ativos", e);
        }
        return produtos;
    }

    public Optional<Produto> buscarPorId(long id) {
        String sql = SELECT_BASE + " WHERE p.id = ?";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setLong(1, id);
            try (ResultSet resultado = preparada.executeQuery()) {
                if (resultado.next()) {
                    return Optional.of(mapear(resultado));
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao buscar produto", e);
        }
        return Optional.empty();
    }

    public Optional<Produto> buscarPorCodigoBarras(String codigo) {
        String sql = SELECT_BASE + " WHERE p.codigo_barras = ?";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setString(1, codigo);
            try (ResultSet resultado = preparada.executeQuery()) {
                if (resultado.next()) {
                    return Optional.of(mapear(resultado));
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao buscar produto por código", e);
        }
        return Optional.empty();
    }

    public boolean codigoExiste(String codigo, Long idExcluido) {
        String sql = "SELECT COUNT(*) FROM produtos WHERE codigo_barras = ? AND id <> ?";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setString(1, codigo);
            preparada.setLong(2, idExcluido == null ? -1L : idExcluido);
            try (ResultSet resultado = preparada.executeQuery()) {
                return resultado.next() && resultado.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao verificar código de barras", e);
        }
    }

    public Produto salvar(Produto produto) {
        String sql = produto.getId() == null ? insercao() : atualizacao();
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preencher(preparada, produto);
            if (produto.getId() != null) {
                preparada.setLong(15, produto.getId());
            }
            preparada.executeUpdate();
            if (produto.getId() == null) {
                try (ResultSet chaves = preparada.getGeneratedKeys()) {
                    if (chaves.next()) {
                        produto.setId(chaves.getLong(1));
                    }
                }
            }
            return produto;
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao salvar produto", e);
        }
    }

    public void remover(long id) {
        String sql = "DELETE FROM produtos WHERE id = ?";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setLong(1, id);
            preparada.executeUpdate();
        } catch (SQLException e) {
            throw new RepositorioException("Não foi possível excluir o produto (possível venda vinculada)", e);
        }
    }

    public boolean alterarEstoque(long produtoId, int quantidade, Connection conexao) throws SQLException {
        String sql = "UPDATE produtos SET estoque = estoque + ? WHERE id = ? AND estoque + ? >= 0";
        try (PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setInt(1, quantidade);
            preparada.setLong(2, produtoId);
            preparada.setInt(3, quantidade);
            return preparada.executeUpdate() == 1;
        }
    }

    private String insercao() {
        return "INSERT INTO produtos (codigo_barras, nome, categoria_id, preco_custo, preco_venda, "
                + "estoque, ativo, estoque_minimo, preco_promocional, promo_inicio, promo_fim, promo_dias, "
                + "promo_hora_inicio, promo_hora_fim) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    private String atualizacao() {
        return "UPDATE produtos SET codigo_barras = ?, nome = ?, categoria_id = ?, preco_custo = ?, "
                + "preco_venda = ?, estoque = ?, ativo = ?, estoque_minimo = ?, preco_promocional = ?, "
                + "promo_inicio = ?, promo_fim = ?, promo_dias = ?, promo_hora_inicio = ?, "
                + "promo_hora_fim = ? WHERE id = ?";
    }

    private void preencher(PreparedStatement preparada, Produto produto) throws SQLException {
        preparada.setString(1, produto.getCodigoBarras());
        preparada.setString(2, produto.getNome());
        if (produto.getCategoriaId() == null) {
            preparada.setObject(3, null);
        } else {
            preparada.setLong(3, produto.getCategoriaId());
        }
        preparada.setBigDecimal(4, produto.getPrecoCusto());
        preparada.setBigDecimal(5, produto.getPrecoVenda());
        preparada.setInt(6, produto.getEstoque());
        preparada.setBoolean(7, produto.isAtivo());
        preparada.setInt(8, produto.getEstoqueMinimo());
        preparada.setBigDecimal(9, produto.getPrecoPromocional());
        if (produto.getPromoInicio() == null) {
            preparada.setNull(10, Types.DATE);
        } else {
            preparada.setObject(10, produto.getPromoInicio());
        }
        if (produto.getPromoFim() == null) {
            preparada.setNull(11, Types.DATE);
        } else {
            preparada.setObject(11, produto.getPromoFim());
        }
        String dias = textoDias(produto.getPromoDias());
        if (dias.isEmpty()) {
            preparada.setNull(12, Types.VARCHAR);
        } else {
            preparada.setString(12, dias);
        }
        if (produto.getPromoHoraInicio() == null) {
            preparada.setNull(13, Types.TIME);
        } else {
            preparada.setObject(13, produto.getPromoHoraInicio());
        }
        if (produto.getPromoHoraFim() == null) {
            preparada.setNull(14, Types.TIME);
        } else {
            preparada.setObject(14, produto.getPromoHoraFim());
        }
    }

    private Produto mapear(ResultSet resultado) throws SQLException {
        long categoriaId = resultado.getLong("categoria_id");
        boolean semCategoria = resultado.wasNull();
        Produto produto = new Produto(
                resultado.getLong("id"),
                resultado.getString("codigo_barras"),
                resultado.getString("nome"),
                semCategoria ? null : categoriaId,
                resultado.getBigDecimal("preco_custo"),
                resultado.getBigDecimal("preco_venda"),
                resultado.getInt("estoque"),
                resultado.getBoolean("ativo"));
        produto.setCategoriaNome(resultado.getString("categoria_nome"));
        produto.setEstoqueMinimo(resultado.getInt("estoque_minimo"));
        produto.setPrecoPromocional(resultado.getBigDecimal("preco_promocional"));
        produto.setPromoInicio(data(resultado, "promo_inicio"));
        produto.setPromoFim(data(resultado, "promo_fim"));
        produto.setPromoDias(dias(resultado.getString("promo_dias")));
        produto.setPromoHoraInicio(hora(resultado, "promo_hora_inicio"));
        produto.setPromoHoraFim(hora(resultado, "promo_hora_fim"));
        return produto;
    }

    private static LocalDate data(ResultSet resultado, String coluna) throws SQLException {
        Date valor = resultado.getDate(coluna);
        return valor == null ? null : valor.toLocalDate();
    }

    private static LocalTime hora(ResultSet resultado, String coluna) throws SQLException {
        Time valor = resultado.getTime(coluna);
        return valor == null ? null : valor.toLocalTime();
    }

    private static Set<DayOfWeek> dias(String texto) {
        if (texto == null || texto.isBlank()) {
            return EnumSet.noneOf(DayOfWeek.class);
        }
        EnumSet<DayOfWeek> dias = EnumSet.noneOf(DayOfWeek.class);
        for (String parte : texto.split(",")) {
            int valor;
            try {
                valor = Integer.parseInt(parte.strip());
            } catch (NumberFormatException e) {
                throw new RepositorioException("Dia de promocao invalido no banco: " + parte, e);
            }
            if (valor < 1 || valor > 7) {
                throw new RepositorioException("Dia de promocao fora de 1..7 no banco: " + valor);
            }
            dias.add(DayOfWeek.of(valor));
        }
        return dias;
    }

    private static String textoDias(Set<DayOfWeek> dias) {
        if (dias == null || dias.isEmpty()) {
            return "";
        }
        return dias.stream()
                .sorted()
                .map(dia -> String.valueOf(dia.getValue()))
                .collect(java.util.stream.Collectors.joining(","));
    }
}
