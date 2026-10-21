package com.portfolio.sistemaloja.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.model.Cliente;
import com.portfolio.sistemaloja.model.ExtratoFidelidade;
import com.portfolio.sistemaloja.model.TipoMovimentoFidelidade;

public class DialogoExtratoFidelidade extends JDialog {

    private static final long serialVersionUID = 1L;

    private final transient Aplicacao aplicacao;
    private final transient Cliente cliente;
    private final javax.swing.table.DefaultTableModel modelo =
            Ui.modeloTabela("Data", "Movimento", "Pontos", "Descrição");
    private final JTable tabela = Ui.tabela(modelo);
    private final JLabel resumo = new JLabel(" ");

    public DialogoExtratoFidelidade(java.awt.Window pai, Aplicacao aplicacao, Cliente cliente) {
        super(pai, "Extrato de pontos - " + cliente.getNome(), java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        this.aplicacao = aplicacao;
        this.cliente = cliente;
        montar();
        carregar();
        pack();
        setLocationRelativeTo(pai);
        setMinimumSize(new Dimension(560, 320));
    }

    private void montar() {
        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        topo.setOpaque(false);
        resumo.setFont(resumo.getFont().deriveFont(java.awt.Font.BOLD, 14f));
        resumo.setForeground(Ui.COR_ESCURA);
        topo.add(resumo);

        JButton fechar = new JButton("Fechar");
        fechar.addActionListener(e -> dispose());
        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        rodape.add(fechar);

        JPanel conteudo = new JPanel(new BorderLayout(10, 10));
        conteudo.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        conteudo.add(topo, BorderLayout.NORTH);
        conteudo.add(new JScrollPane(tabela), BorderLayout.CENTER);
        conteudo.add(rodape, BorderLayout.SOUTH);
        setContentPane(conteudo);
    }

    private void carregar() {
        int saldo = aplicacao.getFidelidade().saldo(cliente.getId());
        List<ExtratoFidelidade> movimentos = aplicacao.getFidelidade().extrato(cliente.getId());
        resumo.setText("Saldo atual: " + saldo + " ponto(s)");
        modelo.setRowCount(0);
        for (ExtratoFidelidade movimento : movimentos) {
            modelo.addRow(new Object[]{
                    movimento.dataHora() == null ? "" : Ui.dataHora(movimento.dataHora().toString()),
                    descricao(movimento.tipo()),
                    (movimento.tipo() == TipoMovimentoFidelidade.CREDITO
                            || movimento.tipo() == TipoMovimentoFidelidade.ESTORNO_DEBITO ? "+" : "-")
                            + movimento.pontos(),
                    movimento.descricao()
            });
        }
    }

    private String descricao(TipoMovimentoFidelidade tipo) {
        switch (tipo) {
            case CREDITO:
                return "Crédito";
            case DEBITO:
                return "Resgate";
            case ESTORNO_CREDITO:
                return "Estorno de crédito";
            case ESTORNO_DEBITO:
                return "Devolução de resgate";
            default:
                return tipo.name();
        }
    }
}
