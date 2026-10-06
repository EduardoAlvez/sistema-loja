package com.portfolio.sistemaloja.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

public class Banco implements ConexaoFonte {

    private final String url;
    private final String usuario;
    private final String senha;

    public Banco(String url, String usuario, String senha) {
        this.url = Objects.requireNonNull(url, "url");
        this.usuario = usuario;
        this.senha = senha;
    }

    public static Banco doAmbiente() {
        String url = System.getenv().getOrDefault("DB_URL",
                "jdbc:mysql://localhost:3306/sistema_loja?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        String usuario = System.getenv().getOrDefault("DB_USER", "root");
        String senha = System.getenv().getOrDefault("DB_PASS", "root");
        return new Banco(url, usuario, senha);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, usuario, senha);
    }

    public void testarConexao() throws SQLException {
        try (Connection conexao = getConnection()) {
            if (conexao == null || conexao.isClosed()) {
                throw new SQLException("Conexão recusada pelo banco de dados.");
            }
        }
    }
}
