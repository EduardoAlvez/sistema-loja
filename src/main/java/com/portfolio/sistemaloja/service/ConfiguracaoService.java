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
        cache.set(new Cache(carregada, System.nanoTime() + VALIDADE_CACHE_MS * 1_000_000));
        return carregada;
    }

    public void invalidar() {
        cache.set(null);
    }

    private record Cache(ConfiguracoesAplicacao valor, long validade) {
        boolean expirado() {
            return System.nanoTime() >= validade;
        }
    }

    public Set<String> chavesSobrescritasPorAmbiente() {
        return configuracoes.chavesSobrescritasPorAmbiente();
    }

    public ConfiguracoesAplicacao salvarLoja(FidelidadeConfig fidelidade, ConfiguracaoPix pix) {
        exigirAdmin();
        try (Connection conexao = conexaoFonte.getConnection()) {
            try {
                conexao.setAutoCommit(false);
                ConfiguracaoRepository.Linha antiga = configuracoes.carregarLinhaParaAtualizacao(conexao);
                FidelidadeConfig salva = preservarOverrides(fidelidade, antiga);
                ConfiguracaoPix pixSalvo = preservarOverrides(pix, antiga);
                validar(salva, pixSalvo);
                ConfiguracoesAplicacao efetiva = configuracoes.salvarLoja(salva, pixSalvo, conexao);
                validar(efetiva.fidelidade(), efetiva.pix());
                auditarAlteracoes(antiga, salva, pixSalvo, conexao);
                conexao.commit();
                cache.set(new Cache(efetiva, System.nanoTime() + VALIDADE_CACHE_MS * 1_000_000));
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

    private FidelidadeConfig preservarOverrides(FidelidadeConfig pedida, ConfiguracaoRepository.Linha antiga) {
        Set<String> sobrescritas = configuracoes.chavesSobrescritasPorAmbiente();
        return new FidelidadeConfig(
                sobrescritas.contains("pontosPorReal") ? antiga.pontosPorReal() : pedida.pontosPorReal(),
                sobrescritas.contains("valorPorPonto") ? antiga.valorPorPonto() : pedida.valorPorPonto());
    }

    private ConfiguracaoPix preservarOverrides(ConfiguracaoPix pedida, ConfiguracaoRepository.Linha antiga) {
        Set<String> sobrescritas = configuracoes.chavesSobrescritasPorAmbiente();
        return new ConfiguracaoPix(
                sobrescritas.contains("pixChave") ? antiga.pixChave() : pedida.chave(),
                sobrescritas.contains("pixNome") ? antiga.pixNome() : pedida.nomeRecebedor(),
                sobrescritas.contains("pixCidade") ? antiga.pixCidade() : pedida.cidade());
    }

    private void validar(FidelidadeConfig fidelidade, ConfiguracaoPix pix) {
        new FidelidadeConfig(fidelidade.pontosPorReal(), fidelidade.valorPorPonto());
        pix.validar();
        if (pix.chave().length() > ConfiguracaoPix.CHAVE_MAX) {
            throw new ValidacaoException("A chave PIX deve ter no maximo " + ConfiguracaoPix.CHAVE_MAX + " caracteres.");
        }
        if (pix.nomeRecebedor().length() > ConfiguracaoPix.NOME_MAX) {
            throw new ValidacaoException("O nome do recebedor deve ter no maximo "
                    + ConfiguracaoPix.NOME_MAX + " caracteres.");
        }
        if (pix.cidade().length() > ConfiguracaoPix.CIDADE_MAX) {
            throw new ValidacaoException("A cidade do recebedor deve ter no maximo "
                    + ConfiguracaoPix.CIDADE_MAX + " caracteres.");
        }
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
