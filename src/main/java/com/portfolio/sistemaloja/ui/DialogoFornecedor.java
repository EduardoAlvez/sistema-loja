package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.model.Fornecedor;
import com.portfolio.sistemaloja.service.ValidacaoException;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;

public class DialogoFornecedor extends JDialog {

    private static final long serialVersionUID = 1L;

    private final Aplicacao aplicacao;
    private Fornecedor fornecedor;
    private boolean confirmado;

    private final JTextField nome = new JTextField(30);
    private final JTextField cnpj = new JTextField(18);
    private final JTextField telefone = new JTextField(20);
    private final JTextField email = new JTextField(30);
    private final JTextField endereco = new JTextField(30);

    public DialogoFornecedor(Window proprietario, Aplicacao aplicacao) {
        this(proprietario, aplicacao, null);
    }

    public DialogoFornecedor(Window proprietario, Aplicacao aplicacao, Fornecedor existente) {
        super(proprietario, existente == null ? "Novo fornecedor" : "Editar fornecedor", Dialog.ModalityType.APPLICATION_MODAL);
        this.aplicacao = aplicacao;
        this.fornecedor = existente == null ? new Fornecedor() : copiar(existente);
        montar();
        preencher();
        pack();
        setLocationRelativeTo(proprietario);
    }

    private Fornecedor copiar(Fornecedor existente) {
        Fornecedor f = new Fornecedor();
        f.setId(existente.getId());
        f.setNome(existente.getNome());
        f.setCnpj(existente.getCnpj());
        f.setTelefone(existente.getTelefone());
        f.setEmail(existente.getEmail());
        f.setEndereco(existente.getEndereco());
        f.setAtivo(existente.isAtivo());
        f.setCriadoEm(existente.getCriadoEm());
        return f;
    }

    private void montar() {
        setLayout(new BorderLayout(10, 10));
        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBorder(Ui.bordaVazia(12, 12, 12, 12));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        adicionar(formulario, gbc, 0, "Nome *", nome);
        adicionar(formulario, gbc, 1, "CNPJ", cnpj);
        adicionar(formulario, gbc, 2, "Telefone", telefone);
        adicionar(formulario, gbc, 3, "E-mail", email);
        adicionar(formulario, gbc, 4, "Endereço", endereco);

        add(formulario, BorderLayout.CENTER);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton cancelar = new JButton("Cancelar");
        cancelar.addActionListener(e -> dispose());
        JButton salvar = new JButton("Salvar");
        salvar.setBackground(Ui.COR_PRINCIPAL);
        salvar.setForeground(java.awt.Color.WHITE);
        salvar.addActionListener(e -> salvar());
        botoes.add(cancelar);
        botoes.add(salvar);
        add(botoes, BorderLayout.SOUTH);
    }

    private void adicionar(JPanel painel, GridBagConstraints gbc, int linha, String rotulo, JTextField campo) {
        gbc.gridx = 0;
        gbc.gridy = linha;
        painel.add(new JLabel(rotulo), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        painel.add(campo, gbc);
        gbc.weightx = 0;
    }

    private void preencher() {
        nome.setText(fornecedor.getNome());
        cnpj.setText(fornecedor.getCnpj());
        telefone.setText(fornecedor.getTelefone());
        email.setText(fornecedor.getEmail());
        endereco.setText(fornecedor.getEndereco());
    }

    private void salvar() {
        try {
            fornecedor.setNome(nome.getText());
            fornecedor.setCnpj(cnpj.getText());
            fornecedor.setTelefone(telefone.getText());
            fornecedor.setEmail(email.getText());
            fornecedor.setEndereco(endereco.getText());
            fornecedor.setAtivo(true);
            fornecedor = aplicacao.getFornecedores().salvar(fornecedor);
            confirmado = true;
            dispose();
        } catch (ValidacaoException ex) {
            Ui.erro(this, ex.getMessage());
        }
    }

    public boolean isConfirmado() {
        return confirmado;
    }

    public Fornecedor getFornecedor() {
        return fornecedor;
    }
}
