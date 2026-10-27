package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.model.Fornecedor;
import com.portfolio.sistemaloja.service.ValidacaoException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

public class PainelFornecedores extends JPanel {

    private static final long serialVersionUID = 1L;

    private final Aplicacao aplicacao;
    private final JTextField busca = new JTextField(20);
    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"ID", "Nome", "CNPJ", "Telefone", "E-mail"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tabela = new JTable(modelo);

    public PainelFornecedores(Aplicacao aplicacao) {
        this.aplicacao = aplicacao;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        montarCabecalho();
        add(new JScrollPane(tabela), BorderLayout.CENTER);
        montarRodape();
        carregar("");
    }

    private void montarCabecalho() {
        JPanel cabecalho = new JPanel(new BorderLayout(10, 10));
        cabecalho.add(Ui.titulo("Fornecedores"), BorderLayout.WEST);
        JPanel filtro = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filtro.add(new javax.swing.JLabel("Buscar:"));
        busca.addActionListener(e -> carregar(busca.getText()));
        filtro.add(busca);
        JButton filtrar = new JButton("Filtrar");
        filtrar.addActionListener(e -> carregar(busca.getText()));
        filtro.add(filtrar);
        cabecalho.add(filtro, BorderLayout.EAST);
        add(cabecalho, BorderLayout.NORTH);
    }

    private void montarRodape() {
        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton novo = new JButton("Novo");
        novo.addActionListener(e -> novo());
        JButton editar = new JButton("Editar");
        editar.addActionListener(e -> editar());
        JButton remover = new JButton("Remover");
        remover.addActionListener(e -> remover());
        rodape.add(novo);
        rodape.add(editar);
        rodape.add(remover);
        add(rodape, BorderLayout.SOUTH);
    }

    private void carregar(String termo) {
        modelo.setRowCount(0);
        List<Fornecedor> fornecedores = aplicacao.getFornecedores().listar(termo);
        for (Fornecedor fornecedor : fornecedores) {
            modelo.addRow(new Object[]{
                    fornecedor.getId(),
                    fornecedor.getNome(),
                    fornecedor.getCnpj() == null ? "" : fornecedor.getCnpj(),
                    fornecedor.getTelefone() == null ? "" : fornecedor.getTelefone(),
                    fornecedor.getEmail() == null ? "" : fornecedor.getEmail()
            });
        }
    }

    private void novo() {
        DialogoFornecedor dialogo = new DialogoFornecedor(SwingUtilities.getWindowAncestor(this), aplicacao);
        dialogo.setVisible(true);
        if (dialogo.isConfirmado()) {
            carregar(busca.getText());
        }
    }

    private void editar() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) {
            Ui.aviso(this, "Selecione um fornecedor para editar.");
            return;
        }
        long id = (Long) modelo.getValueAt(linha, 0);
        Fornecedor fornecedor = aplicacao.getFornecedores().listar(busca.getText()).stream()
                .filter(f -> id == f.getId())
                .findFirst()
                .orElse(null);
        if (fornecedor == null) {
            Ui.erro(this, "Fornecedor não encontrado.");
            return;
        }
        DialogoFornecedor dialogo = new DialogoFornecedor(SwingUtilities.getWindowAncestor(this), aplicacao, fornecedor);
        dialogo.setVisible(true);
        if (dialogo.isConfirmado()) {
            carregar(busca.getText());
        }
    }

    private void remover() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) {
            Ui.aviso(this, "Selecione um fornecedor para remover.");
            return;
        }
        String nome = String.valueOf(modelo.getValueAt(linha, 1));
        int opcao = JOptionPane.showConfirmDialog(this,
                "Remover o fornecedor " + nome + "?",
                "Confirmar remoção",
                JOptionPane.YES_NO_OPTION);
        if (opcao != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            long id = (Long) modelo.getValueAt(linha, 0);
            aplicacao.getFornecedores().remover(id);
            carregar(busca.getText());
        } catch (ValidacaoException ex) {
            Ui.erro(this, ex.getMessage());
        }
    }
}
