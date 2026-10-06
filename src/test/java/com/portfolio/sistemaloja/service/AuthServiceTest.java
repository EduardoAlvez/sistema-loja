package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {

    private Aplicacao app;

    @BeforeEach
    void preparar() {
        app = BancoTeste.criar("auth" + System.nanoTime());
    }

    @Test
    void autenticaUsuarioPadraoCriadoNaMigracao() {
        Usuario admin = app.getAuth().autenticar("admin", "admin123");
        assertEquals("ADMIN", admin.getPerfil().name());
        assertTrue(admin.isAtivo());
    }

    @Test
    void guardaUsuarioNaSessaoAposLogin() {
        assertNull(app.getUsuarioAtual());

        app.getAuth().autenticar("admin", "admin123");

        assertEquals("admin", app.getUsuarioAtual().getLogin());
    }

    @Test
    void rejeitaSenhaErrada() {
        assertThrows(ValidacaoException.class, () -> app.getAuth().autenticar("admin", "senhaerrada"));
    }

    @Test
    void rejeitaLoginInexistente() {
        assertThrows(ValidacaoException.class, () -> app.getAuth().autenticar("naoexiste", "qualquer"));
    }

    @Test
    void rejeitaCamposVazios() {
        assertThrows(ValidacaoException.class, () -> app.getAuth().autenticar("  ", "x"));
        assertThrows(ValidacaoException.class, () -> app.getAuth().autenticar("admin", " "));
    }

    @Test
    void criaUsuarioNovoEAutenticaComANovaSenha() {
        app.getUsuarios().criar("Vendedor Novo", "vendedor", "senha123",
                com.portfolio.sistemaloja.model.PerfilUsuario.OPERADOR);
        Usuario criado = app.getAuth().autenticar("vendedor", "senha123");
        assertEquals("Vendedor Novo", criado.getNome());
    }

    @Test
    void naoPermiteLoginDuplicado() {
        assertThrows(ValidacaoException.class, () -> app.getUsuarios()
                .criar("Outro Admin", "admin", "senha123", com.portfolio.sistemaloja.model.PerfilUsuario.ADMIN));
    }

    @Test
    void naoPermiteSenhaCurta() {
        assertThrows(ValidacaoException.class, () -> app.getUsuarios()
                .criar("Fraco", "fraco", "123", com.portfolio.sistemaloja.model.PerfilUsuario.OPERADOR));
    }
}
