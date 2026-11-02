package com.portfolio.sistemaloja;

import com.portfolio.sistemaloja.db.Banco;
import com.portfolio.sistemaloja.db.Migrador;
import com.portfolio.sistemaloja.model.Autorizacao;
import com.portfolio.sistemaloja.model.Caixa;
import com.portfolio.sistemaloja.model.MotivoAutorizacao;
import com.portfolio.sistemaloja.model.Usuario;

import java.math.BigDecimal;
import java.time.Clock;

public final class BancoTeste {

    private BancoTeste() {
    }

    private static String url(String nomeBanco) {
        return "jdbc:h2:mem:" + nomeBanco
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000";
    }

    public static java.sql.Connection conexao(String nomeBanco) throws java.sql.SQLException {
        return java.sql.DriverManager.getConnection(url(nomeBanco));
    }

    public static Caixa abrirCaixa(Aplicacao app, Usuario usuario) {
        return app.getCaixa().abrir(new BigDecimal("100.00"), usuario);
    }

    public static Autorizacao autorizarSupervisor(Aplicacao app, MotivoAutorizacao motivo) {
        // Quem solicita e' o operador da sessao corrente; reautentica para garantir o contexto.
        app.getAuth().autenticar("operador", "caixa123");
        return app.getAutorizacoes().autorizar("admin", "1234", motivo);
    }

    public static Aplicacao criar(String nomeBanco) {
        return criar(nomeBanco, Clock.systemDefaultZone());
    }

    public static Aplicacao criar(String nomeBanco, Clock relogio) {
        Banco banco = new Banco(url(nomeBanco), null, null);
        try {
            Migrador.executar(banco);
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Falha ao preparar o banco de teste", e);
        }
        return new Aplicacao(banco, relogio);
    }
}
