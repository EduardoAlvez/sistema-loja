package com.portfolio.sistemaloja;

import com.portfolio.sistemaloja.db.ConexaoFonte;
import com.portfolio.sistemaloja.db.Migrador;

import java.sql.DriverManager;

public final class BancoTeste {

    private BancoTeste() {
    }

    public static Aplicacao criar(String nomeBanco) {
        String url = "jdbc:h2:mem:" + nomeBanco
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        ConexaoFonte fonte = () -> DriverManager.getConnection(url);
        try {
            Migrador.executar(fonte);
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Falha ao preparar o banco de teste", e);
        }
        return new Aplicacao(fonte);
    }
}
