package com.portfolio.sistemaloja.db;

import com.portfolio.sistemaloja.model.PerfilUsuario;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class Migrador {

    private Migrador() {
    }

    public static void executar(ConexaoFonte fonte) throws SQLException {
        String ddl = carregarEsquema();
        try (Connection conexao = fonte.getConnection();
             Statement declaracao = conexao.createStatement()) {
            for (String comando : ddl.split(";")) {
                String sql = comando.strip();
                if (!sql.isEmpty()) {
                    declaracao.execute(sql);
                }
            }
            semear(conexao);
        }
    }

    private static String carregarEsquema() {
        try (InputStream entrada = Migrador.class.getResourceAsStream("/sql/esquema.sql")) {
            if (entrada == null) {
                throw new IllegalStateException("Recurso /sql/esquema.sql não encontrado no classpath");
            }
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao ler o esquema do banco", e);
        }
    }

    private static void semear(Connection conexao) throws SQLException {
        if (existeRegistro(conexao, "SELECT COUNT(*) FROM usuarios")) {
            return;
        }
        inserirUsuario(conexao, "Administrador", "admin", "admin123", PerfilUsuario.ADMIN);
        inserirUsuario(conexao, "Operador Caixa", "operador", "caixa123", PerfilUsuario.OPERADOR);

        try (Statement declaracao = conexao.createStatement()) {
            declaracao.execute("INSERT INTO categorias (nome) VALUES ('Bebidas'), ('Alimentos'), ('Higiene'), ('Limpeza')");

            declaracao.execute("INSERT INTO produtos (codigo_barras, nome, categoria_id, preco_custo, preco_venda, estoque) "
                    + "SELECT '7891000100103', 'Refrigerante Cola 2L', id, 5.50, 8.99, 40 FROM categorias WHERE nome = 'Bebidas'");
            declaracao.execute("INSERT INTO produtos (codigo_barras, nome, categoria_id, preco_custo, preco_venda, estoque) "
                    + "SELECT '7891000100104', 'Água Mineral 500ml', id, 0.80, 2.50, 120 FROM categorias WHERE nome = 'Bebidas'");
            declaracao.execute("INSERT INTO produtos (codigo_barras, nome, categoria_id, preco_custo, preco_venda, estoque) "
                    + "SELECT '7891000200100', 'Arroz Tipo 1 5kg', id, 22.00, 29.90, 25 FROM categorias WHERE nome = 'Alimentos'");
            declaracao.execute("INSERT INTO produtos (codigo_barras, nome, categoria_id, preco_custo, preco_venda, estoque) "
                    + "SELECT '7891000200101', 'Feijão Carioca 1kg', id, 6.20, 8.49, 60 FROM categorias WHERE nome = 'Alimentos'");
            declaracao.execute("INSERT INTO produtos (codigo_barras, nome, categoria_id, preco_custo, preco_venda, estoque) "
                    + "SELECT '7891000300100', 'Sabão em Pó 800g', id, 9.90, 14.99, 30 FROM categorias WHERE nome = 'Limpeza'");
        }

        inserirClientePadrao(conexao);
    }

    private static void inserirUsuario(Connection conexao, String nome, String login, String senha,
                                       PerfilUsuario perfil) throws SQLException {
        String sal = Senhas.gerarSal();
        try (PreparedStatement preparada = conexao.prepareStatement(
                "INSERT INTO usuarios (nome, login, senha_hash, sal, perfil, ativo) VALUES (?, ?, ?, ?, ?, TRUE)")) {
            preparada.setString(1, nome);
            preparada.setString(2, login);
            preparada.setString(3, Senhas.hash(senha, sal));
            preparada.setString(4, sal);
            preparada.setString(5, perfil.name());
            preparada.executeUpdate();
        }
    }

    private static void inserirClientePadrao(Connection conexao) throws SQLException {
        try (PreparedStatement preparada = conexao.prepareStatement(
                "INSERT INTO clientes (nome, cpf, telefone, email, ativo) VALUES (?, ?, ?, ?, TRUE)")) {
            preparada.setString(1, "Consumidor Final");
            preparada.setString(2, "000.000.000-00");
            preparada.setString(3, "");
            preparada.setString(4, "");
            preparada.executeUpdate();
        }
    }

    private static boolean existeRegistro(Connection conexao, String sql) throws SQLException {
        try (Statement declaracao = conexao.createStatement();
             ResultSet resultado = declaracao.executeQuery(sql)) {
            return resultado.next() && resultado.getInt(1) > 0;
        }
    }
}
