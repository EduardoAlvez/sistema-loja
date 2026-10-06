package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.model.Cliente;
import com.portfolio.sistemaloja.service.Cpf;
import com.portfolio.sistemaloja.service.ValidacaoException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class DialogoCliente extends JDialog {

    private final transient Aplicacao aplicacao;
    private final Cliente cliente;
    private final Runnable aoSalvar;

    private final JTextField nome = new JTextField(22);
    private final JTextField cpf = new JTextField(14);
    private final JTextField telefone = new JTextField(14);
    private final JTextField email = new JTextField(22);
    private final JCheckBox ativo = new JCheckBox("Cliente ativo");

    public DialogoCliente(java.awt.Window pai, Aplicacao aplicacao, Cliente cliente, Runnable aoSalvar) {
        super(pai, cliente.getId() == null ? "Novo cliente" : "Editar cliente", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        this.aplicacao = aplicacao;
        this.cliente = cliente;
        this.aoSalvar = aoSalvar;
        montar();
        preencher();
        pack();
        setLocationRelativeTo(pai);
        setResizable(false);
    }

    private void montar() {
        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.anchor = GridBagConstraints.WEST;

        adicionar(formulario, gbc, 0, "Nome *", nome);
        adicionar(formulario, gbc, 1, "CPF *", cpf);
        adicionar(formulario, gbc, 2, "Telefone", telefone);
        adicionar(formulario, gbc, 3, "E-mail", email);
        gbc.gridx = 1;
        gbc.gridy = 4;
        formulario.add(ativo, gbc);

        JButton salvar = new JButton("Salvar");
        salvar.setBackground(Ui.COR_PRINCIPAL);
        salvar.setForeground(java.awt.Color.WHITE);
        salvar.addActionListener(e -> salvar());
        JButton cancelar = new JButton("Cancelar");
        cancelar.addActionListener(e -> dispose());

        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        rodape.add(cancelar);
        rodape.add(salvar);
        getRootPane().setDefaultButton(salvar);

        setLayout(new BorderLayout());
        add(formulario, BorderLayout.CENTER);
        add(rodape, BorderLayout.SOUTH);
    }

    private void adicionar(JPanel painel, GridBagConstraints gbc, int linha, String rotulo, JTextField campo) {
        gbc.gridx = 0;
        gbc.gridy = linha;
        painel.add(Ui.rotulo(rotulo), gbc);
        gbc.gridx = 1;
        painel.add(campo, gbc);
    }

    private void preencher() {
        nome.setText(cliente.getNome());
        cpf.setText(cliente.getCpf());
        telefone.setText(cliente.getTelefone());
        email.setText(cliente.getEmail());
        ativo.setSelected(cliente.isAtivo());
    }

    private void salvar() {
        try {
            cliente.setNome(nome.getText().strip());
            cliente.setCpf(cpf.getText().strip());
            cliente.setTelefone(telefone.getText().strip());
            cliente.setEmail(email.getText().strip());
            cliente.setAtivo(ativo.isSelected());
            aplicacao.getClientes().salvar(cliente);
            aoSalvar.run();
            dispose();
        } catch (ValidacaoException e) {
            Ui.erro(this, e.getMessage());
        } catch (RuntimeException e) {
            Ui.erro(this, "Falha ao salvar: " + e.getMessage());
        }
    }
}
