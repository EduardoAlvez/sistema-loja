package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.model.Autorizacao;
import com.portfolio.sistemaloja.model.MotivoAutorizacao;
import com.portfolio.sistemaloja.service.AutorizacaoService;
import com.portfolio.sistemaloja.service.ValidacaoException;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class PainelAutorizacao extends JPanel {

    private static final long serialVersionUID = 1L;

    private final AutorizacaoService servico;
    private final MotivoAutorizacao motivo;
    private final JTextField login = new JTextField(14);
    private final JPasswordField pin = new JPasswordField(12);

    public PainelAutorizacao(AutorizacaoService servico, MotivoAutorizacao motivo) {
        this.servico = servico;
        this.motivo = motivo;
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        adicionar(gbc, 0, "Login do supervisor *", login);
        adicionar(gbc, 1, "PIN do supervisor *", pin);
    }

    private void adicionar(GridBagConstraints gbc, int linha, String rotulo, JTextField campo) {
        gbc.gridx = 0;
        gbc.gridy = linha;
        add(Ui.rotulo(rotulo), gbc);
        gbc.gridx = 1;
        add(campo, gbc);
    }

    public void definirLogin(String loginTexto) {
        login.setText(loginTexto == null ? "" : loginTexto);
    }

    public void definirPin(String pinTexto) {
        pin.setText(pinTexto == null ? "" : pinTexto);
    }

    public Autorizacao autorizar() {
        return servico.autorizar(login.getText(), new String(pin.getPassword()), motivo);
    }

    public MotivoAutorizacao motivo() {
        return motivo;
    }
}
