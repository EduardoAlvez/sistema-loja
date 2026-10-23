package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.model.Autorizacao;
import com.portfolio.sistemaloja.model.MotivoAutorizacao;
import com.portfolio.sistemaloja.service.ValidacaoException;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.Optional;

public class DialogoAutorizacao extends JDialog {

    private static final long serialVersionUID = 1L;

    private final PainelAutorizacao painel;
    private Autorizacao resultado;

    public DialogoAutorizacao(Window proprietario, Aplicacao aplicacao, MotivoAutorizacao motivo) {
        super(proprietario, "Autorização do supervisor", Dialog.ModalityType.APPLICATION_MODAL);
        painel = new PainelAutorizacao(aplicacao.getAutorizacoes(), motivo);
        montar();
        pack();
        setLocationRelativeTo(proprietario);
        setMinimumSize(getPreferredSize());
    }

    private void montar() {
        setLayout(new BorderLayout(10, 10));
        add(painel, BorderLayout.CENTER);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton cancelar = new JButton("Cancelar");
        cancelar.addActionListener(e -> dispose());
        JButton autorizar = new JButton("Autorizar");
        autorizar.setBackground(Ui.COR_PRINCIPAL);
        autorizar.setForeground(Color.WHITE);
        autorizar.addActionListener(e -> autorizar());
        botoes.add(cancelar);
        botoes.add(autorizar);
        add(botoes, BorderLayout.SOUTH);
    }

    private void autorizar() {
        try {
            resultado = painel.autorizar();
            dispose();
        } catch (ValidacaoException e) {
            Ui.erro(this, e.getMessage());
        }
    }

    public Optional<Autorizacao> getResultado() {
        return Optional.ofNullable(resultado);
    }

    public static Optional<Autorizacao> pedir(Window proprietario, Aplicacao aplicacao,
                                              MotivoAutorizacao motivo) {
        DialogoAutorizacao dialogo = new DialogoAutorizacao(proprietario, aplicacao, motivo);
        dialogo.setVisible(true);
        return dialogo.getResultado();
    }
}
