package com.portfolio.sistemaloja.repository;

public class RepositorioException extends RuntimeException {

    public RepositorioException(String mensagem) {
        super(mensagem);
    }

    public RepositorioException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
