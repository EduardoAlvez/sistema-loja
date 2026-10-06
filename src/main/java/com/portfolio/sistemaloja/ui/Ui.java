package com.portfolio.sistemaloja.ui;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class Ui {

    public static final Color COR_PRINCIPAL = new Color(33, 97, 140);
    public static final Color COR_ESCURA = new Color(23, 66, 96);
    public static final Color COR_FUNDO = new Color(245, 247, 250);
    public static final Color COR_ALERTA = new Color(192, 80, 48);
    public static final Color COR_OK = new Color(63, 125, 74);

    private static final NumberFormat MOEDA = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Ui() {
    }

    public static String moeda(BigDecimal valor) {
        return MOEDA.format(valor == null ? BigDecimal.ZERO : valor);
    }

    public static String data(LocalDate data) {
        return data == null ? "-" : DATA.format(data);
    }

    public static String dataHora(String dataHoraBanco) {
        if (dataHoraBanco == null) {
            return "-";
        }
        try {
            return DATA_HORA.format(java.time.LocalDateTime.parse(
                    dataHoraBanco.replace(' ', 'T')).withNano(0));
        } catch (java.time.format.DateTimeParseException e) {
            return dataHoraBanco;
        }
    }

    public static void erro(Component pai, String mensagem) {
        JOptionPane.showMessageDialog(pai, mensagem, "Erro", JOptionPane.ERROR_MESSAGE);
    }

    public static void info(Component pai, String mensagem) {
        JOptionPane.showMessageDialog(pai, mensagem, "Aviso", JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirma(Component pai, String mensagem) {
        return JOptionPane.showConfirmDialog(pai, mensagem, "Confirmar",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    public static DefaultTableModel modeloTabela(String... colunas) {
        return new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int linha, int coluna) {
                return false;
            }
        };
    }

    public static JTable tabela(DefaultTableModel modelo) {
        JTable tabela = new JTable(modelo);
        tabela.setRowHeight(26);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.setFillsViewportHeight(true);
        tabela.getTableHeader().setReorderingAllowed(false);
        tabela.getTableHeader().setFont(tabela.getTableHeader().getFont().deriveFont(Font.BOLD));
        tabela.setRowSorter(new TableRowSorter<>(modelo));
        tabela.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object valor, boolean selecionado,
                                                           boolean foco, int linha, int coluna) {
                Component c = super.getTableCellRendererComponent(t, valor, selecionado, foco, linha, coluna);
                if (!selecionado) {
                    c.setBackground(linha % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                }
                return c;
            }
        });
        return tabela;
    }

    public static JScrollPane rolagem(JComponent componente) {
        JScrollPane painel = new JScrollPane(componente);
        painel.setBorder(BorderFactory.createLineBorder(new Color(224, 228, 232)));
        painel.setPreferredSize(new Dimension(painel.getPreferredSize().width, painel.getPreferredSize().height));
        return painel;
    }

    public static JLabel rotulo(String texto) {
        JLabel rotulo = new JLabel(texto);
        rotulo.setFont(rotulo.getFont().deriveFont(Font.PLAIN, 13f));
        return rotulo;
    }

    public static JLabel titulo(String texto) {
        JLabel rotulo = new JLabel(texto);
        rotulo.setFont(rotulo.getFont().deriveFont(Font.BOLD, 20f));
        rotulo.setForeground(COR_ESCURA);
        rotulo.setBorder(BorderFactory.createEmptyBorder(4, 0, 12, 0));
        return rotulo;
    }
}
