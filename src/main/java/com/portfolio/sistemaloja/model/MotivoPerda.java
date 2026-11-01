package com.portfolio.sistemaloja.model;

/**
 * Motivos pelos quais o estoque de um produto pode ser baixado como perda.
 */
public enum MotivoPerda {
    QUEBRA("Quebra"),
    VENCIMENTO("Vencimento"),
    ROUBO("Roubo"),
    AVARIA("Avaria"),
    OUTROS("Outros");

    private final String descricao;

    MotivoPerda(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /**
     * Motivo "OUTROS" só é aceito com observação, para que a perda fique explicada.
     */
    public boolean exigeObservacao() {
        return this == OUTROS;
    }

    /**
     * Converte o texto informado pelo usuário no motivo correspondente.
     *
     * @return o motivo correspondente ou {@code null} se o texto não corresponder a nenhum motivo válido
     */
    public static MotivoPerda porNome(String nome) {
        if (nome == null) {
            return null;
        }
        String normalizado = nome.trim().toUpperCase();
        for (MotivoPerda motivo : values()) {
            if (motivo.name().equals(normalizado)) {
                return motivo;
            }
        }
        return null;
    }
}
