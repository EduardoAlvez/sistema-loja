package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.model.Fiado;
import com.portfolio.sistemaloja.model.RecebimentoFiado;
import com.portfolio.sistemaloja.service.FiadoService;
import com.portfolio.sistemaloja.service.ValidacaoException;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.math.BigDecimal;
import java.util.Optional;

public class DialogoRecebimentoFiado extends JDialog {

    private static final long serialVersionUID = 1L;

    private final FiadoService fiados;
    private final Aplicacao aplicacao;
    private final Fiado fiado;
    private final JTextField valorField = new JTextField(12);
    private final JTextField formaField = new JTextField(12);
    private final JTextField observacaoField = new JTextField(25);
    private RecebimentoFiado resultado;

    public DialogoRecebimentoFiado(Window proprietario, Aplicacao aplicacao, Fiado fiado) {
        super(proprietario, "Recebimento de Fiado", Dialog.ModalityType.APPLICATION_MODAL);
        this.fiados = aplicacao.getFiados();
        this.aplicacao = aplicacao;
        this.fiado = fiado;
        montar();
        pack();
        setLocationRelativeTo(proprietario);
        setMinimumSize(getPreferredSize());
    }

    private void montar() {
        setLayout(new BorderLayout(10, 10));

        JPanel painel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        gbc.gridx = 0; gbc.gridy = 0;
        painel.add(Ui.rotulo("Cliente ID:"), gbc);
        gbc.gridx = 1;
        painel.add(Ui.rotulo("" + fiado.clienteId()), gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        painel.add(Ui.rotulo("Valor total:"), gbc);
        gbc.gridx = 1;
        painel.add(Ui.rotulo(Ui.moeda(fiado.valorTotal())), gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        painel.add(Ui.rotulo("Já pago:"), gbc);
        gbc.gridx = 1;
        painel.add(Ui.rotulo(Ui.moeda(fiado.valorPago())), gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        painel.add(Ui.rotulo("A receber:"), gbc);
        gbc.gridx = 1;
        painel.add(Ui.rotulo(Ui.moeda(fiado.valorDevedor())), gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        painel.add(Ui.rotulo("Valor a receber agora:*"), gbc);
        gbc.gridx = 1;
        painel.add(valorField, gbc);

        gbc.gridx = 0; gbc.gridy = 5;
        painel.add(Ui.rotulo("Forma de pagamento:*"), gbc);
        gbc.gridx = 1;
        painel.add(formaField, gbc);

        gbc.gridx = 0; gbc.gridy = 6;
        painel.add(Ui.rotulo("Observação:"), gbc);
        gbc.gridx = 1;
        painel.add(observacaoField, gbc);

        add(painel, BorderLayout.CENTER);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton cancelar = new JButton("Cancelar");
        cancelar.addActionListener(e -> dispose());
        JButton receber = new JButton("Receber");
        receber.setBackground(Ui.COR_PRINCIPAL);
        receber.setForeground(Color.WHITE);
        receber.addActionListener(e -> receber());
        botoes.add(cancelar);
        botoes.add(receber);
        add(botoes, BorderLayout.SOUTH);
    }

    private void receber() {
        try {
            BigDecimal valor;
            try {
                valor = Ui.parseMoeda(valorField.getText());
            } catch (NumberFormatException e) {
                Ui.erro(this, "Informe um valor válido.");
                return;
            }
            String forma = formaField.getText().trim();
            if (forma.isBlank()) {
                Ui.erro(this, "Informe a forma de pagamento.");
                return;
            }
            String obs = observacaoField.getText().trim();
            if (obs.isBlank()) {
                obs = null;
            }

            Long usuarioId = aplicacao.getUsuarioAtual() != null ? aplicacao.getUsuarioAtual().getId() : null;
            RecebimentoFiado recebimento = new RecebimentoFiado(null, fiado.id(), null,
                    valor, forma, usuarioId, obs);
            fiados.receber(fiado.id(), valor, forma, usuarioId, obs);
            resultado = recebimento;
            dispose();
        } catch (ValidacaoException e) {
            Ui.erro(this, e.getMessage());
        }
    }

    public Optional<RecebimentoFiado> getResultado() {
        return Optional.ofNullable(resultado);
    }

    public static Optional<RecebimentoFiado> pedir(Window proprietario, Aplicacao aplicacao,
                                                   Fiado fiado) {
        DialogoRecebimentoFiado dialogo = new DialogoRecebimentoFiado(proprietario, aplicacao, fiado);
        dialogo.setVisible(true);
        return dialogo.getResultado();
    }
}
