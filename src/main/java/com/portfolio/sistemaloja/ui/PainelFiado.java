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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Optional;

public class PainelFiado extends JPanel {

    private static final long serialVersionUID = 1L;

    private final transient Aplicacao aplicacao;
    private final FiadoService fiados;
    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"Cliente ID", "Venda ID", "Valor Total", "Pago", "Devedor", "Juros", "Vencimento", "Status"}, 0) {
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
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + texto, 0));
        }
    }

    private void carregar() {
        modelo.setRowCount(0);
        List<Fiado> fiadosLista = fiados.listarAbertos();
        for (Fiado f : fiadosLista) {
            boolean atrasado = f.dataVencimento().isBefore(LocalDate.now()) && !f.isQuitado();
            String statusExibicao = atrasado ? "INADIMPLENTE" : f.status();
            modelo.addRow(new Object[]{
                    f.clienteId(), f.vendaId(),
                    Ui.moeda(f.valorTotal()), Ui.moeda(f.valorPago()),
                    Ui.moeda(f.valorDevedor()), Ui.moeda(f.jurosAplicados()),
                    f.dataVencimento(), statusExibicao
            });
        }
    }

    private void receberSelecionado() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) {
            Ui.info(this, "Selecione um fiado na tabela.");
            return;
        }
        long clienteId = (Long) tabela.getValueAt(linha, 0);
        long vendaId = (Long) tabela.getValueAt(linha, 1);
        Fiado fiado = fiados.listarPorCliente(clienteId).stream()
                .filter(f -> f.vendaId().equals(vendaId))
                .findFirst()
                .orElse(null);
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
}
