package com.portfolio.sistemaloja.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import com.portfolio.sistemaloja.model.FormaPagamento;
import com.portfolio.sistemaloja.model.VendaPagamento;

public class PainelPagamentoDividido extends JPanel {

    private static final long serialVersionUID = 1L;

    private final transient BigDecimal total;
    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"Forma", "Valor"}, 0) {
        @Override
        public boolean isCellEditable(int linha, int coluna) {
            return true;
        }
    };
    private final JTable tabela = Ui.tabela(modelo);
    private final JLabel restante = new JLabel();

    public PainelPagamentoDividido(BigDecimal total, FormaPagamento sugerida) {
        this.total = total == null ? BigDecimal.ZERO : total;
        setLayout(new BorderLayout(8, 8));
        setBackground(Ui.COR_FUNDO);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel titulo = Ui.titulo("Pagamento dividido - total " + Ui.moeda(this.total));
        add(titulo, BorderLayout.NORTH);

        tabela.setRowHeight(24);
        tabela.getColumnModel().getColumn(0).setCellEditor(
                new DefaultCellEditor(new JComboBox<>(FormaPagamento.values())));
        add(new JScrollPane(tabela), BorderLayout.CENTER);

        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        acoes.setOpaque(false);
        JButton adicionar = new JButton("Adicionar parcela");
        adicionar.addActionListener(e -> adicionarParcela(sugerida, restante()));
        JButton remover = new JButton("Remover selecionada");
        remover.addActionListener(e -> removerSelecionada());
        acoes.add(adicionar);
        acoes.add(remover);

        restante.setFont(restante.getFont().deriveFont(Font.BOLD, 14f));
        acoes.add(restante);
        add(acoes, BorderLayout.SOUTH);

        modelo.addTableModelListener(e -> atualizarRestante());
        atualizarRestante();
    }

    public final void adicionarParcela(FormaPagamento forma, BigDecimal valor) {
        modelo.addRow(new Object[]{forma == null ? FormaPagamento.DINHEIRO : forma, Ui.moeda(valor)});
    }

    private void removerSelecionada() {
        int linha = tabela.getSelectedRow();
        if (linha >= 0) {
            modelo.removeRow(linha);
        }
    }

    public List<VendaPagamento> parcelas() {
        List<VendaPagamento> parcelas = new ArrayList<>();
        for (int linha = 0; linha < modelo.getRowCount(); linha++) {
            FormaPagamento forma = (FormaPagamento) modelo.getValueAt(linha, 0);
            parcelas.add(new VendaPagamento(forma, valorDaLinha(linha)));
        }
        return parcelas;
    }

    public BigDecimal somaParcelas() {
        return parcelas().stream()
                .map(VendaPagamento::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal restante() {
        return total.subtract(somaParcelas());
    }

    public boolean completo() {
        if (parcelas().isEmpty()) {
            return false;
        }
        return parcelas().stream().allMatch(parcela -> parcela.getValor().compareTo(BigDecimal.ZERO) > 0)
                && restante().compareTo(BigDecimal.ZERO) <= 0;
    }

    private BigDecimal valorDaLinha(int linha) {
        return Ui.parseMoeda(String.valueOf(modelo.getValueAt(linha, 1)));
    }

    private void atualizarRestante() {
        BigDecimal falta = restante();
        restante.setForeground(falta.compareTo(BigDecimal.ZERO) > 0 ? Ui.COR_ALERTA : Ui.COR_OK);
        restante.setText(falta.compareTo(BigDecimal.ZERO) >= 0
                ? "Falta: " + Ui.moeda(falta)
                : "Troco: " + Ui.moeda(falta.negate()));
    }
}
