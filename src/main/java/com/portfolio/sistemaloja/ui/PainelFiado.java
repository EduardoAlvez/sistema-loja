package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.model.Fiado;
import com.portfolio.sistemaloja.model.RecebimentoFiado;
import com.portfolio.sistemaloja.service.FiadoService;
import com.portfolio.sistemaloja.service.ValidacaoException;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Optional;

public class PainelFiado extends JPanel {

    private static final long serialVersionUID = 1L;

    private final transient Aplicacao aplicacao;
    private final FiadoService fiados;
    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"Fiado ID", "Cliente ID", "Venda ID", "Valor Total", "Pago", "Devedor", "Juros", "Vencimento", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tabela = new JTable(modelo);
    private final JTextField busca = new JTextField(15);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(modelo);

    public PainelFiado(Aplicacao aplicacao) {
        this.aplicacao = aplicacao;
        this.fiados = aplicacao.getFiados();
        setLayout(new BorderLayout(10, 10));
        setOpaque(false);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        topo.setOpaque(false);
        topo.add(Ui.titulo("Fiado / Recebíveis"));
        topo.add(Ui.rotulo("Buscar cliente:"));
        topo.add(busca);
        JButton atualizar = new JButton("Atualizar");
        atualizar.addActionListener(e -> carregar());
        topo.add(atualizar);
        JButton receber = new JButton("Receber Selecionado");
        receber.setBackground(Ui.COR_PRINCIPAL);
        receber.setForeground(Color.WHITE);
        receber.addActionListener(e -> receberSelecionado());
        topo.add(receber);
        add(topo, BorderLayout.NORTH);

        tabela.setRowSorter(sorter);
        // Fiado ID mora so no modelo (coluna 0): a visao mantem as colunas de sempre.
        tabela.removeColumn(tabela.getColumnModel().getColumn(0));
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
        carregar();
    }

    private void filtrar() {
        String texto = busca.getText().trim();
        if (texto.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + texto, 1));
        }
    }

    private void carregar() {
        fiados.marcarVencidos();
        modelo.setRowCount(0);
        List<Fiado> fiadosLista = fiados.listarPendentes();
        for (Fiado f : fiadosLista) {
            modelo.addRow(new Object[]{
                    f.id(), f.clienteId(), f.vendaId(),
                    Ui.moeda(f.valorTotal()), Ui.moeda(f.valorPago()),
                    Ui.moeda(f.valorDevedor()), Ui.moeda(f.jurosAplicados()),
                    f.dataVencimento(), f.status()
            });
        }
    }

    private void receberSelecionado() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) {
            Ui.info(this, "Selecione um fiado na tabela.");
            return;
        }
        Fiado fiado = fiadoDaLinha(linha);
        if (fiado == null) {
            Ui.erro(this, "Fiado não encontrado.");
            return;
        }
        Optional<RecebimentoFiado> resultado = DialogoRecebimentoFiado.pedir(
                SwingUtilities.getWindowAncestor(this), aplicacao, fiado);
        if (resultado.isPresent()) {
            carregar();
            Ui.info(this, "Recebimento de " + Ui.moeda(resultado.get().valor()) + " registrado.");
        }
    }

    /** Linha resolvida pelo id proprio do fiado (divida #45): sem re-lookup por cliente+venda. */
    Fiado fiadoDaLinha(int linhaView) {
        if (linhaView < 0 || linhaView >= tabela.getRowCount()) {
            return null;
        }
        Object id = modelo.getValueAt(sorter.convertRowIndexToModel(linhaView), 0);
        if (!(id instanceof Number numero)) {
            return null;
        }
        return fiados.buscarPorId(numero.longValue()).orElse(null);
    }
}
