package com.portfolio.sistemaloja.model;

import java.util.Objects;

public record Autorizacao(long autorizadorId, String autorizadorLogin, int autorizadorNivel,
                          long operadorId, String operadorLogin, MotivoAutorizacao motivo) {

    public Autorizacao {
        Objects.requireNonNull(autorizadorLogin, "autorizadorLogin");
        Objects.requireNonNull(operadorLogin, "operadorLogin");
        Objects.requireNonNull(motivo, "motivo");
    }
}
