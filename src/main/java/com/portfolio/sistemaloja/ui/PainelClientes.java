package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.model.Cliente;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

public class PainelClientes extends JPanel {

    private final transient Aplicacao aplicacao;
    private final JTextField busca = new JTextField(24);
    private final javax.swing.table.DefaultTableModel modelo =
            Ui.modeloTabela("Nome", "CPF", "Telefone", "E-mail", "Situação");
    private final JTable tabela = Ui.tabela(modelo);
    private List<Cliente> linhas = List.of();

    public PainelClientes(Aplicacao aplicacao) {
        this.aplicacao = aplicacao;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setBackground(Ui.COR_FUNDO);
        add(montarTopo(), BorderLayout.NORTH);
        add(new JScrollPane(tabela), BorderLayout.CENTER);
        atualizar();
    }

    private JPanel montarTopo() {
        JPanel topo = new JPanel(new BorderLayout());
        topo.setOpaque(false);

        JPanel titulo = new JPanel(new BorderLayout());
        titulo.setOpaque(false);
        titulo.add(Ui.titulo("Clientes"), BorderLayout.WEST);

        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acoes.setOpaque(false);
        JButton novo = new JButton("Novo");
        novo.setBackground(Ui.COR_PRINCIPAL);
        novo.setForeground(java.awt.Color.WHITE);
        novo.addActionListener(e -> editar(null));
        JButton editar = new JButton("Editar");
        editar.addActionListener(e -> editar(selecionado()));
        JButton excluir = new JButton("Excluir");
        excluir.setBackground(Ui.COR_ALERTA);
        excluir.setForeground(java.awt.Color.WHITE);
        excluir.addActionListener(e -> excluir());
        JButton atualizar = new JButton("Atualizar");
        atualizar.addActionListener(e -> atualizar());
        acoes.add(novo);
        acoes.add(editar);
        acoes.add(excluir);
        acoes.add(atualizar);
        titulo.add(acoes, BorderLayout.EAST);

        JPanel linhaBusca = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        linhaBusca.setOpaque(false);
        linhaBusca.add(Ui.rotulo("Buscar:"));
        linhaBusca.add(busca);
        busca.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    atualizar();
                }
            }
        });

        topo.add(titulo, BorderLayout.NORTH);
        topo.add(linhaBusca, BorderLayout.CENTER);
        return topo;
    }

    public final void atualizar() {
        linhas = aplicacao.getClientes().listar(busca.getText());
        modelo.setRowCount(0);
        for (Cliente cliente : linhas) {
            modelo.addRow(new Object[]{
                    cliente.getNome(),
                    cliente.getCpf(),
                    cliente.getTelefone(),
                    cliente.getEmail(),
                    cliente.isAtivo() ? "Ativo" : "Inativo"
            });
        }
    }

    private Cliente selecionado() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) {
            Ui.info(this, "Selecione um cliente na tabela.");
            return null;
        }
        return linhas.get(tabela.convertRowIndexToModel(linha));
    }

    private void editar(Cliente cliente) {
        Cliente alvo = cliente == null ? new Cliente() : cliente;
        if (cliente == null) {
            alvo.setAtivo(true);
        }
        new DialogoCliente(javax.swing.SwingUtilities.getWindowAncestor(this), aplicacao, alvo, this::atualizar)
                .setVisible(true);
    }

    private void excluir() {
        Cliente cliente = selecionado();
        if (cliente == null) {
            return;
        }
        if (!Ui.confirma(this, "Excluir o cliente \"" + cliente.getNome() + "\"?")) {
            return;
        }
        try {
            aplicacao.getClientes().remover(cliente.getId());
            atualizar();
        } catch (RuntimeException e) {
            Ui.erro(this, e.getMessage());
        }
    }
}
