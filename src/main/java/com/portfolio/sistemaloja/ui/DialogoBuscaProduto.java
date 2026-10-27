package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.service.ProdutoService;
import com.portfolio.sistemaloja.service.ValidacaoException;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

public class DialogoBuscaProduto extends JDialog {

    private static final long serialVersionUID = 1L;

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
    private final JLabel status = new JLabel(" ");
    private Produto produtoSelecionado = null;

    public DialogoBuscaProduto(Window proprietario, Aplicacao aplicacao) {
        super(proprietario, "Buscar produto", Dialog.ModalityType.APPLICATION_MODAL);
        this.produtos = aplicacao.getProdutos();
        montar();
        carregar();
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

        JPanel rodape = new JPanel(new BorderLayout(8, 0));
        rodape.setOpaque(false);
        status.setForeground(Ui.COR_ESCURA);
        rodape.add(status, BorderLayout.WEST);
        rodape.add(botoes, BorderLayout.EAST);
        add(rodape, BorderLayout.SOUTH);

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

    private void carregar() {
        modelo.setRowCount(0);
        for (Produto produto : produtos.listarAtivos()) {
            modelo.addRow(new Object[]{
                    produto.getCodigoBarras(),
                    produto.getNome(),
                    produto.getEstoque(),
                    Ui.moeda(produtos.precoVigente(produto, LocalDateTime.now()))
            });
        }
        selecionarPrimeiraLinhaVisivel();
        atualizarStatus("");
    }

    private void filtrar() {
        String termo = busca.getText().strip().toLowerCase(Locale.ROOT);
        if (termo.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
                @Override
                public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entrada) {
                    for (int coluna = 0; coluna < entrada.getValueCount(); coluna++) {
                        String valor = entrada.getStringValue(coluna);
                        if (valor != null && valor.toLowerCase(Locale.ROOT).contains(termo)) {
                            return true;
                        }
                    }
                    return false;
                }
            });
        }
        selecionarPrimeiraLinhaVisivel();
        atualizarStatus(termo);
    }

    private void atualizarStatus(String termo) {
        int quantidade = tabela.getRowCount();
        if (termo.isEmpty()) {
            status.setText(quantidade + " produto(s) ativo(s)");
            return;
        }
        if (quantidade == 0) {
            status.setText("Nenhum produto encontrado para \"" + termo + "\"");
            return;
        }
        status.setText(quantidade + " produto(s) encontrado(s)");
    }

    private void selecionarPrimeiraLinhaVisivel() {
        tabela.clearSelection();
        if (tabela.getRowCount() == 0) {
            return;
        }
        tabela.setRowSelectionInterval(0, 0);
    }

    private void confirmar() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) {
            Ui.erro(this, "Selecione um produto na lista.");
            return;
        }
        String codigo = String.valueOf(tabela.getValueAt(linha, 0));
        try {
            produtoSelecionado = produtos.buscarPorCodigoBarras(codigo);
        } catch (ValidacaoException e) {
            Ui.erro(this, e.getMessage());
            return;
        }
        dispose();
    }

    int totalLinhas() {
        return tabela.getRowCount();
    }

    int totalProdutosCarregados() {
        return modelo.getRowCount();
    }

    Object valor(int linha, int coluna) {
        return tabela.getValueAt(linha, coluna);
    }

    void informarBusca(String texto) {
        busca.setText(texto);
    }

    int linhaSelecionada() {
        return tabela.getSelectedRow();
    }

    String textoStatus() {
        return status.getText();
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
