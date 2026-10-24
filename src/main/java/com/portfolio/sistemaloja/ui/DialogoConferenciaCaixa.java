package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.model.ConferenciaCaixa;
import com.portfolio.sistemaloja.model.Caixa;
import com.portfolio.sistemaloja.service.CaixaService;

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

public class DialogoConferenciaCaixa extends JDialog {

    private static final long serialVersionUID = 1L;

    private final PainelConferenciaCaixa painel;
    private ConferenciaCaixa resultado;

    public DialogoConferenciaCaixa(Window proprietario, Aplicacao aplicacao, Caixa caixaAberto) {
        super(proprietario, "Conferencia de caixa", Dialog.ModalityType.APPLICATION_MODAL);
        painel = new PainelConferenciaCaixa();
        CaixaService.Resumo resumo = aplicacao.getCaixa().resumo(caixaAberto);
        montar(resumo);
        pack();
        setLocationRelativeTo(proprietario);
        setMinimumSize(getPreferredSize());
    }

    private void montar(CaixaService.Resumo resumo) {
        setLayout(new BorderLayout(10, 10));
        add(painel, BorderLayout.CENTER);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton cancelar = new JButton("Cancelar");
        cancelar.addActionListener(e -> dispose());
        JButton confirmar = new JButton("Confirmar");
        confirmar.setBackground(Ui.COR_PRINCIPAL);
        confirmar.setForeground(Color.WHITE);
        confirmar.addActionListener(e -> confirmar());
        botoes.add(cancelar);
        botoes.add(confirmar);
        add(botoes, BorderLayout.SOUTH);
    }

    private void confirmar() {
        try {
            resultado = painel.obterConferencia();
            dispose();
        } catch (IllegalArgumentException e) {
            Ui.erro(this, e.getMessage());
        }
    }

    public Optional<ConferenciaCaixa> getResultado() {
        return Optional.ofNullable(resultado);
    }

    public static Optional<ConferenciaCaixa> pedir(Window proprietario, Aplicacao aplicacao,
                                                   Caixa caixaAberto) {
        DialogoConferenciaCaixa dialogo = new DialogoConferenciaCaixa(proprietario, aplicacao, caixaAberto);
        dialogo.setVisible(true);
        return dialogo.getResultado();
    }
}
