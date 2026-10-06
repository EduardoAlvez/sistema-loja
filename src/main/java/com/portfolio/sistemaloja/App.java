package com.portfolio.sistemaloja;

import com.formdev.flatlaf.FlatLightLaf;
import com.portfolio.sistemaloja.db.Banco;
import com.portfolio.sistemaloja.db.Migrador;
import com.portfolio.sistemaloja.ui.TelaLogin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.sql.SQLException;

public final class App {

    private static final Logger LOG = LogManager.getLogger(App.class);

    private App() {
    }

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, causa) ->
                LOG.error("Erro nao tratado na thread {}", thread.getName(), causa));
        FlatLightLaf.setup();
        Banco banco = Banco.doAmbiente();
        LOG.info("Aplicacao iniciada — Java {} — banco {}",
                System.getProperty("java.version"), banco.getUrl());
        try {
            banco.testarConexao();
            Migrador.executar(banco);
        } catch (SQLException e) {
            LOG.error("Falha ao conectar ou migrar o banco de dados", e);
            mostrarErroBanco(e);
            return;
        }
        LOG.info("Banco de dados migrado e pronto para uso");
        Aplicacao aplicacao = new Aplicacao(banco);
        SwingUtilities.invokeLater(() -> new TelaLogin(aplicacao).setVisible(true));
    }

    private static void mostrarErroBanco(SQLException e) {
        String mensagem = """
                Não foi possível conectar ao MySQL.

                Verifique se o container está no ar:
                  docker start mysql-sistema-loja

                Conexão padrão: jdbc:mysql://localhost:3306/sistema_loja (root/root)
                Variáveis opcionais: DB_URL, DB_USER, DB_PASS

                Detalhe técnico: %s""".formatted(e.getMessage());
        JOptionPane.showMessageDialog(null, mensagem, "Erro de conexão", JOptionPane.ERROR_MESSAGE);
    }
}
