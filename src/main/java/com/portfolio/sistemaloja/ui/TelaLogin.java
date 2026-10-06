package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.service.ValidacaoException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;

public class TelaLogin extends JFrame {

    private final transient Aplicacao aplicacao;
    private final JTextField campoLogin = new JTextField(18);
    private final JPasswordField campoSenha = new JPasswordField(18);

    public TelaLogin(Aplicacao aplicacao) {
        this.aplicacao = aplicacao;
        setTitle("Sistema de Gestão de Loja - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        montar();
        pack();
        setLocationRelativeTo(null);
    }

    private void montar() {
        JPanel fundo = new JPanel(new BorderLayout());
        fundo.setBackground(Ui.COR_PRINCIPAL);
        fundo.setBorder(BorderFactory.createEmptyBorder(24, 30, 24, 30));

        JLabel marca = new JLabel("SISTEMA DE GESTÃO DE LOJA");
        marca.setForeground(Color.WHITE);
        marca.setFont(marca.getFont().deriveFont(Font.BOLD, 18f));
        marca.setHorizontalAlignment(JLabel.CENTER);
        fundo.add(marca, BorderLayout.NORTH);

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBackground(Color.WHITE);
        formulario.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 216, 222)),
                BorderFactory.createEmptyBorder(24, 28, 24, 28)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        formulario.add(Ui.rotulo("Login"), gbc);
        gbc.gridy = 1;
        formulario.add(campoLogin, gbc);
        gbc.gridy = 2;
        formulario.add(Ui.rotulo("Senha"), gbc);
        gbc.gridy = 3;
        formulario.add(campoSenha, gbc);

        JButton botaoEntrar = new JButton("Entrar");
        botaoEntrar.setBackground(Ui.COR_PRINCIPAL);
        botaoEntrar.setForeground(Color.WHITE);
        botaoEntrar.setFocusPainted(false);
        botaoEntrar.addActionListener(e -> entrar());

        JButton botaoSair = new JButton("Sair");
        botaoSair.addActionListener(e -> System.exit(0));

        JPanel botoes = new JPanel(new BorderLayout());
        botoes.setBackground(Color.WHITE);
        botoes.add(botaoEntrar, BorderLayout.CENTER);
        botoes.add(botaoSair, BorderLayout.EAST);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        formulario.add(botoes, gbc);

        JLabel dica = new JLabel("Padrão: admin / admin123");
        dica.setForeground(new Color(120, 128, 136));
        dica.setFont(dica.getFont().deriveFont(Font.PLAIN, 11f));
        gbc.gridy = 5;
        formulario.add(dica, gbc);

        JPanel centro = new JPanel(new GridBagLayout());
        centro.setBackground(Ui.COR_PRINCIPAL);
        formulario.setPreferredSize(new Dimension(340, formulario.getPreferredSize().height));
        centro.add(formulario);
        fundo.add(centro, BorderLayout.CENTER);

        setContentPane(fundo);
        getRootPane().setDefaultButton(botaoEntrar);
        getRootPane().getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "sair");
        getRootPane().getActionMap().put("sair", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                System.exit(0);
            }
        });
    }

    private void entrar() {
        try {
            Usuario usuario = aplicacao.getAuth().autenticar(campoLogin.getText(), new String(campoSenha.getPassword()));
            dispose();
            new TelaPrincipal(aplicacao, usuario).setVisible(true);
        } catch (ValidacaoException e) {
            Ui.erro(this, e.getMessage());
            campoSenha.setText("");
            campoSenha.requestFocusInWindow();
        } catch (RuntimeException e) {
            Ui.erro(this, "Falha inesperada ao entrar: " + e.getMessage());
        }
    }
}
