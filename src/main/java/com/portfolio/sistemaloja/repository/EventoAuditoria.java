package com.portfolio.sistemaloja.repository;

public record EventoAuditoria(String dataHora, String usuarioLogin, String acao,
                              String entidade, Long entidadeId, String detalhe) {
}
