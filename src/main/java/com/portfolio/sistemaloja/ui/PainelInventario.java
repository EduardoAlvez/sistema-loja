package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.model.PerdaEstoque;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.service.ValidacaoException;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

public class PainelInventario extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final String[] MOTIVOS = {"QUEBRA", "VENCIMENTO", "ROUBO", "AVARIA", "OUTROS"};

    private final Aplicacao aplicacao;
    private final JComboBox<Produto> produtos = new JComboBox<>();
    private final JTextField quantidade = new JTextField(8);
    private final JComboBox<String> motivos = new JComboBox<>(MOTIVOS);
    private final JTextField observacao = new JTextField(28);
    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"Data", "Motivo", "Quantidade", "Observação"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tabela = new JTable(modelo);

    public PainelInventario(Aplicacao aplicacao) {
        this.aplicacao = aplicacao;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        montarFormulario();
        add(new JScrollPane(tabela), BorderLayout.CENTER);
        carregarProdutos();
        carregarPerdas();
    }

    private void montarFormulario() {
        JPanel painel = new JPanel(new BorderLayout(10, 10));
        painel.add(Ui.titulo("Inventário - Registrar perda de estoque"), BorderLayout.NORTH);

        JPanel formulario = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        formulario.add(new JLabel("Produto *"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        produtos.addActionListener(e -> carregarPerdas());
        formulario.add(produtos, gbc);

        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.gridx = 0;
        gbc.gridy = 1;
        formulario.add(new JLabel("Quantidade *"), gbc);
        gbc.gridx = 1;
        formulario.add(quantidade, gbc);

        gbc.gridx = 2;
        formulario.add(new JLabel("Motivo *"), gbc);
        gbc.gridx = 3;
        formulario.add(motivos, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        formulario.add(new JLabel("Observação"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        formulario.add(observacao, gbc);

        painel.add(formulario, BorderLayout.CENTER);

        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton registrar = new JButton("Registrar perda");
        registrar.setBackground(Ui.COR_PRINCIPAL);
        registrar.setForeground(Color.WHITE);
        registrar.addActionListener(e -> registrar());
        acoes.add(registrar);
        painel.add(acoes, BorderLayout.SOUTH);

        add(painel, BorderLayout.NORTH);
    }

    private void carregarProdutos() {
        Long selecionado = produtoSelecionadoId();
        DefaultComboBoxModel<Produto> modeloCombo = new DefaultComboBoxModel<>();
        List<Produto> ativos = aplicacao.getProdutos().listarAtivos();
        for (Produto produto : ativos) {
            modeloCombo.addElement(produto);
        }
        produtos.setModel(modeloCombo);
        produtos.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> lista, Object valor,
                                                                 int indice, boolean selecionado, boolean foco) {
                super.getListCellRendererComponent(lista, valor, indice, selecionado, foco);
                if (valor instanceof Produto produto) {
                    setText(produto.getCodigoBarras() + " - " + produto.getNome() + " (estoque: " + produto.getEstoque() + ")");
                }
                return this;
            }
        });
        restaurarSelecao(selecionado);
    }

    private Long produtoSelecionadoId() {
        Produto atual = (Produto) produtos.getSelectedItem();
        return atual == null ? null : atual.getId();
    }

    private void restaurarSelecao(Long produtoId) {
        if (produtoId == null) {
            return;
        }
        for (int i = 0; i < produtos.getItemCount(); i++) {
            Produto produto = produtos.getItemAt(i);
            if (produto.getId().equals(produtoId)) {
                produtos.setSelectedIndex(i);
                return;
            }
        }
    }

    private void registrar() {
        Produto produto = (Produto) produtos.getSelectedItem();
        if (produto == null) {
            Ui.aviso(this, "Selecione um produto.");
            return;
        }
        int quantidadePerda;
        try {
            quantidadePerda = Integer.parseInt(quantidade.getText().trim());
        } catch (NumberFormatException ex) {
            Ui.erro(this, "Informe uma quantidade válida.");
            return;
        }
        Usuario usuario = aplicacao.getUsuarioAtual();
        if (usuario == null) {
            Ui.erro(this, "Nenhum usuário autenticado.");
            return;
        }
        try {
            aplicacao.getInventario().registrarPerda(usuario, produto.getId(), quantidadePerda,
                    (String) motivos.getSelectedItem(), observacao.getText());
            Ui.informacao(this, "Perda registrada com sucesso.");
            quantidade.setText("");
            observacao.setText("");
            carregarProdutos();
            carregarPerdas();
        } catch (ValidacaoException ex) {
            Ui.erro(this, ex.getMessage());
        }
    }

    private void carregarPerdas() {
        modelo.setRowCount(0);
        Produto produto = (Produto) produtos.getSelectedItem();
        if (produto == null) {
            return;
        }
        for (PerdaEstoque perda : aplicacao.getInventario().listarPerdas(produto.getId())) {
            modelo.addRow(new Object[]{
                    perda.getDataHora() == null ? "" : Ui.dataHora(perda.getDataHora().toString()),
                    perda.getMotivo(),
                    perda.getQuantidade(),
                    perda.getObservacao() == null ? "" : perda.getObservacao()
            });
        }
    }
}
