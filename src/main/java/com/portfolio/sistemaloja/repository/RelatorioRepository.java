package com.portfolio.sistemaloja.repository;

import com.portfolio.sistemaloja.db.ConexaoFonte;
import com.portfolio.sistemaloja.repository.ComparativoPeriodo;
import com.portfolio.sistemaloja.repository.CurvaABC;
import com.portfolio.sistemaloja.repository.GiroEstoque;
import com.portfolio.sistemaloja.repository.ProdutoSemMovimento;
import com.portfolio.sistemaloja.repository.VendaHora;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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

    public List<ResumoOperador> resumoPorOperador(LocalDate inicio, LocalDate fim) {
        String sql = """
                SELECT u.id, u.nome, u.comissao_percentual, COUNT(v.id) AS qtd,
                       COALESCE(SUM(v.total), 0) AS total,
                       COALESCE(SUM(v.total * u.comissao_percentual / 100), 0) AS comissao
                  FROM usuarios u
                  JOIN vendas v ON v.usuario_id = u.id
                 WHERE v.status = 'CONCLUIDA' AND v.data_hora >= ? AND v.data_hora < ?
                 GROUP BY u.id, u.nome, u.comissao_percentual
                 ORDER BY total DESC, u.nome""";
        List<ResumoOperador> lista = new ArrayList<>();
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setObject(1, inicio);
            preparada.setObject(2, fim.plusDays(1));
            try (ResultSet resultado = preparada.executeQuery()) {
                while (resultado.next()) {
                    lista.add(new ResumoOperador(
                            resultado.getLong("id"),
                            resultado.getString("nome"),
                            resultado.getBigDecimal("comissao_percentual"),
                            resultado.getInt("qtd"),
                            resultado.getBigDecimal("total"),
                            resultado.getBigDecimal("comissao")));
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao gerar relatório por operador", e);
        }
        return lista;
    }

    public List<VendaDoDia> faturamentoPorDia(LocalDate inicio, LocalDate fim) {
        String sql = """
                SELECT CAST(data_hora AS DATE) AS dia, COUNT(*) AS qtd, COALESCE(SUM(total), 0) AS total
                  FROM vendas
                 WHERE status = 'CONCLUIDA' AND data_hora >= ? AND data_hora < ?
                 GROUP BY CAST(data_hora AS DATE)
                 ORDER BY dia""";
        List<VendaDoDia> lista = new ArrayList<>();
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setObject(1, inicio);
            preparada.setObject(2, fim.plusDays(1));
            try (ResultSet resultado = preparada.executeQuery()) {
                while (resultado.next()) {
                    lista.add(new VendaDoDia(
                            resultado.getObject("dia", LocalDate.class),
                            resultado.getInt("qtd"),
                            resultado.getBigDecimal("total")));
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao gerar o faturamento por dia", e);
        }
        return lista;
    }

    public List<ProdutoEstoque> estoqueBaixo() {
        String sql = """
                SELECT id, nome, estoque, preco_venda
                  FROM produtos
                 WHERE ativo = TRUE AND estoque <= estoque_minimo
                 ORDER BY estoque ASC""";
        List<ProdutoEstoque> lista = new ArrayList<>();
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
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

    public List<VendaHora> vendasPorHora(LocalDate inicio, LocalDate fim) {
        String sql = """
                SELECT EXTRACT(HOUR FROM v.data_hora) AS hora,
                       COUNT(*) AS quantidade,
                       COALESCE(SUM(v.total), 0) AS total
                  FROM vendas v
                 WHERE v.status = 'CONCLUIDA' AND v.data_hora >= ? AND v.data_hora < ?
                 GROUP BY EXTRACT(HOUR FROM v.data_hora)
                 ORDER BY hora""";
        List<VendaHora> lista = new ArrayList<>();
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setObject(1, inicio);
            preparada.setObject(2, fim.plusDays(1));
            try (ResultSet resultado = preparada.executeQuery()) {
                while (resultado.next()) {
                    lista.add(new VendaHora(
                            resultado.getInt("hora"),
                            resultado.getInt("quantidade"),
                            resultado.getBigDecimal("total")));
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao gerar relatório de vendas por hora", e);
        }
        return lista;
    }

    public List<CurvaABC> curvaABCProdutos(LocalDate inicio, LocalDate fim) {
        String sql = """
                SELECT p.id, p.nome, SUM(i.quantidade) AS qtd, SUM(i.subtotal) AS total
                  FROM venda_itens i
                  JOIN vendas v ON v.id = i.venda_id
                  JOIN produtos p ON p.id = i.produto_id
                 WHERE v.status = 'CONCLUIDA' AND v.data_hora >= ? AND v.data_hora < ?
                 GROUP BY p.id, p.nome
                 ORDER BY total DESC""";
        List<CurvaABC> lista = new ArrayList<>();
        BigDecimal totalGeral = BigDecimal.ZERO;
        List<CurvaABC> crua = new ArrayList<>();
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setObject(1, inicio);
            preparada.setObject(2, fim.plusDays(1));
            try (ResultSet resultado = preparada.executeQuery()) {
                while (resultado.next()) {
                    BigDecimal total = resultado.getBigDecimal("total");
                    totalGeral = totalGeral.add(total == null ? BigDecimal.ZERO : total);
                    crua.add(new CurvaABC(
                            resultado.getString("nome"),
                            "produto",
                            resultado.getInt("qtd"),
                            total,
                            BigDecimal.ZERO,
                            ""));
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao gerar curva ABC de produtos", e);
        }
        BigDecimal acumulado = BigDecimal.ZERO;
        for (CurvaABC item : crua) {
            BigDecimal percentual = totalGeral.compareTo(BigDecimal.ZERO) > 0
                    ? item.total().multiply(BigDecimal.valueOf(100)).divide(totalGeral, 4, BigDecimal.ROUND_HALF_UP)
                    : BigDecimal.ZERO;
            // Classe com base no percentual acumulado ANTES de incluir este item:
            // acumulado anterior < 80%  => A
            // acumulado anterior < 95%  => B
            // acumulado anterior >= 95% => C
            String classe = classificarABC(acumulado, totalGeral);
            acumulado = acumulado.add(item.total());
            lista.add(new CurvaABC(item.nome(), item.tipo(), item.quantidade(), item.total(),
                    percentual.stripTrailingZeros(), classe));
        }
        return lista;
    }

    public List<CurvaABC> curvaABCCategorias(LocalDate inicio, LocalDate fim) {
        String sql = """
                SELECT c.nome, SUM(i.subtotal) AS total, SUM(i.quantidade) AS qtd
                  FROM venda_itens i
                  JOIN vendas v ON v.id = i.venda_id
                  JOIN produtos p ON p.id = i.produto_id
                  JOIN categorias c ON c.id = p.categoria_id
                 WHERE v.status = 'CONCLUIDA' AND v.data_hora >= ? AND v.data_hora < ?
                 GROUP BY c.id, c.nome
                 ORDER BY total DESC""";
        List<CurvaABC> lista = new ArrayList<>();
        BigDecimal totalGeral = BigDecimal.ZERO;
        List<CurvaABC> crua = new ArrayList<>();
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setObject(1, inicio);
            preparada.setObject(2, fim.plusDays(1));
            try (ResultSet resultado = preparada.executeQuery()) {
                while (resultado.next()) {
                    BigDecimal total = resultado.getBigDecimal("total");
                    totalGeral = totalGeral.add(total == null ? BigDecimal.ZERO : total);
                    crua.add(new CurvaABC(
                            resultado.getString("nome"),
                            "categoria",
                            resultado.getInt("qtd"),
                            total,
                            BigDecimal.ZERO,
                            ""));
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao gerar curva ABC de categorias", e);
        }
        BigDecimal acumulado = BigDecimal.ZERO;
        for (CurvaABC item : crua) {
            BigDecimal percentual = totalGeral.compareTo(BigDecimal.ZERO) > 0
                    ? item.total().multiply(BigDecimal.valueOf(100)).divide(totalGeral, 4, BigDecimal.ROUND_HALF_UP)
                    : BigDecimal.ZERO;
            // Classe com base no percentual acumulado ANTES de incluir este item:
            // acumulado anterior < 80%  => A
            // acumulado anterior < 95%  => B
            // acumulado anterior >= 95% => C
            String classe = classificarABC(acumulado, totalGeral);
            acumulado = acumulado.add(item.total());
            lista.add(new CurvaABC(item.nome(), item.tipo(), item.quantidade(), item.total(),
                    percentual.stripTrailingZeros(), classe));
        }
        return lista;
    }

    public List<GiroEstoque> giroEstoque(LocalDate inicio, LocalDate fim) {
        String sql = """
                SELECT p.id, p.nome, c.nome AS categoria, p.estoque, p.estoque_minimo,
                       COALESCE(SUM(CASE WHEN v.id IS NOT NULL THEN i.quantidade ELSE 0 END), 0) AS vendas_periodo
                  FROM produtos p
                  LEFT JOIN categorias c ON c.id = p.categoria_id
                  LEFT JOIN venda_itens i ON i.produto_id = p.id
                  LEFT JOIN vendas v ON v.id = i.venda_id
                         AND v.status = 'CONCLUIDA'
                         AND v.data_hora >= ? AND v.data_hora < ?
                 WHERE p.ativo = TRUE
                 GROUP BY p.id, p.nome, c.nome, p.estoque, p.estoque_minimo
                 ORDER BY p.nome""";
        List<GiroEstoque> lista = new ArrayList<>();
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setObject(1, inicio);
            preparada.setObject(2, fim.plusDays(1));
            try (ResultSet resultado = preparada.executeQuery()) {
                while (resultado.next()) {
                    int estoque = resultado.getInt("estoque");
                    int estoqueMinimo = resultado.getInt("estoque_minimo");
                    int vendasPeriodo = resultado.getInt("vendas_periodo");
                    BigDecimal giro = estoque > 0
                            ? BigDecimal.valueOf(vendasPeriodo).divide(BigDecimal.valueOf(estoque), 4, BigDecimal.ROUND_HALF_UP)
                            : BigDecimal.ZERO;
                    boolean ruptura = estoque <= estoqueMinimo;
                    lista.add(new GiroEstoque(
                            resultado.getLong("id"),
                            resultado.getString("nome"),
                            resultado.getString("categoria") == null ? "-" : resultado.getString("categoria"),
                            estoque,
                            estoqueMinimo,
                            vendasPeriodo,
                            giro.stripTrailingZeros(),
                            ruptura));
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao gerar relatório de giro de estoque", e);
        }
        return lista;
    }

    public List<ProdutoSemMovimento> produtosSemMovimento(LocalDate inicio, LocalDate fim) {
        String sql = """
                SELECT p.id, p.nome, c.nome AS categoria, p.estoque
                  FROM produtos p
                  LEFT JOIN categorias c ON c.id = p.categoria_id
                 WHERE p.ativo = TRUE
                   AND NOT EXISTS (
                       SELECT 1 FROM venda_itens i
                       JOIN vendas v ON v.id = i.venda_id
                      WHERE v.status = 'CONCLUIDA'
                        AND v.data_hora >= ? AND v.data_hora < ?
                        AND i.produto_id = p.id
                   )
                 ORDER BY p.nome""";
        List<ProdutoSemMovimento> lista = new ArrayList<>();
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setObject(1, inicio);
            preparada.setObject(2, fim.plusDays(1));
            try (ResultSet resultado = preparada.executeQuery()) {
                while (resultado.next()) {
                    lista.add(new ProdutoSemMovimento(
                            resultado.getLong("id"),
                            resultado.getString("nome"),
                            resultado.getString("categoria") == null ? "-" : resultado.getString("categoria"),
                            resultado.getInt("estoque"),
                            null));
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao gerar relatório de produtos sem movimento", e);
        }
        return lista;
    }

    public ComparativoPeriodo comparativoPeriodo(LocalDate inicio, LocalDate fim) {
        long dias = ChronoUnit.DAYS.between(inicio, fim) + 1;
        LocalDate anteriorInicio = inicio.minusDays(dias);
        LocalDate anteriorFim = fim.minusDays(dias);

        BigDecimal totalAtual = vendasTotalPeriodo(inicio, fim);
        int quantidadeAtual = vendasQuantidadePeriodo(inicio, fim);
        BigDecimal totalAnterior = vendasTotalPeriodo(anteriorInicio, anteriorFim);

        BigDecimal variacao = totalAtual.subtract(totalAnterior);
        BigDecimal variacaoPercentual = totalAnterior.compareTo(BigDecimal.ZERO) > 0
                ? variacao.multiply(BigDecimal.valueOf(100)).divide(totalAnterior, 4, BigDecimal.ROUND_HALF_UP)
                : BigDecimal.ZERO;

        return new ComparativoPeriodo(
                "vs " + DATA.format(anteriorInicio) + " a " + DATA.format(anteriorFim),
                totalAtual,
                quantidadeAtual,
                variacao.stripTrailingZeros(),
                variacaoPercentual.stripTrailingZeros());
    }

    private BigDecimal vendasTotalPeriodo(LocalDate inicio, LocalDate fim) {
        String sql = """
                SELECT COALESCE(SUM(total), 0) AS total
                  FROM vendas
                 WHERE status = 'CONCLUIDA' AND data_hora >= ? AND data_hora < ?""";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setObject(1, inicio);
            preparada.setObject(2, fim.plusDays(1));
            try (ResultSet resultado = preparada.executeQuery()) {
                if (resultado.next()) {
                    return resultado.getBigDecimal("total");
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao calcular total do período", e);
        }
        return BigDecimal.ZERO;
    }

    private int vendasQuantidadePeriodo(LocalDate inicio, LocalDate fim) {
        String sql = """
                SELECT COUNT(*) AS quantidade
                  FROM vendas
                 WHERE status = 'CONCLUIDA' AND data_hora >= ? AND data_hora < ?""";
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setObject(1, inicio);
            preparada.setObject(2, fim.plusDays(1));
            try (ResultSet resultado = preparada.executeQuery()) {
                if (resultado.next()) {
                    return resultado.getInt("quantidade");
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao contar vendas do período", e);
        }
        return 0;
    }

    private String classificarABC(BigDecimal acumulado, BigDecimal totalGeral) {
        if (totalGeral.compareTo(BigDecimal.ZERO) <= 0) {
            return "C";
        }
        BigDecimal percentualAcumulado = acumulado.multiply(BigDecimal.valueOf(100)).divide(totalGeral, 4, BigDecimal.ROUND_HALF_UP);
        if (percentualAcumulado.compareTo(BigDecimal.valueOf(80)) <= 0) {
            return "A";
        }
        if (percentualAcumulado.compareTo(BigDecimal.valueOf(95)) <= 0) {
            return "B";
        }
        return "C";
    }

    private static final java.time.format.DateTimeFormatter DATA =
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
}

