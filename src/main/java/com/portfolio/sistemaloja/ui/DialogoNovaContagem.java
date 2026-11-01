package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.model.Produto;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Define o alcance da contagem: todos os produtos ativos ou um lote escolhido.
 *
 * <p>A contagem de um lote é a forma cíclica de trabalho: conta-se parte do estoque por
 * vez, abrindo e fechando uma contagem a cada ciclo.
 */
public class DialogoNovaContagem extends JDialog {

    private static final long serialVersionUID = 1L;

    private final List<Produto> produtos;
    private final JRadioButton todos = new JRadioButton("Todos os produtos ativos", true);
    private final JRadioButton lote = new JRadioButton("Contar um lote de produtos");
    private final JTextField filtro = new JTextField(22);
    private final DefaultListModel<Produto> modelo = new DefaultListModel<>();
    private final JList<Produto> lista = new JList<>(modelo);
    private final JLabel contador = new JLabel(" ");
    private List<Long> resultado;

    public DialogoNovaContagem(Window proprietario, List<Produto> produtos) {
        super(proprietario, "Nova contagem de inventário", Dialog.ModalityType.APPLICATION_MODAL);
        this.produtos = produtos;
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

        ButtonGroup grupo = new ButtonGroup();
        grupo.add(todos);
        grupo.add(lote);

        gbc.gridx = 0;
        gbc.gridy = 0;
        painel.add(Ui.rotulo("Alcance da contagem:"), gbc);
        gbc.gridx = 1;
        painel.add(todos, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        painel.add(Ui.rotulo(""), gbc);
        gbc.gridx = 1;
        painel.add(lote, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        painel.add(Ui.rotulo("Filtrar produtos:", filtro), gbc);
        gbc.gridx = 1;
        painel.add(filtro, gbc);

        add(painel, BorderLayout.NORTH);

        lista.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        lista.setVisibleRowCount(12);
        lista.setCellRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> listaJList,
                                                                     Object valor, int indice,
                                                                     boolean selecionado, boolean foco) {
                super.getListCellRendererComponent(listaJList, valor, indice, selecionado, foco);
                if (valor instanceof Produto produto) {
                    setText(produto.getCodigoBarras() + " - " + produto.getNome()
                            + " (estoque: " + produto.getEstoque() + ")");
                }
                return this;
            }
        });
        lista.getAccessibleContext().setAccessibleName("Produtos para contar");
        JScrollPane rolagem = new JScrollPane(lista);
        add(rolagem, BorderLayout.CENTER);

        contador.setFont(contador.getFont().deriveFont(java.awt.Font.PLAIN, 13f));

        JPanel rodape = new JPanel(new BorderLayout(8, 8));
        rodape.add(contador, BorderLayout.WEST);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton cancelar = new JButton("Cancelar");
        cancelar.addActionListener(e -> dispose());
        JButton iniciar = new JButton("Iniciar contagem");
        iniciar.setBackground(Ui.COR_PRINCIPAL);
        iniciar.setForeground(Color.WHITE);
        iniciar.addActionListener(e -> iniciar());
        botoes.add(cancelar);
        botoes.add(iniciar);
        rodape.add(botoes, BorderLayout.EAST);
        add(rodape, BorderLayout.SOUTH);

        todos.addActionListener(e -> atualizarEstado());
        lote.addActionListener(e -> atualizarEstado());
        lista.addListSelectionListener(e -> atualizarContador());
        filtro.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                aplicarFiltro();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                aplicarFiltro();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                aplicarFiltro();
            }
        });

        aplicarFiltro();
        atualizarEstado();
    }

    private void aplicarFiltro() {
        String termo = filtro.getText() == null ? "" : filtro.getText().trim().toLowerCase();
        modelo.clear();
        for (Produto produto : produtos) {
            if (termo.isEmpty()
                    || produto.getNome().toLowerCase().contains(termo)
                    || produto.getCodigoBarras().toLowerCase().contains(termo)) {
                modelo.addElement(produto);
            }
        }
        atualizarContador();
    }

    private void atualizarEstado() {
        lista.setEnabled(lote.isSelected());
        filtro.setEnabled(lote.isSelected());
        atualizarContador();
    }

    private void atualizarContador() {
        if (todos.isSelected()) {
            contador.setText(produtos.size() + " produto(s) ativo(s) entrarão na contagem.");
            return;
        }
        int selecionados = lista.getSelectedValuesList().size();
        contador.setText(selecionados + " produto(s) selecionado(s) de " + modelo.getSize() + " na lista.");
    }

    private void iniciar() {
        List<Long> ids = new ArrayList<>();
        if (todos.isSelected()) {
            for (Produto produto : produtos) {
                ids.add(produto.getId());
            }
        } else {
            for (Produto produto : lista.getSelectedValuesList()) {
                ids.add(produto.getId());
            }
            if (ids.isEmpty()) {
                Ui.erro(this, "Selecione ao menos um produto para contar.");
                return;
            }
        }
        resultado = ids;
        dispose();
    }

    public Optional<List<Long>> getResultado() {
        return Optional.ofNullable(resultado);
    }

    public static Optional<List<Long>> pedir(Window proprietario, List<Produto> produtos) {
        DialogoNovaContagem dialogo = new DialogoNovaContagem(proprietario, produtos);
        dialogo.setVisible(true);
        return dialogo.getResultado();
    }
}
