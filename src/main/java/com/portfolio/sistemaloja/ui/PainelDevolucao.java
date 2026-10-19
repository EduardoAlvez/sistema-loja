package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.model.DevolucaoItem;
import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.model.VendaItem;
import com.portfolio.sistemaloja.service.ValidacaoException;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableModel;

public class PainelDevolucao extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int COLUNA_DEVOLVER = 4;

    private final transient List<VendaItem> itens;
    private final transient int[] maximos;
    private final transient ToIntFunction<Long> jaDevolvido;
    private final DefaultTableModel modelo;
    private final JCheckBox credito;
    private final JLabel total = new JLabel();

    public PainelDevolucao(Venda venda, ToIntFunction<Long> jaDevolvido) {
        this.itens = new ArrayList<>(venda.getItens());
        this.maximos = new int[itens.size()];
        this.jaDevolvido = jaDevolvido;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        modelo = new DefaultTableModel(
                new Object[]{"Produto", "Vendido", "Devolvido", "Disponível", "Devolver", "Unitário"}, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int linha, int coluna) {
                return coluna == COLUNA_DEVOLVER;
            }
        };
        for (int linha = 0; linha < itens.size(); linha++) {
            VendaItem item = itens.get(linha);
            int devolvido = item.getId() == null ? 0 : jaDevolvido.applyAsInt(item.getId());
            maximos[linha] = Math.max(item.getQuantidade() - devolvido, 0);
            modelo.addRow(new Object[] {
                    item.getProdutoNome(), item.getQuantidade(), devolvido, maximos[linha], 0,
                    Ui.moeda(item.getPrecoUnitario())
            });
        }

        modelo.addTableModelListener(e -> {
            if (e.getType() == javax.swing.event.TableModelEvent.UPDATE
                    && e.getColumn() == COLUNA_DEVOLVER) {
                normalizar(e.getFirstRow());
            }
        });

        JScrollPane rolagem = new JScrollPane(Ui.tabela(modelo));
        rolagem.setPreferredSize(new Dimension(720, 260));
        add(rolagem, BorderLayout.CENTER);

        credito = new JCheckBox("Creditar o valor devolvido no cliente");
        credito.setOpaque(false);
        credito.setEnabled(venda.getClienteId() != null);
        if (venda.getClienteId() == null) {
            credito.setToolTipText("Venda sem cliente identificado: não é possível gerar crédito.");
        }

        total.setFont(total.getFont().deriveFont(Font.BOLD, 16f));
        atualizarTotal();

        JPanel rodape = new JPanel(new BorderLayout());
        rodape.setOpaque(false);
        rodape.add(credito, BorderLayout.WEST);
        rodape.add(total, BorderLayout.EAST);
        add(rodape, BorderLayout.SOUTH);
    }

    public int getQuantidadeLinhas() {
        return itens.size();
    }

    public int quantidadeMaxima(int linha) {
        return maximos[linha];
    }

    public int quantidade(int linha) {
        return (int) modelo.getValueAt(linha, COLUNA_DEVOLVER);
    }

    public void definirQuantidade(int linha, int quantidade) {
        modelo.setValueAt(quantidade, linha, COLUNA_DEVOLVER);
    }

    public void definirCreditoNoCliente(boolean valor) {
        credito.setSelected(valor);
    }

    public boolean creditoNoCliente() {
        return credito.isSelected();
    }

    public boolean creditoDisponivel() {
        return credito.isEnabled();
    }

    public BigDecimal total() {
        BigDecimal soma = BigDecimal.ZERO;
        for (int linha = 0; linha < itens.size(); linha++) {
            soma = soma.add(itens.get(linha).getPrecoUnitario()
                    .multiply(BigDecimal.valueOf(quantidade(linha))));
        }
        return soma;
    }

    public List<DevolucaoItem> itensParaDevolucao() {
        List<DevolucaoItem> selecionados = new ArrayList<>();
        for (int linha = 0; linha < itens.size(); linha++) {
            int quantidade = quantidade(linha);
            if (quantidade <= 0) {
                continue;
            }
            VendaItem item = itens.get(linha);
            selecionados.add(new DevolucaoItem(item.getId(), item.getProdutoId(), item.getProdutoNome(),
                    quantidade, item.getPrecoUnitario()));
        }
        if (selecionados.isEmpty()) {
            throw new ValidacaoException("Informe a quantidade a devolver em pelo menos um item.");
        }
        return selecionados;
    }

    private void normalizar(int linha) {
        Object valor = modelo.getValueAt(linha, COLUNA_DEVOLVER);
        int quantidade;
        try {
            quantidade = valor == null || String.valueOf(valor).isBlank()
                    ? 0 : Integer.parseInt(String.valueOf(valor).strip());
        } catch (NumberFormatException e) {
            quantidade = 0;
        }
        if (quantidade < 0) {
            quantidade = 0;
        }
        if (quantidade > maximos[linha]) {
            quantidade = maximos[linha];
        }
        if (quantidade != (int) modelo.getValueAt(linha, COLUNA_DEVOLVER)) {
            modelo.setValueAt(quantidade, linha, COLUNA_DEVOLVER);
        }
        atualizarTotal();
    }

    private void atualizarTotal() {
        total.setText("Devolução: " + Ui.moeda(total()));
    }
}
