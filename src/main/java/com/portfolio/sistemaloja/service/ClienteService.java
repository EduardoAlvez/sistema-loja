package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.model.Cliente;
import com.portfolio.sistemaloja.repository.ClienteRepository;

import java.util.List;

public class ClienteService {

    private final ClienteRepository clientes;
    private final AuditoriaService auditoria;

    public ClienteService(ClienteRepository clientes, AuditoriaService auditoria) {
        this.clientes = clientes;
        this.auditoria = auditoria;
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
        Cliente salvo = clientes.salvar(cliente);
        auditoria.registrar("CLIENTE_SALVO", "CLIENTE", salvo.getId(), salvo.getNome());
        return salvo;
    }

    public void remover(long id) {
        clientes.remover(id);
    }
}
