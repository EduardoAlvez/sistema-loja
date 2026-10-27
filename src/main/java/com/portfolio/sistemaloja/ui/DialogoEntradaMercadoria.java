package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.model.EntradaItem;
import com.portfolio.sistemaloja.model.EntradaMercadoria;
import com.portfolio.sistemaloja.model.Fornecedor;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.service.ValidacaoException;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DialogoEntradaMercadoria extends JDialog {

    private static final long serialVersionUID = 1L;

    private final Aplicacao aplicacao;
    private boolean registrado;

    private final JComboBox<Fornecedor> fornecedor = new JComboBox<>();
    private final JTextField numeroNota = new JTextField(20);
    private final JTextField observacao = new JTextField(30);

    private final DefaultTableModel modeloItens = new DefaultTableModel(
            new Object[]{"ProdutoId", "Produto", "Quantidade", "Valor Unitário", "Subtotal"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tabelaItens = new JTable(modeloItens);
    private final JLabel total = new JLabel("Total: R$ 0,00");
    private final List<EntradaItem> itens = new ArrayList<>();

    public DialogoEntradaMercadoria(Window proprietario, Aplicacao aplicacao) {
        super(proprietario, "Lançar entrada de mercadoria", Dialog.ModalityType.APPLICATION_MODAL);
        this.aplicacao = aplicacao;
        montar();
        carregarFornecedores();
        pack();
        setLocationRelativeTo(proprietario);
        setMinimumSize(getPreferredSize());
    }

    private void montar() {
        setLayout(new BorderLayout(10, 10));
        JPanel cabecalho = new JPanel(new GridBagLayout());
        cabecalho.setBorder(Ui.bordaVazia(12, 12, 12, 12));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        adicionar(cabecalho, gbc, 0, "Fornecedor", fornecedor);
        adicionar(cabecalho, gbc, 1, "Nº da Nota", numeroNota);
        adicionar(cabecalho, gbc, 2, "Observação", observacao);

        add(cabecalho, BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(8, 8));
        centro.setBorder(Ui.bordaVazia(0, 12, 0, 12));
        JPanel botoesItens = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton adicionarItem = new JButton("Adicionar item");
        adicionarItem.addActionListener(e -> adicionarItem());
        JButton removerItem = new JButton("Remover item");
        removerItem.addActionListener(e -> removerItem());
        botoesItens.add(adicionarItem);
        botoesItens.add(removerItem);
        centro.add(botoesItens, BorderLayout.NORTH);
        tabelaItens.getColumnModel().getColumn(0).setMinWidth(0);
        tabelaItens.getColumnModel().getColumn(0).setMaxWidth(0);
        tabelaItens.getColumnModel().getColumn(0).setWidth(0);
        centro.add(new JScrollPane(tabelaItens), BorderLayout.CENTER);
        JPanel rodapeItens = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rodapeItens.add(total);
        centro.add(rodapeItens, BorderLayout.SOUTH);
        add(centro, BorderLayout.CENTER);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton cancelar = new JButton("Cancelar");
        cancelar.addActionListener(e -> dispose());
        JButton registrar = new JButton("Registrar entrada");
        registrar.setBackground(Ui.COR_PRINCIPAL);
        registrar.setForeground(java.awt.Color.WHITE);
        registrar.addActionListener(e -> registrar());
        botoes.add(cancelar);
        botoes.add(registrar);
        add(botoes, BorderLayout.SOUTH);
    }

    private void adicionar(JPanel painel, GridBagConstraints gbc, int linha, String rotulo, java.awt.Component campo) {
        gbc.gridx = 0;
        gbc.gridy = linha;
        painel.add(new JLabel(rotulo), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        painel.add(campo, gbc);
        gbc.weightx = 0;
    }

    private void carregarFornecedores() {
        fornecedor.removeAllItems();
        fornecedor.addItem(null);
        for (Fornecedor f : aplicacao.getFornecedores().listarAtivos()) {
            fornecedor.addItem(f);
        }
    }

    private void adicionarItem() {
        try {
            Produto produto = DialogoBuscaProduto.pedir(this, aplicacao).orElse(null);
            if (produto == null) {
                return;
            }
            String qtdStr = javax.swing.JOptionPane.showInputDialog(this, "Quantidade:", "Adicionar item", javax.swing.JOptionPane.QUESTION_MESSAGE);
            if (qtdStr == null) {
                return;
            }
            int quantidade = Integer.parseInt(qtdStr.trim());
            String valorStr = javax.swing.JOptionPane.showInputDialog(this, "Valor unitário (custo):", produto.getPrecoCusto());
            if (valorStr == null) {
                return;
            }
            BigDecimal valorUnitario = Ui.valorMonetario(valorStr);
            EntradaItem item = new EntradaItem();
            item.setProdutoId(produto.getId());
            item.setProdutoNome(produto.getNome());
            item.setQuantidade(quantidade);
            item.setValorUnitario(valorUnitario);
            item.recalcularSubtotal();
            itens.add(item);
            modeloItens.addRow(new Object[]{
                    produto.getId(),
                    produto.getNome(),
                    quantidade,
                    Ui.moeda(valorUnitario),
                    Ui.moeda(item.getSubtotal())
            });
            atualizarTotal();
        } catch (NumberFormatException ex) {
            Ui.erro(this, "Quantidade e valor unitário devem ser numéricos.");
        } catch (ValidacaoException ex) {
            Ui.erro(this, ex.getMessage());
        }
    }

    private void removerItem() {
        int linha = tabelaItens.getSelectedRow();
        if (linha < 0) {
            Ui.aviso(this, "Selecione um item para remover.");
            return;
        }
        itens.remove(linha);
        modeloItens.removeRow(linha);
        atualizarTotal();
    }

    private void atualizarTotal() {
        BigDecimal soma = BigDecimal.ZERO;
        for (EntradaItem item : itens) {
            if (item.getSubtotal() != null) {
                soma = soma.add(item.getSubtotal());
            }
        }
        total.setText("Total: " + Ui.moeda(soma));
    }

    private void registrar() {
        try {
            EntradaMercadoria entrada = new EntradaMercadoria();
            Fornecedor f = (Fornecedor) fornecedor.getSelectedItem();
            if (f != null) {
                entrada.setFornecedorId(f.getId());
                entrada.setFornecedorNome(f.getNome());
            }
            entrada.setNumeroNota(numeroNota.getText());
            entrada.setObservacao(observacao.getText());
            entrada.setDataEntrada(LocalDateTime.now());
            entrada.getItens().addAll(itens);
            entrada.recalcularTotal();
            aplicacao.getCompras().registrar(entrada, aplicacao.getSessao().getUsuario());
            registrado = true;
            Ui.informacao(this, "Entrada de mercadoria registrada com sucesso.");
            dispose();
        } catch (ValidacaoException ex) {
            Ui.erro(this, ex.getMessage());
        }
    }

    public boolean isRegistrado() {
        return registrado;
    }
}
