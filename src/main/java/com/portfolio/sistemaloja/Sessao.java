package com.portfolio.sistemaloja;

import com.portfolio.sistemaloja.model.Usuario;

public class Sessao {

    private Usuario atual;

    public Usuario getAtual() {
        return atual;
    }

    public void setAtual(Usuario usuario) {
        this.atual = usuario;
    }
}
