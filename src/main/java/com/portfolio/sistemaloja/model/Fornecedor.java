package com.portfolio.sistemaloja.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Fornecedor {

    private Long id;
    private String nome;
    private String cnpj;
    private String telefone;
    private String email;
    private String endereco;
    private boolean ativo = true;
    private LocalDateTime criadoEm;

    public Fornecedor() {
    }

    public Fornecedor(Long id, String nome, String cnpj, String telefone, String email, String endereco,
                      boolean ativo, LocalDateTime criadoEm) {
        this.id = id;
        this.nome = nome;
        this.cnpj = cnpj;
        this.telefone = telefone;
        this.email = email;
        this.endereco = endereco;
        this.ativo = ativo;
        this.criadoEm = criadoEm;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Fornecedor fornecedor)) {
            return false;
        }
        return Objects.equals(id, fornecedor.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return nome == null ? "" : nome;
    }
}
