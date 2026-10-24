package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.model.ConferenciaCaixa;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;

public class PainelConferenciaCaixa extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JTextField valorConferido = new JTextField(14);
    private final JTextArea observacao = new JTextArea(3, 28);
    private final JLabel saldoEsperado = new JLabel();

    public PainelConferenciaCaixa() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        adicionar(gbc, 0, "Valor em especie contado no caixa *", valorConferido);
        gbc.gridy++;
        adicionar(gbc, gbc.gridy, "Observacao (opcional)", observacao);
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        add(saldoEsperado, gbc);
        saldoEsperado.setVisible(false);
    }

    private void adicionar(GridBagConstraints gbc, int linha, String rotulo, JComponent campo) {
        gbc.gridx = 0;
        gbc.gridy = linha;
        add(Ui.rotulo(rotulo), gbc);
        gbc.gridx = 1;
        add(campo, gbc);
    }

    public void definirValorInicial(String texto) {
        valorConferido.setText(texto == null ? "" : texto);
        valorConferido.requestFocusInWindow();
    }

    public ConferenciaCaixa obterConferencia() {
        BigDecimal valor;
        try {
            valor = Ui.parseMoeda(valorConferido.getText());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Informe um valor valido para o fechamento.");
        }
        String obs = observacao.getText();
        if (obs == null || obs.isBlank()) {
            obs = null;
        }
        return new ConferenciaCaixa(valor, obs);
    }

    public void mostrarSaldoEsperado(BigDecimal valor) {
        saldoEsperado.setText("Saldo esperado em especie: "
                + (valor == null ? "-" : Ui.moeda(valor)));
        saldoEsperado.setVisible(valor != null);
    }
}
