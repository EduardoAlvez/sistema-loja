package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.model.MotivoAutorizacao;

public class AutorizacaoNecessariaException extends ValidacaoException {

    private static final long serialVersionUID = 1L;

    private final MotivoAutorizacao motivo;

    public AutorizacaoNecessariaException(MotivoAutorizacao motivo) {
        super("É necessária autorização de um supervisor para " + motivo.getDescricao() + ".");
        this.motivo = motivo;
    }

    public MotivoAutorizacao getMotivo() {
        return motivo;
    }
}
