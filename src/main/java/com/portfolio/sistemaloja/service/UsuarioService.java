package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.db.Senhas;
import com.portfolio.sistemaloja.model.PerfilUsuario;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.repository.UsuarioRepository;

import java.math.BigDecimal;
import java.util.List;

public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final AuditoriaService auditoria;

    public UsuarioService(UsuarioRepository usuarios, AuditoriaService auditoria) {
        this.usuarios = usuarios;
        this.auditoria = auditoria;
    }

    public List<Usuario> listar() {
        return usuarios.listar();
    }

    public Usuario criar(String nome, String login, String senha, PerfilUsuario perfil) {
        return criar(nome, login, senha, perfil, BigDecimal.ZERO);
    }

    public Usuario criar(String nome, String login, String senha, PerfilUsuario perfil,
                         BigDecimal comissaoPercentual) {
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
        validarComissao(comissaoPercentual);
        if (usuarios.loginExiste(login.strip(), null)) {
            throw new ValidacaoException("Já existe um usuário com esse login.");
        }
        Usuario usuario = new Usuario(null, nome.strip(), login.strip(), perfil, true);
        usuario.setComissaoPercentual(comissaoPercentual);
        String sal = Senhas.gerarSal();
        usuario.setSal(sal);
        usuario.setSenhaHash(Senhas.hash(senha, sal));
        Usuario criado = usuarios.salvar(usuario);
        auditoria.registrar("USUARIO_CRIADO", "USUARIO", criado.getId(), criado.getLogin());
        return criado;
    }

    public Usuario definirPin(long usuarioId, String pin) {
        if (pin == null || !pin.matches("\\d{4,6}")) {
            throw new ValidacaoException("O PIN deve ter de 4 a 6 dígitos.");
        }
        Usuario usuario = usuarios.buscarPorId(usuarioId)
                .orElseThrow(() -> new ValidacaoException("Usuário não encontrado."));
        String sal = Senhas.gerarSal();
        usuarios.definirPin(usuarioId, Senhas.hash(pin, sal), sal);
        auditoria.registrar("PIN_DEFINIDO", "USUARIO", usuarioId, usuario.getLogin());
        return usuario;
    }

    public Usuario definirComissao(long usuarioId, BigDecimal comissaoPercentual) {
        validarComissao(comissaoPercentual);
        Usuario usuario = usuarios.buscarPorId(usuarioId)
                .orElseThrow(() -> new ValidacaoException("Usuário não encontrado."));
        usuario.setComissaoPercentual(comissaoPercentual);
        Usuario salvo = usuarios.salvar(usuario);
        auditoria.registrar("COMISSAO_DEFINIDA", "USUARIO", usuarioId,
                salvo.getLogin() + " comissao " + salvo.getComissaoPercentual() + "%");
        return salvo;
    }

    private void validarComissao(BigDecimal comissaoPercentual) {
        if (comissaoPercentual == null) {
            throw new ValidacaoException("Informe a comissão do usuário.");
        }
        if (comissaoPercentual.compareTo(BigDecimal.ZERO) < 0
                || comissaoPercentual.compareTo(new BigDecimal("100")) > 0) {
            throw new ValidacaoException("A comissão deve estar entre 0 e 100.");
        }
    }
}
