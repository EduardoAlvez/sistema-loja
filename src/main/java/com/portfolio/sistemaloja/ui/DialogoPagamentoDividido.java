package com.portfolio.sistemaloja.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.math.BigDecimal;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import com.portfolio.sistemaloja.model.FormaPagamento;
import com.portfolio.sistemaloja.model.VendaPagamento;

public class DialogoPagamentoDividido extends JDialog {

    private static final long serialVersionUID = 1L;

    private final PainelPagamentoDividido painel;
    private List<VendaPagamento> parcelas;
    private boolean confirmado;

    public DialogoPagamentoDividido(Window proprietario, BigDecimal total, FormaPagamento sugerida) {
        super(proprietario, "Pagamento dividido", Dialog.ModalityType.APPLICATION_MODAL);
        painel = new PainelPagamentoDividido(total, sugerida);
        painel.adicionarParcela(sugerida, total);
        setLayout(new BorderLayout());
        add(painel, BorderLayout.CENTER);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton cancelar = new JButton("Cancelar");
        cancelar.addActionListener(e -> dispose());
        JButton confirmar = new JButton("Confirmar");
        confirmar.setBackground(Ui.COR_OK);
        confirmar.setForeground(Color.WHITE);
        confirmar.setFont(confirmar.getFont().deriveFont(Font.BOLD));
        confirmar.addActionListener(e -> confirmar());
        botoes.add(cancelar);
        botoes.add(confirmar);
        add(botoes, BorderLayout.SOUTH);

        setSize(420, 320);
        setLocationRelativeTo(proprietario);
    }

    private void confirmar() {
        if (!painel.completo()) {
            Ui.erro(this, "Informe parcelas com valor maior que zero que cubram o total da venda.");
            return;
        }
        parcelas = painel.parcelas();
        confirmado = true;
        dispose();
    }

    public boolean isConfirmado() {
        return confirmado;
    }

    public List<VendaPagamento> getParcelas() {
        return parcelas;
    }

    public PainelPagamentoDividido getPainel() {
        return painel;
    }
}
