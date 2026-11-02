package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.model.ContagemItem;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.Optional;

/**
 * Registra a quantidade contada de um item da contagem.
 *
 * <p>O diálogo apenas anota o que foi visto na prateleira: o estoque só muda quando a
 * contagem é fechada. A diferença prevista é mostrada em tempo real para que o operador
 * confira a leitura antes de confirmar.
 */
public class DialogoContagemItem extends JDialog {

    private static final long serialVersionUID = 1L;

    private final ContagemItem item;
    private final int estoqueAtual;
    private final JTextField quantidadeField = new JTextField(10);
    private final JLabel previsaoLabel = new JLabel(" ");
    private Integer resultado;

    public DialogoContagemItem(Window proprietario, ContagemItem item, int estoqueAtual) {
        super(proprietario, "Contagem de item", Dialog.ModalityType.APPLICATION_MODAL);
        this.item = item;
        this.estoqueAtual = estoqueAtual;
        montar();
        pack();
        setLocationRelativeTo(proprietario);
        setMinimumSize(getPreferredSize());
        javax.swing.SwingUtilities.invokeLater(quantidadeField::requestFocusInWindow);
    }

    private void montar() {
        setLayout(new BorderLayout(10, 10));

        JPanel painel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        gbc.gridx = 0;
        gbc.gridy = 0;
        painel.add(Ui.rotulo("Produto:"), gbc);
        gbc.gridx = 1;
        painel.add(Ui.rotulo(item.getProdutoNome() + " (" + item.getProdutoCodigoBarras() + ")"), gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        painel.add(Ui.rotulo("Estoque no sistema:"), gbc);
        gbc.gridx = 1;
        painel.add(Ui.rotulo(String.valueOf(estoqueAtual)), gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        painel.add(Ui.rotulo("Estoque na abertura da contagem:"), gbc);
        gbc.gridx = 1;
        painel.add(Ui.rotulo(String.valueOf(item.getEstoqueSistema())), gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        painel.add(Ui.rotulo("Quantidade contada:*", quantidadeField), gbc);
        gbc.gridx = 1;
        painel.add(quantidadeField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        painel.add(Ui.rotulo("Diferença prevista:"), gbc);
        gbc.gridx = 1;
        previsaoLabel.setFont(previsaoLabel.getFont().deriveFont(java.awt.Font.BOLD, 13f));
        painel.add(previsaoLabel, gbc);

        add(painel, BorderLayout.CENTER);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton cancelar = new JButton("Cancelar");
        cancelar.addActionListener(e -> dispose());
        JButton registrar = new JButton("Registrar");
        registrar.setBackground(Ui.COR_PRINCIPAL);
        registrar.setForeground(Color.WHITE);
        registrar.addActionListener(e -> registrar());
        botoes.add(cancelar);
        botoes.add(registrar);
        add(botoes, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(registrar);
        getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), "sair");
        getRootPane().getActionMap().put("sair", new javax.swing.AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                dispose();
            }
        });

        quantidadeField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                atualizarPrevisao();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                atualizarPrevisao();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                atualizarPrevisao();
            }
        });
        atualizarPrevisao();
    }

    private void atualizarPrevisao() {
        String texto = quantidadeField.getText().trim();
        if (texto.isEmpty()) {
            previsaoLabel.setText("Informe a quantidade contada.");
            return;
        }
        int quantidade;
        try {
            quantidade = Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            previsaoLabel.setText("Quantidade inválida.");
            return;
        }
        if (quantidade < 0) {
            previsaoLabel.setText("A quantidade contada não pode ser negativa.");
            return;
        }
        int previsao = quantidade - estoqueAtual;
        if (previsao == 0) {
            previsaoLabel.setText("Sem divergência: o estoque permanece " + estoqueAtual + ".");
        } else if (previsao > 0) {
            previsaoLabel.setText("Divergência de +" + previsao + ": o estoque será ajustado para "
                    + quantidade + ".");
        } else {
            previsaoLabel.setText("Divergência de " + previsao + ": o estoque será ajustado para "
                    + quantidade + ".");
        }
    }

    private void registrar() {
        String texto = quantidadeField.getText().trim();
        int quantidade;
        try {
            quantidade = Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            Ui.erro(this, "Informe uma quantidade válida.");
            return;
        }
        if (quantidade < 0) {
            Ui.erro(this, "A quantidade contada não pode ser negativa.");
            return;
        }
        resultado = quantidade;
        dispose();
    }

    public Optional<Integer> getResultado() {
        return Optional.ofNullable(resultado);
    }

    public static Optional<Integer> pedir(Window proprietario, ContagemItem item, int estoqueAtual) {
        DialogoContagemItem dialogo = new DialogoContagemItem(proprietario, item, estoqueAtual);
        dialogo.setVisible(true);
        return dialogo.getResultado();
    }
}
