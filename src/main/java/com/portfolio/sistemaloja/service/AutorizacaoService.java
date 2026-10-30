package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.Sessao;
import com.portfolio.sistemaloja.db.Senhas;
import com.portfolio.sistemaloja.model.Autorizacao;
import com.portfolio.sistemaloja.model.MotivoAutorizacao;
import com.portfolio.sistemaloja.model.PerfilUsuario;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.repository.UsuarioRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

/**
 * Serviço para autorização de ações sensíveis conforme perfil e motivo.
 */
public class AutorizacaoService {

    private static final Logger LOG = LogManager.getLogger(AutorizacaoService.class);

    private final UsuarioRepository usuarios;
    private final Sessao sessao;
    private final AuditoriaService auditoria;

    public AutorizacaoService(UsuarioRepository usuarios, Sessao sessao, AuditoriaService auditoria) {
        this.usuarios = usuarios;
        this.sessao = sessao;
        this.auditoria = auditoria;
    }

    public Autorizacao autorizar(String loginSupervisor, String pin, MotivoAutorizacao motivo) {
        Usuario operador = sessao.getAtual();
        if (operador == null) {
            throw new ValidacaoException("É necessário estar autenticado para autorizar.");
        }
        if (motivo == null) {
            throw new ValidacaoException("Informe o motivo da autorização.");
        }
        String login = loginSupervisor == null ? "" : loginSupervisor.strip();
        Optional<UsuarioRepository.CredencialPin> encontrada = usuarios.credencialPin(login);

        String recusa = recusa(encontrada, pin, operador);
        if (recusa != null) {
            auditarNegada(operador, login, motivo, recusa);
            LOG.warn("Autorizacao negada para {} ({}): {}", login, motivo, recusa);
            throw new ValidacaoException("Login ou PIN inválido.");
        }

        UsuarioRepository.CredencialPin supervisor = encontrada.orElseThrow();
        auditoria.registrarComo(supervisor.login(), "AUTORIZACAO_SUPERVISOR", "USUARIO",
                supervisor.id(), "operador " + operador.getLogin() + " autorizado para " + motivo.getDescricao());
        LOG.info("Supervisor {} autorizou {} para {}", supervisor.login(), operador.getLogin(), motivo);
        return new Autorizacao(supervisor.id(), supervisor.login(), supervisor.perfil().getNivel(),
                operador.getId(), operador.getLogin(), motivo);
    }

    public void exigir(Autorizacao autorizacao, MotivoAutorizacao motivo) {
        exigir(sessao.getAtual(), autorizacao, motivo);
    }

    public void exigir(Usuario operador, Autorizacao autorizacao, MotivoAutorizacao motivo) {
        if (operador == null || operador.getPerfil() == PerfilUsuario.ADMIN) {
            return;
        }
        if (autorizacao == null) {
            throw new AutorizacaoNecessariaException(motivo);
        }
        if (autorizacao.operadorId() != operador.getId()) {
            throw new ValidacaoException("A autorização não pertence ao operador da operação.");
        }
        if (autorizacao.motivo() != motivo) {
            throw new ValidacaoException("A autorização não vale para esta operação.");
        }
    }

    private String recusa(Optional<UsuarioRepository.CredencialPin> encontrada, String pin, Usuario operador) {
        if (encontrada.isEmpty()) {
            return "login inexistente";
        }
        UsuarioRepository.CredencialPin supervisor = encontrada.get();
        if (!supervisor.ativo()) {
            return "usuário desativado";
        }
        if (supervisor.pinHash() == null || supervisor.pinSal() == null) {
            return "sem PIN cadastrado";
        }
        if (!Senhas.confere(pin, supervisor.pinSal(), supervisor.pinHash())) {
            return "PIN inválido";
        }
        if (supervisor.id() == operador.getId()) {
            return "o próprio operador não pode autorizar";
        }
        if (!supervisor.perfil().autoriza(operador.getPerfil())) {
            return "perfil sem alçada superior";
        }
        return null;
    }

    private void auditarNegada(Usuario operador, String loginTentado, MotivoAutorizacao motivo, String recusa) {
        String detalhe = "operador " + operador.getLogin() + " tentou " + motivo.getDescricao()
                + " com supervisor " + (loginTentado.isBlank() ? "(vazio)" : loginTentado) + ": " + recusa;
        if (detalhe.length() > 250) {
            detalhe = detalhe.substring(0, 250);
        }
        auditoria.registrar("AUTORIZACAO_NEGADA", "USUARIO", null, detalhe);
    }
}
