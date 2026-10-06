package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.Sessao;
import com.portfolio.sistemaloja.db.Senhas;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.repository.UsuarioRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

public class AuthService {

    private static final Logger LOG = LogManager.getLogger(AuthService.class);

    private final UsuarioRepository usuarios;
    private final Sessao sessao;

    public AuthService(UsuarioRepository usuarios, Sessao sessao) {
        this.usuarios = usuarios;
        this.sessao = sessao;
    }

    public Usuario autenticar(String login, String senha) {
        if (login == null || login.isBlank()) {
            throw new ValidacaoException("Informe o login.");
        }
        if (senha == null || senha.isBlank()) {
            throw new ValidacaoException("Informe a senha.");
        }
        Optional<Usuario> encontrado = usuarios.buscarPorLogin(login.strip());
        if (encontrado.isEmpty() || !Senhas.confere(senha, encontrado.get().getSal(), encontrado.get().getSenhaHash())) {
            LOG.warn("Tentativa de login invalida para '{}'", login.strip());
            throw new ValidacaoException("Login ou senha inválidos.");
        }
        Usuario usuario = encontrado.get();
        if (!usuario.isAtivo()) {
            LOG.warn("Login '{}' desativado tentou entrar", login.strip());
            throw new ValidacaoException("Usuário desativado. Contate o administrador.");
        }
        LOG.info("Login realizado: {} ({})", usuario.getLogin(), usuario.getPerfil());
        sessao.setAtual(usuario);
        return usuario;
    }
}
