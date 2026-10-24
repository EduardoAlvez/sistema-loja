package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.service.ProdutoService;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Optional;

public class DialogoBuscaProduto extends JDialog {

    private static final long serialVersionUID = 1L;

    private final transient Aplicacao aplicacao;
    private final transient ProdutoService produtos;
    private final JTextField busca = new JTextField(24);
    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"Código", "Nome", "Estoque", "Preço"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tabela = new JTable(modelo);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(modelo);
    private Produto produtoSelecionado = null;

    public DialogoBuscaProduto(Window proprietario, Aplicacao aplicacao) {
        super(proprietario, "Buscar produto", Dialog.ModalityType.APPLICATION_MODAL);
        this.aplicacao = aplicacao;
        this.produtos = aplicacao.getProdutos();
        montar();
        pack();
        setLocationRelativeTo(proprietario);
        setMinimumSize(getPreferredSize());
        busca.requestFocusInWindow();
    }

    private void montar() {
        setLayout(new BorderLayout(10, 10));

        JPanel topo = new JPanel(new BorderLayout(8, 0));
        topo.setOpaque(false);
        topo.add(Ui.titulo("Buscar produto"), BorderLayout.WEST);

        JPanel buscaLinha = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buscaLinha.setOpaque(false);
        buscaLinha.add(Ui.rotulo("Buscar (código ou nome):"));
        buscaLinha.add(busca);
        topo.add(buscaLinha, BorderLayout.CENTER);

        add(topo, BorderLayout.NORTH);

        tabela.setRowSorter(sorter);
        busca.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                filtrar();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                filtrar();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                filtrar();
            }
        });

        add(new JScrollPane(tabela), BorderLayout.CENTER);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton cancelar = new JButton("Cancelar");
        cancelar.addActionListener(e -> dispose());
        JButton adicionar = new JButton("Adicionar ao carrinho");
        adicionar.setBackground(Ui.COR_PRINCIPAL);
        adicionar.setForeground(Color.WHITE);
        adicionar.addActionListener(e -> confirmar());
        botoes.add(cancelar);
        botoes.add(adicionar);
        add(botoes, BorderLayout.SOUTH);

        busca.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    confirmar();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    dispose();
                }
            }
        });

        tabela.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2 && tabela.getSelectedRow() >= 0) {
                    confirmar();
                }
            }
        });
    }

    private void filtrar() {
        String texto = busca.getText().trim();
        if (texto.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + texto));
        }
    }

    private void confirmar() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) {
            Ui.erro(this, "Selecione um produto na lista.");
            return;
        }
        String codigo = (String) tabela.getValueAt(linha, 0);
        Produto produto = produtos.buscarPorCodigoBarras(codigo);
        if (produto == null) {
            Ui.erro(this, "Produto não encontrado: " + codigo);
            return;
        }
        produtoSelecionado = produto;
        dispose();
    }

    public Optional<Produto> getResultado() {
        return Optional.ofNullable(produtoSelecionado);
    }

    public static Optional<Produto> pedir(Window proprietario, Aplicacao aplicacao) {
        DialogoBuscaProduto dialogo = new DialogoBuscaProduto(proprietario, aplicacao);
        dialogo.setVisible(true);
        return dialogo.getResultado();
    }
}
