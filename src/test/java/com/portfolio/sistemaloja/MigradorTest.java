package com.portfolio.sistemaloja;

import com.portfolio.sistemaloja.db.Banco;
import com.portfolio.sistemaloja.db.Migrador;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigradorTest {

    @Test
    void aplicaAsMigracoesSemRepetirEmExecucoesSeguidas() throws Exception {
        Banco banco = banco("migrador_repeticao");

        Migrador.executar(banco);
        int aposPrimeira = contarHistorico(banco);
        Migrador.executar(banco);

        assertTrue(aposPrimeira >= 1, "deveria aplicar ao menos a migracao inicial");
        assertEquals(aposPrimeira, contarHistorico(banco),
                "a segunda execucao nao deve aplicar migracoes de novo");
        assertEquals(2, contar(banco, "SELECT COUNT(*) FROM usuarios"),
                "o seed de usuarios deve existir");
        assertEquals(1, contar(banco,
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'clientes'"),
                "o esquema deve ter sido migrado");
    }

    @Test
    void baselinaQuandoOEsquemaJaExisteSemHistorico() throws Exception {
        Banco banco = banco("migrador_baseline");
        executarV1Manualmente(banco);

        Migrador.executar(banco);

        assertEquals(1, contar(banco,
                "SELECT COUNT(*) FROM flyway_schema_history WHERE type = 'BASELINE'"),
                "esquema pre-existente deve receber baseline em vez de reaplicar o V1");
        assertEquals(2, contar(banco, "SELECT COUNT(*) FROM usuarios"),
                "o seed deve ser aplicado sobre o esquema baselinado");
    }

    private static Banco banco(String nome) {
        return new Banco("jdbc:h2:mem:" + nome
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", null, null);
    }

    private static void executarV1Manualmente(Banco banco) throws Exception {
        try (InputStream entrada = MigradorTest.class
                .getResourceAsStream("/db/migration/V1__esquema_inicial.sql")) {
            assertTrue(entrada != null, "migracao V1 nao encontrada no classpath");
            String ddl = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
            try (Connection conexao = banco.getConnection();
                 Statement declaracao = conexao.createStatement()) {
                for (String comando : ddl.split(";")) {
                    if (!comando.strip().isEmpty()) {
                        declaracao.execute(comando.strip());
                    }
                }
            }
        }
    }

    private static int contarHistorico(Banco banco) throws Exception {
        return contar(banco, "SELECT COUNT(*) FROM flyway_schema_history WHERE success = TRUE");
    }

    private static int contar(Banco banco, String sql) throws Exception {
        try (Connection conexao = banco.getConnection();
             Statement declaracao = conexao.createStatement();
             ResultSet resultado = declaracao.executeQuery(sql)) {
            assertTrue(resultado.next(), "consulta deveria retornar uma linha: " + sql);
            return resultado.getInt(1);
        }
    }
}
