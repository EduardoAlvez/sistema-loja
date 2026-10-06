package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.db.Senhas;
import com.portfolio.sistemaloja.model.PerfilUsuario;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.repository.UsuarioRepository;

import java.util.List;

public class UsuarioService {

    private final UsuarioRepository usuarios;

    public UsuarioService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    public List<Usuario> listar() {
        return usuarios.listar();
    }

    public Usuario criar(String nome, String login, String senha, PerfilUsuario perfil) {
        if (nome == null || nome.isBlank()) {
            throw new ValidacaoException("Nome é obrigatório.");
        }
        if (login == null || login.isBlank()) {
            throw new ValidacaoException("Login é obrigatório.");
        }
        if (senha == null || senha.length() < 6) {
            throw new ValidacaoException("A senha deve ter pelo menos 6 caracteres.");
        }
        if (perfil == null) {
            throw new ValidacaoException("Selecione o perfil do usuário.");
        }
        if (usuarios.loginExiste(login.strip(), null)) {
            throw new ValidacaoException("Já existe um usuário com esse login.");
        }
        Usuario usuario = new Usuario(null, nome.strip(), login.strip(), perfil, true);
        String sal = Senhas.gerarSal();
        usuario.setSal(sal);
        usuario.setSenhaHash(Senhas.hash(senha, sal));
        return usuarios.salvar(usuario);
    }
}
