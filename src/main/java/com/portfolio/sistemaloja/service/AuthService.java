package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.db.Senhas;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.repository.UsuarioRepository;

import java.util.Optional;

public class AuthService {

    private final UsuarioRepository usuarios;

    public AuthService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
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
            throw new ValidacaoException("Login ou senha inválidos.");
        }
        Usuario usuario = encontrado.get();
        if (!usuario.isAtivo()) {
            throw new ValidacaoException("Usuário desativado. Contate o administrador.");
        }
        return usuario;
    }
}
