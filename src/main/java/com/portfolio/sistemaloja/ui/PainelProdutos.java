package com.portfolio.sistemaloja.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.model.Produto;

public class PainelProdutos extends JPanel {

    private static final long serialVersionUID = 1L;

    private final transient Aplicacao aplicacao;
    private final JTextField busca = new JTextField(24);
    private final javax.swing.table.DefaultTableModel modelo =
            Ui.modeloTabela("Código", "Produto", "Categoria", "Custo", "Venda", "Margem", "Estoque", "Situação");
    private final JTable tabela = Ui.tabela(modelo);
    private List<Produto> linhas = List.of();

    public PainelProdutos(Aplicacao aplicacao) {
        this.aplicacao = aplicacao;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setBackground(Ui.COR_FUNDO);
        add(montarTopo(), BorderLayout.NORTH);
        add(new JScrollPane(tabela), BorderLayout.CENTER);
        atualizar();
    }

    private JPanel montarTopo() {
        JPanel topo = new JPanel(new BorderLayout(10, 0));
        topo.setOpaque(false);

        JPanel titulo = new JPanel(new BorderLayout());
        titulo.setOpaque(false);
        titulo.add(Ui.titulo("Produtos"), BorderLayout.WEST);
        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acoes.setOpaque(false);

        JButton novo = new JButton("Novo");
        novo.setBackground(Ui.COR_PRINCIPAL);
        novo.setForeground(java.awt.Color.WHITE);
        novo.addActionListener(e -> editar(null));

        JButton editar = new JButton("Editar");
        editar.addActionListener(e -> editar(selecionado()));
        JButton excluir = new JButton("Excluir");
        excluir.setBackground(Ui.COR_ALERTA);
        excluir.setForeground(java.awt.Color.WHITE);
        excluir.addActionListener(e -> excluir());

        JButton atualizar = new JButton("Atualizar");
        atualizar.addActionListener(e -> atualizar());

        acoes.add(novo);
        acoes.add(editar);
        acoes.add(excluir);
        acoes.add(atualizar);
        titulo.add(acoes, BorderLayout.EAST);

        JPanel linhaBusca = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        linhaBusca.setOpaque(false);
        linhaBusca.add(Ui.rotulo("Buscar:"));
        linhaBusca.add(busca);
        busca.setToolTipText("Digite o nome ou código de barras e pressione Enter");
        busca.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    atualizar();
                }
            }
        });

        topo.add(titulo, BorderLayout.NORTH);
        topo.add(linhaBusca, BorderLayout.CENTER);
        return topo;
    }

    public final void atualizar() {
        linhas = aplicacao.getProdutos().listar(busca.getText());
        modelo.setRowCount(0);
        for (Produto produto : linhas) {
            modelo.addRow(new Object[]{
                    produto.getCodigoBarras(),
                    produto.getNome(),
                    produto.getCategoriaNome() == null ? "-" : produto.getCategoriaNome(),
                    Ui.moeda(produto.getPrecoCusto()),
                    Ui.moeda(produto.getPrecoVenda()),
                    Ui.moeda(produto.calcularLucro()),
                    produto.getEstoque(),
                    produto.isAtivo() ? "Ativo" : "Inativo"
            });
        }
    }

    private Produto selecionado() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) {
            Ui.info(this, "Selecione um produto na tabela.");
            return null;
        }
        return linhas.get(tabela.convertRowIndexToModel(linha));
    }

    private void editar(Produto produto) {
        Produto alvo = produto;
        if (alvo == null) {
            alvo = new Produto(null, "", "", null, BigDecimal.ZERO, BigDecimal.ZERO, 0, true);
        }
        DialogoProduto dialogo = new DialogoProduto(
                javax.swing.SwingUtilities.getWindowAncestor(this), aplicacao, alvo, this::atualizar);
        dialogo.setVisible(true);
    }

    private void excluir() {
        Produto produto = selecionado();
        if (produto == null) {
            return;
        }
        if (!Ui.confirma(this, "Excluir o produto \"" + produto.getNome() + "\"?")) {
            return;
        }
        try {
            aplicacao.getProdutos().remover(produto.getId());
            atualizar();
        } catch (RuntimeException e) {
            Ui.erro(this, e.getMessage(), e);
        }
    }
}
