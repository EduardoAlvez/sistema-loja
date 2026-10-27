package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.model.EntradaMercadoria;
import com.portfolio.sistemaloja.model.EntradaItem;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PainelCompras extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Aplicacao aplicacao;
    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"ID", "Data", "Fornecedor", "Nº Nota", "Usuário", "Itens", "Total"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tabela = new JTable(modelo);

    public PainelCompras(Aplicacao aplicacao) {
        this.aplicacao = aplicacao;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        montarCabecalho();
        add(new JScrollPane(tabela), BorderLayout.CENTER);
        montarRodape();
        carregar(java.time.LocalDate.now(), java.time.LocalDate.now());
    }

    private void montarCabecalho() {
        JPanel cabecalho = new JPanel(new BorderLayout(10, 10));
        cabecalho.add(Ui.titulo("Entradas de Mercadoria"), BorderLayout.WEST);
        add(cabecalho, BorderLayout.NORTH);
    }

    private void montarRodape() {
        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton lancar = new JButton("Lançar entrada");
        lancar.setBackground(Ui.COR_PRINCIPAL);
        lancar.setForeground(Color.WHITE);
        lancar.addActionListener(e -> lancar());
        JButton visualizar = new JButton("Visualizar");
        visualizar.addActionListener(e -> visualizar());
        JButton fornecedores = new JButton("Fornecedores");
        fornecedores.addActionListener(e -> fornecedores());
        rodape.add(fornecedores);
        rodape.add(visualizar);
        rodape.add(lancar);
        add(rodape, BorderLayout.SOUTH);
    }

    private void carregar(java.time.LocalDate inicio, java.time.LocalDate fim) {
        modelo.setRowCount(0);
        try {
            List<EntradaMercadoria> entradas = aplicacao.getCompras().listar(inicio, fim);
            for (EntradaMercadoria entrada : entradas) {
                modelo.addRow(new Object[]{
                        entrada.getId(),
                        entrada.getDataEntrada() == null ? "" : entrada.getDataEntrada().format(DATA),
                        entrada.getFornecedorNome() == null ? "-" : entrada.getFornecedorNome(),
                        entrada.getNumeroNota() == null ? "" : entrada.getNumeroNota(),
                        entrada.getUsuarioNome() == null ? "" : entrada.getUsuarioNome(),
                        entrada.getItens() == null ? 0 : entrada.getItens().size(),
                        Ui.moeda(entrada.getValorTotal())
                });
            }
        } catch (Exception ex) {
            // ignora em ambiente inicial
        }
    }

    private void lancar() {
        DialogoEntradaMercadoria dialogo = new DialogoEntradaMercadoria(SwingUtilities.getWindowAncestor(this), aplicacao);
        dialogo.setVisible(true);
        if (dialogo.isRegistrado()) {
            carregar(java.time.LocalDate.now(), java.time.LocalDate.now());
        }
    }

    private void visualizar() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) {
            Ui.aviso(this, "Selecione uma entrada para visualizar.");
            return;
        }
        long id = (Long) modelo.getValueAt(linha, 0);
        try {
            EntradaMercadoria entrada = aplicacao.getCompras().buscarPorId(id);
            StringBuilder sb = new StringBuilder();
            sb.append("Fornecedor: ").append(entrada.getFornecedorNome() == null ? "-" : entrada.getFornecedorNome()).append('\n');
            sb.append("Nº Nota: ").append(entrada.getNumeroNota() == null ? "-" : entrada.getNumeroNota()).append('\n');
            sb.append("Data: ").append(entrada.getDataEntrada() == null ? "-" : entrada.getDataEntrada().format(DATA)).append('\n');
            sb.append("Usuário: ").append(entrada.getUsuarioNome() == null ? "-" : entrada.getUsuarioNome()).append('\n');
            sb.append("Observação: ").append(entrada.getObservacao() == null ? "-" : entrada.getObservacao()).append('\n');
            sb.append('\n');
            for (EntradaItem item : entrada.getItens()) {
                sb.append("- ").append(item.getProdutoNome()).append(" | Qtd: ").append(item.getQuantidade())
                        .append(" | Unit: ").append(Ui.moeda(item.getValorUnitario()))
                        .append(" | Subtotal: ").append(Ui.moeda(item.getSubtotal())).append('\n');
            }
            sb.append('\n').append("Total: ").append(Ui.moeda(entrada.getValorTotal()));
            JOptionPane.showMessageDialog(this, sb.toString(), "Detalhes da entrada", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            Ui.erro(this, ex.getMessage());
        }
    }

    private void fornecedores() {
        PainelFornecedores painel = new PainelFornecedores(aplicacao);
        javax.swing.JDialog dialogo = new javax.swing.JDialog(SwingUtilities.getWindowAncestor(this), "Fornecedores", Dialog.ModalityType.APPLICATION_MODAL);
        dialogo.add(painel, BorderLayout.CENTER);
        dialogo.setSize(700, 400);
        dialogo.setLocationRelativeTo(this);
        dialogo.setVisible(true);
    }
}
