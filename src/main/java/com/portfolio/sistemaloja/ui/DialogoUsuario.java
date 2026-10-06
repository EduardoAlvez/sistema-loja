package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.model.PerfilUsuario;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.service.ValidacaoException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

public class DialogoUsuario extends JDialog {

    private static final long serialVersionUID = 1L;

    private final transient Aplicacao aplicacao;
    private final JTextField nome = new JTextField(18);
    private final JTextField login = new JTextField(14);
    private final JPasswordField senha = new JPasswordField(14);
    private final JComboBox<PerfilUsuario> perfil = new JComboBox<>(PerfilUsuario.values());
    private final javax.swing.table.DefaultTableModel modelo =
            Ui.modeloTabela("Nome", "Login", "Perfil", "Situação");
    private final JTable tabela = Ui.tabela(modelo);
    private List<Usuario> usuarios = List.of();

    public DialogoUsuario(Aplicacao aplicacao, Frame pai) {
        super(pai, "Gerenciar usuários", true);
        this.aplicacao = aplicacao;
        montar();
        pack();
        setLocationRelativeTo(pai);
        setMinimumSize(getPreferredSize());
    }

    private void montar() {
        setLayout(new BorderLayout(10, 10));

        JPanel topo = new JPanel(new BorderLayout());
        topo.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));
        topo.add(Ui.titulo("Usuários"), BorderLayout.NORTH);

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBorder(BorderFactory.createLineBorder(new Color(226, 230, 234)));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.anchor = GridBagConstraints.WEST;

        adicionar(formulario, gbc, 0, "Nome *", nome);
        adicionar(formulario, gbc, 1, "Login *", login);
        adicionar(formulario, gbc, 2, "Senha * (mín. 6)", senha);
        gbc.gridx = 0;
        gbc.gridy = 3;
        formulario.add(Ui.rotulo("Perfil *"), gbc);
        gbc.gridx = 1;
        formulario.add(perfil, gbc);

        JButton criar = new JButton("Criar usuário");
        criar.setBackground(Ui.COR_PRINCIPAL);
        criar.setForeground(java.awt.Color.WHITE);
        criar.addActionListener(e -> criar());

        JPanel linhaAcao = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        linhaAcao.add(criar);
        topo.add(formulario, BorderLayout.CENTER);
        topo.add(linhaAcao, BorderLayout.SOUTH);

        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        centro.add(new JScrollPane(tabela), BorderLayout.CENTER);
        JLabel aviso = Ui.rotulo("Para redefinir a senha de um usuário, desative-o e crie um novo login.");
        aviso.setForeground(new Color(110, 118, 126));
        centro.add(aviso, BorderLayout.SOUTH);

        add(topo, BorderLayout.NORTH);
        add(centro, BorderLayout.CENTER);
        atualizar();
    }

    private void adicionar(JPanel painel, GridBagConstraints gbc, int linha, String rotulo, JTextField campo) {
        gbc.gridx = 0;
        gbc.gridy = linha;
        painel.add(Ui.rotulo(rotulo), gbc);
        gbc.gridx = 1;
        painel.add(campo, gbc);
    }

    private void criar() {
        try {
            aplicacao.getUsuarios().criar(
                    nome.getText(),
                    login.getText(),
                    new String(senha.getPassword()),
                    (PerfilUsuario) perfil.getSelectedItem());
            nome.setText("");
            login.setText("");
            senha.setText("");
            atualizar();
            Ui.info(this, "Usuário criado com sucesso.");
        } catch (ValidacaoException e) {
            Ui.erro(this, e.getMessage());
        } catch (RuntimeException e) {
            Ui.erro(this, "Falha ao criar usuário: " + e.getMessage(), e);
        }
    }

    private void atualizar() {
        usuarios = aplicacao.getUsuarios().listar();
        modelo.setRowCount(0);
        for (Usuario usuario : usuarios) {
            modelo.addRow(new Object[]{
                    usuario.getNome(),
                    usuario.getLogin(),
                    usuario.getPerfil().getDescricao(),
                    usuario.isAtivo() ? "Ativo" : "Inativo"
            });
        }
    }
}
