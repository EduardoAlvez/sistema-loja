package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.Sessao;
import com.portfolio.sistemaloja.repository.AuditoriaRepository;
import com.portfolio.sistemaloja.repository.EventoAuditoria;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDate;
import java.util.List;

public class AuditoriaService {

    private static final Logger LOG = LogManager.getLogger(AuditoriaService.class);

    private final AuditoriaRepository auditoria;
    private final Sessao sessao;

    public AuditoriaService(AuditoriaRepository auditoria, Sessao sessao) {
        this.auditoria = auditoria;
        this.sessao = sessao;
    }

    public void registrar(String acao, String entidade, Long entidadeId, String detalhe) {
        String usuario = sessao.getAtual() == null ? "sistema" : sessao.getAtual().getLogin();
        registrarComo(usuario, acao, entidade, entidadeId, detalhe);
    }

    public void registrarComo(String usuarioLogin, String acao, String entidade, Long entidadeId,
                              String detalhe) {
        try {
            auditoria.registrar(usuarioLogin, acao, entidade, entidadeId, detalhe);
        } catch (RuntimeException e) {
            LOG.error("Falha ao gravar auditoria {} de {}", acao, usuarioLogin, e);
        }
    }

    public List<EventoAuditoria> listar(LocalDate inicio, LocalDate fim) {
        return auditoria.listar(inicio, fim);
    }
}
