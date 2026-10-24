package com.portfolio.sistemaloja.repository;

import com.portfolio.sistemaloja.db.ConexaoFonte;
import com.portfolio.sistemaloja.impressao.ImpressoraConfig;
import com.portfolio.sistemaloja.model.ConfiguracoesAplicacao;
import com.portfolio.sistemaloja.service.ConfiguracaoPix;
import com.portfolio.sistemaloja.service.FidelidadeConfig;
import com.portfolio.sistemaloja.service.ValidacaoException;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class ConfiguracaoRepository {

    private final ConexaoFonte fonte;

    public ConfiguracaoRepository(ConexaoFonte fonte) {
        this.fonte = fonte;
    }

    public ConfiguracoesAplicacao carregar() {
        try (Connection conexao = fonte.getConnection()) {
            return resolver(carregarLinha(conexao));
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao carregar configuracoes da loja", e);
        }
    }

    public ConfiguracoesAplicacao salvarLoja(FidelidadeConfig fidelidade, ConfiguracaoPix pix, Connection conexao)
            throws SQLException {
        String sql = "UPDATE configuracoes SET pontos_por_real = ?, valor_por_ponto = ?, "
                + "pix_chave = ?, pix_nome = ?, pix_cidade = ?, atualizado_em = ? WHERE id = 1";
        try (PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setBigDecimal(1, fidelidade.pontosPorReal());
            preparada.setBigDecimal(2, fidelidade.valorPorPonto());
            preparada.setString(3, pix.chave());
            preparada.setString(4, pix.nomeRecebedor());
            preparada.setString(5, pix.cidade());
            preparada.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            if (preparada.executeUpdate() == 0) {
                inserirPadrao(conexao, fidelidade, pix);
            }
        }
        return resolver(carregarLinha(conexao));
    }

    public Linha carregarLinha(Connection conexao) throws SQLException {
        return carregarLinha(conexao, false);
    }

    public Linha carregarLinhaParaAtualizacao(Connection conexao) throws SQLException {
        return carregarLinha(conexao, true);
    }

    private Linha carregarLinha(Connection conexao, boolean travar) throws SQLException {
        String sql = "SELECT pontos_por_real, valor_por_ponto, pix_chave, pix_nome, pix_cidade "
                + "FROM configuracoes WHERE id = 1" + (travar ? " FOR UPDATE" : "");
        try (PreparedStatement preparada = conexao.prepareStatement(sql)) {
            try (ResultSet resultado = preparada.executeQuery()) {
                if (resultado.next()) {
                    return lerLinha(resultado);
                }
            }
        }
        return padrao();
    }

    public Set<String> chavesSobrescritasPorAmbiente() {
        Set<String> sobrescritas = new HashSet<>();
        if (temPropriedade("fidelidade.pontosPorReal") || temAmbiente("FIDELIDADE_PONTOS_POR_REAL")) {
            sobrescritas.add("pontosPorReal");
        }
        if (temPropriedade("fidelidade.valorPorPonto") || temAmbiente("FIDELIDADE_VALOR_PONTO")) {
            sobrescritas.add("valorPorPonto");
        }
        if (temPropriedade("pix.chave") || temAmbiente("PIX_CHAVE")) {
            sobrescritas.add("pixChave");
        }
        if (temPropriedade("pix.nome") || temAmbiente("PIX_NOME")) {
            sobrescritas.add("pixNome");
        }
        if (temPropriedade("pix.cidade") || temAmbiente("PIX_CIDADE")) {
            sobrescritas.add("pixCidade");
        }
        return sobrescritas;
    }

    private void inserirPadrao(Connection conexao, FidelidadeConfig fidelidade, ConfiguracaoPix pix)
            throws SQLException {
        String sql = "INSERT INTO configuracoes "
                + "(id, pontos_por_real, valor_por_ponto, pix_chave, pix_nome, pix_cidade, atualizado_em) "
                + "VALUES (1,?,?,?,?,?,?)";
        try (PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setBigDecimal(1, fidelidade.pontosPorReal());
            preparada.setBigDecimal(2, fidelidade.valorPorPonto());
            preparada.setString(3, pix.chave());
            preparada.setString(4, pix.nomeRecebedor());
            preparada.setString(5, pix.cidade());
            preparada.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            preparada.executeUpdate();
        }
    }

    private Linha padrao() {
        return new Linha(
                FidelidadeConfig.PONTOS_POR_REAL_PADRAO,
                FidelidadeConfig.VALOR_POR_PONTO_PADRAO,
                "pagamentos@lojaexemplo.com.br",
                "LOJA EXEMPLO LTDA",
                "SAO PAULO");
    }

    private boolean temPropriedade(String chave) {
        String valor = System.getProperty(chave);
        return valor != null && !valor.isBlank();
    }

    private boolean temAmbiente(String chave) {
        String valor = System.getenv(chave);
        return valor != null && !valor.isBlank();
    }

    private Linha lerLinha(ResultSet resultado) throws SQLException {
        BigDecimal pontosPorReal = resultado.getBigDecimal("pontos_por_real");
        BigDecimal valorPorPonto = resultado.getBigDecimal("valor_por_ponto");
        String pixChave = resultado.getString("pix_chave");
        String pixNome = resultado.getString("pix_nome");
        String pixCidade = resultado.getString("pix_cidade");
        return new Linha(pontosPorReal, valorPorPonto, pixChave, pixNome, pixCidade);
    }

    private ConfiguracoesAplicacao resolver(Linha linha) {
        BigDecimal pontosPorReal = resolverDecimal("fidelidade.pontosPorReal", "FIDELIDADE_PONTOS_POR_REAL",
                linha.pontosPorReal(), FidelidadeConfig.PONTOS_POR_REAL_PADRAO);
        BigDecimal valorPorPonto = resolverDecimal("fidelidade.valorPorPonto", "FIDELIDADE_VALOR_PONTO",
                linha.valorPorPonto(), FidelidadeConfig.VALOR_POR_PONTO_PADRAO);
        FidelidadeConfig fidelidade = new FidelidadeConfig(pontosPorReal, valorPorPonto);

        String pixChave = resolverTexto("pix.chave", "PIX_CHAVE", linha.pixChave(),
                "pagamentos@lojaexemplo.com.br");
        String pixNome = resolverTexto("pix.nome", "PIX_NOME", linha.pixNome(), "LOJA EXEMPLO LTDA");
        String pixCidade = resolverTexto("pix.cidade", "PIX_CIDADE", linha.pixCidade(), "SAO PAULO");
        ConfiguracaoPix pix = new ConfiguracaoPix(pixChave, pixNome, pixCidade);
        pix.validar();

        ImpressoraConfig impressora = ImpressoraConfig.padrao();
        return new ConfiguracoesAplicacao(fidelidade, pix, impressora);
    }

    private BigDecimal resolverDecimal(String propriedade, String variavel, BigDecimal banco,
                                       BigDecimal padrao) {
        String valor = valorDe(propriedade, variavel);
        if (valor == null) {
            return banco == null ? padrao : banco;
        }
        try {
            return new BigDecimal(valor.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new ValidacaoException("Valor invalido para " + propriedade + ": " + valor);
        }
    }

    private String resolverTexto(String propriedade, String variavel, String banco, String padrao) {
        String valor = valorDe(propriedade, variavel);
        if (valor == null) {
            return banco == null || banco.isBlank() ? padrao : banco;
        }
        return valor;
    }

    private String valorDe(String propriedade, String variavel) {
        String valor = System.getProperty(propriedade);
        if (valor == null || valor.isBlank()) {
            valor = System.getenv(variavel);
        }
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    public record Linha(BigDecimal pontosPorReal, BigDecimal valorPorPonto,
                        String pixChave, String pixNome, String pixCidade) {
    }

    public boolean fiadoHabilitado() {
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(
                     "SELECT fiado_habilitado FROM configuracoes WHERE id = 1")) {
            try (ResultSet resultado = preparada.executeQuery()) {
                if (resultado.next()) {
                    return resultado.getBoolean("fiado_habilitado");
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao ler se o fiado está habilitado", e);
        }
        return false;
    }

    public BigDecimal taxaJurosFiado() {
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(
                     "SELECT taxa_juros_fiado FROM configuracoes WHERE id = 1")) {
            try (ResultSet resultado = preparada.executeQuery()) {
                if (resultado.next()) {
                    return resultado.getBigDecimal("taxa_juros_fiado");
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao ler taxa de juros do fiado", e);
        }
        return BigDecimal.ZERO;
    }

    public void definirFiadoHabilitado(boolean habilitado) {
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(
                     "UPDATE configuracoes SET fiado_habilitado = ? WHERE id = 1")) {
            preparada.setBoolean(1, habilitado);
            preparada.executeUpdate();
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao definir se o fiado está habilitado", e);
        }
    }

    public void definirTaxaJurosFiado(BigDecimal taxa) {
        try (Connection conexao = fonte.getConnection();
             PreparedStatement preparada = conexao.prepareStatement(
                     "UPDATE configuracoes SET taxa_juros_fiado = ? WHERE id = 1")) {
            preparada.setBigDecimal(1, taxa);
            preparada.executeUpdate();
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao definir taxa de juros do fiado", e);
        }
    }
}
