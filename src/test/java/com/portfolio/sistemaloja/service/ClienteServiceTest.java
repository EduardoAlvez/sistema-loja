package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.Cliente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClienteServiceTest {

    private Aplicacao app;

    @BeforeEach
    void preparar() {
        app = BancoTeste.criar("cliente" + System.nanoTime());
    }

    @Test
    void salvaClienteComCpfValidoEFormata() {
        Cliente cliente = novo("Maria Silva", "52998224725");
        Cliente salvo = app.getClientes().salvar(cliente);

        assertEquals("529.982.247-25", salvo.getCpf());
        assertTrue(app.getClientes().listar("Maria").size() == 1);
    }

    @Test
    void rejeitaCpfInvalido() {
        assertThrows(ValidacaoException.class,
                () -> app.getClientes().salvar(novo("Sem Documento", "12345678901")));
    }

    @Test
    void rejeitaCpfDuplicado() {
        app.getClientes().salvar(novo("Primeira", "52998224725"));
        assertThrows(ValidacaoException.class,
                () -> app.getClientes().salvar(novo("Segunda", "529.982.247-25")));
    }

    @Test
    void rejeitaNomeVazio() {
        assertThrows(ValidacaoException.class,
                () -> app.getClientes().salvar(novo("   ", "52998224725")));
    }

    @Test
    void rejeitaEmailInvalido() {
        Cliente cliente = novo("Email Ruim", "52998224725");
        cliente.setEmail("nao-eh-email");
        assertThrows(ValidacaoException.class, () -> app.getClientes().salvar(cliente));
    }

    private Cliente novo(String nome, String cpf) {
        Cliente cliente = new Cliente();
        cliente.setNome(nome);
        cliente.setCpf(cpf);
        cliente.setAtivo(true);
        return cliente;
    }
}
