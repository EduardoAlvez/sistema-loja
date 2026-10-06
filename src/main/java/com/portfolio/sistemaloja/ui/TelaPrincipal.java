package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.model.Usuario;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class TelaPrincipal extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final transient Aplicacao aplicacao;
    private final transient Usuario usuario;
    private final JLabel relogio = new JLabel();

    public TelaPrincipal(Aplicacao aplicacao, Usuario usuario) {
        this.aplicacao = aplicacao;
        this.usuario = usuario;
        setTitle("Sistema de Gestão de Loja - " + usuario.getNome());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 720));
        setLocationRelativeTo(null);
        montar();
        iniciarRelogio();
    }

    private void montar() {
        JTabbedPane abas = new JTabbedPane(JTabbedPane.LEFT);
        abas.setFont(abas.getFont().deriveFont(13f));
        abas.addTab("Dashboard", new PainelDashboard(aplicacao));
        abas.addTab("PDV", new PainelPDV(aplicacao, usuario));
        abas.addTab("Produtos", new PainelProdutos(aplicacao));
        abas.addTab("Clientes", new PainelClientes(aplicacao));
        abas.addTab("Relatórios", new PainelRelatorios(aplicacao));
        add(abas, BorderLayout.CENTER);
        setJMenuBar(criarMenu(abas));
        add(criarBarraStatus(), BorderLayout.SOUTH);
    }

    private JMenuBar criarMenu(JTabbedPane abas) {
        JMenuBar barra = new JMenuBar();

        JMenu sistema = new JMenu("Sistema");
        JMenuItem trocar = new JMenuItem("Trocar de usuário");
        trocar.addActionListener(e -> {
            dispose();
            new TelaLogin(aplicacao).setVisible(true);
        });
        JMenuItem sair = new JMenuItem("Sair");
        sair.addActionListener(e -> System.exit(0));
        sistema.add(trocar);
        sistema.addSeparator();
        sistema.add(sair);

        JMenu cadastros = new JMenu("Cadastros");
        JMenuItem usuarios = new JMenuItem("Usuários...");
        usuarios.setEnabled(usuario.isAdmin());
        usuarios.addActionListener(e -> abrirDialogo(new DialogoUsuario(aplicacao, this)));
        JMenuItem produtos = new JMenuItem("Produtos");
        produtos.addActionListener(e -> abas.setSelectedIndex(2));
        JMenuItem clientes = new JMenuItem("Clientes");
        clientes.addActionListener(e -> abas.setSelectedIndex(3));
        cadastros.add(usuarios);
        cadastros.add(produtos);
        cadastros.add(clientes);

        barra.add(sistema);
        barra.add(cadastros);
        return barra;
    }

    private void abrirDialogo(java.awt.Dialog dialogo) {
        dialogo.setVisible(true);
    }

    private JPanel criarBarraStatus() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(Ui.COR_ESCURA);
        barra.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 12, 5, 12));

        JLabel usuarioLabel = new JLabel(usuario.getNome() + "  |  " + usuario.getPerfil().getDescricao());
        usuarioLabel.setForeground(Color.WHITE);
        usuarioLabel.setFont(usuarioLabel.getFont().deriveFont(12f));

        relogio.setForeground(Color.WHITE);
        relogio.setFont(relogio.getFont().deriveFont(12f));

        barra.add(usuarioLabel, BorderLayout.WEST);
        barra.add(relogio, BorderLayout.EAST);
        return barra;
    }

    private void iniciarRelogio() {
        javax.swing.Timer timer = new javax.swing.Timer(30000, e -> relogio.setText(LocalTime.now().format(HORA)));
        timer.start();
        relogio.setText(LocalTime.now().format(HORA));
        SwingUtilities.invokeLater(() -> relogio.setText(LocalTime.now().format(HORA)));
    }
}
