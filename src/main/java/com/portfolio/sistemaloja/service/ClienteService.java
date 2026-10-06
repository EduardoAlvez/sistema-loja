package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.model.Cliente;
import com.portfolio.sistemaloja.repository.ClienteRepository;

import java.util.List;

public class ClienteService {

    private final ClienteRepository clientes;

    public ClienteService(ClienteRepository clientes) {
        this.clientes = clientes;
    }

    public List<Cliente> listar(String termo) {
        return clientes.listar(termo);
    }

    public List<Cliente> listarAtivos() {
        return clientes.listarAtivos();
    }

    public Cliente salvar(Cliente cliente) {
        if (cliente.getNome() == null || cliente.getNome().isBlank()) {
            throw new ValidacaoException("Nome do cliente é obrigatório.");
        }
        String cpfLimpo = Cpf.limpar(cliente.getCpf());
        if (!Cpf.valido(cpfLimpo)) {
            throw new ValidacaoException("CPF inválido. Confira os dígitos verificadores.");
        }
        if (clientes.cpfExiste(Cpf.formatar(cpfLimpo), cliente.getId())) {
            throw new ValidacaoException("Já existe um cliente com esse CPF.");
        }
        cliente.setCpf(Cpf.formatar(cpfLimpo));
        if (cliente.getEmail() != null && !cliente.getEmail().isBlank()
                && !cliente.getEmail().matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$")) {
            throw new ValidacaoException("E-mail inválido.");
        }
        return clientes.salvar(cliente);
    }

    public void remover(long id) {
        clientes.remover(id);
    }
}
