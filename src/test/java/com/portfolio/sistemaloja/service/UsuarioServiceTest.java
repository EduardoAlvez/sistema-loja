package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.PerfilUsuario;
import com.portfolio.sistemaloja.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UsuarioServiceTest {

    private Aplicacao app;

    @BeforeEach
    void preparar() {
        app = BancoTeste.criar("usuarios" + System.nanoTime());
    }

    @Test
    void criaUsuarioComComissaoEntreZeroECem() {
        Usuario vendedor = app.getUsuarios().criar("Vendedor Um", "vendedor1", "senha123",
                PerfilUsuario.OPERADOR, new BigDecimal("7.5"));

        assertEquals(0, new BigDecimal("7.5").compareTo(vendedor.getComissaoPercentual()));
    }

    @Test
    void recusaComissaoForaDoIntervalo() {
        assertThrows(ValidacaoException.class, () -> app.getUsuarios().criar("Negativo", "negativo1",
                "senha123", PerfilUsuario.OPERADOR, new BigDecimal("-1")));
        assertThrows(ValidacaoException.class, () -> app.getUsuarios().criar("Acima", "acima1",
                "senha123", PerfilUsuario.OPERADOR, new BigDecimal("100.01")));
        assertThrows(ValidacaoException.class, () -> app.getUsuarios().criar("Nulo", "nulo1",
                "senha123", PerfilUsuario.OPERADOR, null));
    }

    @Test
    void definirComissaoAtualizaOUsuarioEAuditoria() {
        Usuario operador = app.getAuth().autenticar("operador", "caixa123");
        app.getUsuarios().definirComissao(operador.getId(), new BigDecimal("3"));

        Usuario atualizado = app.getUsuarios().listar().stream()
                .filter(u -> u.getId().equals(operador.getId())).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("3").compareTo(atualizado.getComissaoPercentual()));

        assertThrows(ValidacaoException.class,
                () -> app.getUsuarios().definirComissao(operador.getId(), new BigDecimal("120")));
        assertThrows(ValidacaoException.class,
                () -> app.getUsuarios().definirComissao(9999L, new BigDecimal("3")));

        Usuario aposErros = app.getUsuarios().listar().stream()
                .filter(u -> u.getId().equals(operador.getId())).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("3").compareTo(aposErros.getComissaoPercentual()));
    }
}
