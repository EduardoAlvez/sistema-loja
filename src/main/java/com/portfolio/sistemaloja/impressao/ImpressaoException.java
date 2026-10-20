package com.portfolio.sistemaloja.impressao;

public class ImpressaoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ImpressaoException(String mensagem) {
        super(mensagem);
    }

    public ImpressaoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
