package com.portfolio.sistemaloja.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class DialogoCupomTermico extends JDialog {

    private static final long serialVersionUID = 1L;

    public DialogoCupomTermico(Frame pai, String conteudo) {
        super(pai, "Cupom térmico 80mm", true);
        setLayout(new BorderLayout(8, 8));
        cast();
        JTextArea area = new JTextArea(conteudo);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setEditable(false);
        area.setCaretPosition(0);

        JScrollPane rolagem = new JScrollPane(area);
        rolagem.setPreferredSize(new Dimension(520, 620));
        add(rolagem, BorderLayout.CENTER);

        JButton salvar = new JButton("Salvar .txt");
        salvar.addActionListener(e -> salvar(conteudo));
        JButton fechar = new JButton("Fechar");
        fechar.addActionListener(e -> dispose());
        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        acoes.add(salvar);
        acoes.add(fechar);
        add(acoes, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(pai);
    }

    private void cast() {
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    private void salvar(String conteudo) {
        JFileChooser escolha = new JFileChooser();
        escolha.setSelectedFile(new java.io.File("cupom-80mm.txt"));
        if (escolha.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path destino = escolha.getSelectedFile().toPath();
        try {
            Files.writeString(destino, conteudo, StandardCharsets.UTF_8);
            Ui.info(this, "Cupom salvo em " + destino + ".");
        } catch (IOException e) {
            Ui.erro(this, "Falha ao salvar o cupom: " + e.getMessage(), e);
        }
    }
}
