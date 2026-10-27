package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.model.Fornecedor;
import com.portfolio.sistemaloja.repository.FornecedorRepository;

import java.util.List;

public class FornecedorService {

    private final FornecedorRepository fornecedores;
    private final AuditoriaService auditoria;

    public FornecedorService(FornecedorRepository fornecedores, AuditoriaService auditoria) {
        this.fornecedores = fornecedores;
        this.auditoria = auditoria;
    }

    public List<Fornecedor> listar(String termo) {
        return fornecedores.listar(termo);
    }

    public List<Fornecedor> listarAtivos() {
        return fornecedores.listarAtivos();
    }

    public Fornecedor salvar(Fornecedor fornecedor) {
        validar(fornecedor);
        if (fornecedores.nomeExiste(fornecedor.getNome(), fornecedor.getId())) {
            throw new ValidacaoException("Já existe um fornecedor com esse nome.");
        }
        Fornecedor salvo = fornecedores.salvar(fornecedor);
        auditoria.registrar("FORNECEDOR_SALVO", "FORNECEDOR", salvo.getId(), salvo.getNome());
        return salvo;
    }

    public void remover(long id) {
        fornecedores.remover(id);
        auditoria.registrar("FORNECEDOR_REMOVIDO", "FORNECEDOR", id, null);
    }

    private void validar(Fornecedor fornecedor) {
        if (fornecedor == null) {
            throw new ValidacaoException("Fornecedor inválido.");
        }
        if (fornecedor.getNome() == null || fornecedor.getNome().strip().isEmpty()) {
            throw new ValidacaoException("Nome do fornecedor é obrigatório.");
        }
        if (fornecedor.getNome().strip().length() > 100) {
            throw new ValidacaoException("Nome do fornecedor deve ter até 100 caracteres.");
        }
        if (fornecedor.getCnpj() != null && !fornecedor.getCnpj().strip().isEmpty()
                && fornecedor.getCnpj().strip().length() > 18) {
            throw new ValidacaoException("CNPJ deve ter até 18 caracteres.");
        }
        if (fornecedor.getTelefone() != null && !fornecedor.getTelefone().strip().isEmpty()
                && fornecedor.getTelefone().strip().length() > 20) {
            throw new ValidacaoException("Telefone deve ter até 20 caracteres.");
        }
        if (fornecedor.getEmail() != null && !fornecedor.getEmail().strip().isEmpty()
                && fornecedor.getEmail().strip().length() > 100) {
            throw new ValidacaoException("E-mail deve ter até 100 caracteres.");
        }
        if (fornecedor.getEndereco() != null && !fornecedor.getEndereco().strip().isEmpty()
                && fornecedor.getEndereco().strip().length() > 150) {
            throw new ValidacaoException("Endereço deve ter até 150 caracteres.");
        }
        fornecedor.setNome(fornecedor.getNome().strip());
    }
}
