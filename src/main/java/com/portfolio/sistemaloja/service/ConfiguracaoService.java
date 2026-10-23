package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.Sessao;
import com.portfolio.sistemaloja.db.ConexaoFonte;
import com.portfolio.sistemaloja.model.ConfiguracoesAplicacao;
import com.portfolio.sistemaloja.repository.ConfiguracaoRepository;
import com.portfolio.sistemaloja.repository.RepositorioException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class ConfiguracaoService {

    private static final long VALIDADE_CACHE_MS = 2000;

    private final ConfiguracaoRepository configuracoes;
    private final Sessao sessao;
    private final ConexaoFonte conexaoFonte;
    private final AtomicReference<Cache> cache = new AtomicReference<>();

    public ConfiguracaoService(ConfiguracaoRepository configuracoes, Sessao sessao, ConexaoFonte conexaoFonte) {
        this.configuracoes = Objects.requireNonNull(configuracoes);
        this.sessao = Objects.requireNonNull(sessao);
        this.conexaoFonte = Objects.requireNonNull(conexaoFonte);
    }

    public ConfiguracoesAplicacao atual() {
        Cache existente = cache.get();
        if (existente != null && !existente.expirado()) {
            return existente.valor();
        }
        ConfiguracoesAplicacao carregada = configuracoes.carregar();
        cache.set(new Cache(carregada, System.currentTimeMillis() + VALIDADE_CACHE_MS));
        return carregada;
    }

    public void invalidar() {
        cache.set(null);
    }

    private record Cache(ConfiguracoesAplicacao valor, long validade) {
        boolean expirado() {
            return System.currentTimeMillis() >= validade;
        }
    }

    public Set<String> chavesSobrescritasPorAmbiente() {
        return configuracoes.chavesSobrescritasPorAmbiente();
    }

    public ConfiguracoesAplicacao salvarLoja(FidelidadeConfig fidelidade, ConfiguracaoPix pix) {
        exigirAdmin();
        validar(fidelidade, pix);
        try (Connection conexao = conexaoFonte.getConnection()) {
            try {
                conexao.setAutoCommit(false);
                ConfiguracaoRepository.Linha antiga = configuracoes.carregarLinha(conexao);
                ConfiguracoesAplicacao efetiva = configuracoes.salvarLoja(fidelidade, pix, conexao);
                validar(efetiva.fidelidade(), efetiva.pix());
                auditarAlteracoes(antiga, fidelidade, pix, conexao);
                conexao.commit();
                cache.set(new Cache(efetiva, System.currentTimeMillis() + VALIDADE_CACHE_MS));
                return efetiva;
            } catch (SQLException e) {
                conexao.rollback();
                throw new RepositorioException("Falha ao salvar configuracoes da loja", e);
            } catch (RuntimeException e) {
                rollbackSilencioso(conexao, e);
                throw e;
            }
        } catch (SQLException e) {
            throw new RepositorioException("Falha ao salvar configuracoes da loja", e);
        }
    }

    private void rollbackSilencioso(Connection conexao, RuntimeException causa) {
        try {
            conexao.rollback();
        } catch (SQLException rollback) {
            causa.addSuppressed(rollback);
        }
    }

    private void validar(FidelidadeConfig fidelidade, ConfiguracaoPix pix) {
        new FidelidadeConfig(fidelidade.pontosPorReal(), fidelidade.valorPorPonto());
        pix.validar();
    }

    private void auditarAlteracoes(ConfiguracaoRepository.Linha antiga, FidelidadeConfig fidelidade,
                                   ConfiguracaoPix pix, Connection conexao) throws SQLException {
        auditarDecimal("fidelidade.pontosPorReal", antiga.pontosPorReal(), fidelidade.pontosPorReal(), conexao);
        auditarDecimal("fidelidade.valorPorPonto", antiga.valorPorPonto(), fidelidade.valorPorPonto(), conexao);
        auditar("pix.chave", antiga.pixChave(), pix.chave(), conexao);
        auditar("pix.nome", antiga.pixNome(), pix.nomeRecebedor(), conexao);
        auditar("pix.cidade", antiga.pixCidade(), pix.cidade(), conexao);
    }

    private void auditarDecimal(String chave, java.math.BigDecimal antigo, java.math.BigDecimal novoValor,
                                Connection conexao) throws SQLException {
        if (antigo != null && novoValor != null && antigo.compareTo(novoValor) == 0) {
            return;
        }
        auditar(chave, antigo == null ? null : antigo.toPlainString(),
                novoValor == null ? null : novoValor.toPlainString(), conexao);
    }

    private void auditar(String chave, String antigo, String novoValor, Connection conexao) throws SQLException {
        if (Objects.equals(antigo, novoValor)) {
            return;
        }
        String usuario = sessao.getAtual() == null ? "sistema" : sessao.getAtual().getLogin();
        String detalhe = chave + ": " + seguro(antigo) + " -> " + seguro(novoValor);
        if (detalhe.length() > 250) {
            detalhe = detalhe.substring(0, 250);
        }
        String sql = "INSERT INTO auditoria (usuario_login, acao, entidade, entidade_id, detalhe) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement preparada = conexao.prepareStatement(sql)) {
            preparada.setString(1, usuario);
            preparada.setString(2, "CONFIG_ALTERADA");
            preparada.setString(3, "CONFIGURACAO");
            preparada.setObject(4, null);
            preparada.setString(5, detalhe);
            preparada.executeUpdate();
        }
    }

    private String seguro(String valor) {
        return valor == null ? "" : valor;
    }

    private void exigirAdmin() {
        if (sessao.getAtual() == null || !sessao.getAtual().isAdmin()) {
            throw new ValidacaoException("Somente administradores podem alterar as configuracoes.");
        }
    }
}
