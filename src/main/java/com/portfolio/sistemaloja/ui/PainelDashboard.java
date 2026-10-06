package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.repository.ProdutoEstoque;
import com.portfolio.sistemaloja.repository.ProdutoVendado;
import com.portfolio.sistemaloja.service.RelatorioService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;

public class PainelDashboard extends JPanel {

    private static final long serialVersionUID = 1L;

    private final transient Aplicacao aplicacao;
    private final JLabel cardVendas = cardValor();
    private final JLabel cardFaturamento = cardValor();
    private final JLabel cardTicket = cardValor();
    private final JLabel cardEstoque = cardValor();
    private final JTable tabelaTop = Ui.tabela(Ui.modeloTabela("Produto", "Qtd. vendida", "Total vendido"));
    private final JTable tabelaEstoque = Ui.tabela(Ui.modeloTabela("Produto", "Estoque", "Preço"));

    public PainelDashboard(Aplicacao aplicacao) {
        this.aplicacao = aplicacao;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setBackground(Ui.COR_FUNDO);
        add(montarTopo(), BorderLayout.NORTH);
        add(montarCentro(), BorderLayout.CENTER);
        atualizar();
    }

    private JPanel montarTopo() {
        JPanel topo = new JPanel(new BorderLayout());
        topo.setOpaque(false);

        JPanel titulo = new JPanel(new BorderLayout());
        titulo.setOpaque(false);
        titulo.add(Ui.titulo("Dashboard"), BorderLayout.WEST);
        JButton atualizar = new JButton("Atualizar");
        atualizar.addActionListener(e -> atualizar());
        titulo.add(atualizar, BorderLayout.EAST);
        topo.add(titulo, BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(1, 4, 12, 0));
        cards.setOpaque(false);
        cards.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        cards.add(card("Vendas hoje", cardVendas, Ui.COR_PRINCIPAL));
        cards.add(card("Faturamento hoje", cardFaturamento, Ui.COR_OK));
        cards.add(card("Ticket médio", cardTicket, new Color(130, 90, 160)));
        cards.add(card("Itens com estoque baixo", cardEstoque, Ui.COR_ALERTA));
        topo.add(cards, BorderLayout.CENTER);
        return topo;
    }

    private JPanel card(String titulo, JLabel valor, Color cor) {
        JPanel painel = new JPanel(new BorderLayout());
        painel.setBackground(Color.WHITE);
        painel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 230, 234)),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));

        JLabel rotulo = new JLabel(titulo);
        rotulo.setForeground(new Color(110, 118, 126));
        rotulo.setFont(rotulo.getFont().deriveFont(Font.PLAIN, 12f));

        valor.setForeground(cor);
        painel.add(rotulo, BorderLayout.NORTH);
        painel.add(valor, BorderLayout.CENTER);
        painel.setPreferredSize(new Dimension(painel.getPreferredSize().width, 96));
        return painel;
    }

    private JLabel cardValor() {
        JLabel valor = new JLabel("-");
        valor.setFont(valor.getFont().deriveFont(Font.BOLD, 24f));
        return valor;
    }

    private JSplitPane montarCentro() {
        JPanel esquerda = painelTabela("Mais vendidos (hoje)", new JScrollPane(tabelaTop));
        JPanel direita = painelTabela("Estoque baixo (≤ 5 un.)", new JScrollPane(tabelaEstoque));
        JSplitPane divisao = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, esquerda, direita);
        divisao.setResizeWeight(0.5);
        divisao.setDividerSize(8);
        divisao.setBorder(null);
        return divisao;
    }

    private JPanel painelTabela(String titulo, JScrollPane rolagem) {
        JPanel painel = new JPanel(new BorderLayout(0, 8));
        painel.setOpaque(false);
        JLabel rotulo = new JLabel(titulo);
        rotulo.setFont(rotulo.getFont().deriveFont(Font.BOLD, 14f));
        painel.add(rotulo, BorderLayout.NORTH);
        rolagem.setBorder(BorderFactory.createLineBorder(new Color(224, 228, 232)));
        painel.add(rolagem, BorderLayout.CENTER);
        return painel;
    }

    public final void atualizar() {
        RelatorioService.ResumoDashboard resumo = aplicacao.getRelatorios().resumoDoDia();
        cardVendas.setText(String.valueOf(resumo.vendas()));
        cardFaturamento.setText(Ui.moeda(resumo.faturamento()));
        cardTicket.setText(Ui.moeda(resumo.ticketMedio()));
        cardEstoque.setText(String.valueOf(resumo.itensEstoqueBaixo()));
        cardEstoque.setForeground(resumo.itensEstoqueBaixo() > 0 ? Ui.COR_ALERTA : Ui.COR_OK);

        List<ProdutoVendado> top = aplicacao.getRelatorios().maisVendidos(
                java.time.LocalDate.now(), java.time.LocalDate.now());
        preencher(tabelaTop, top.stream()
                .map(p -> new Object[]{p.nome(), p.quantidade(), Ui.moeda(p.totalVendido())})
                .toList());

        List<ProdutoEstoque> baixo = aplicacao.getRelatorios().estoqueBaixo();
        preencher(tabelaEstoque, baixo.stream()
                .map(p -> new Object[]{p.nome(), p.estoque(), Ui.moeda(p.precoVenda())})
                .toList());
    }

    private void preencher(JTable tabela, List<Object[]> linhas) {
        var modelo = (javax.swing.table.DefaultTableModel) tabela.getModel();
        modelo.setRowCount(0);
        linhas.forEach(modelo::addRow);
    }
}
