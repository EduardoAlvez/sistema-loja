package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.model.ContagemInventario;
import com.portfolio.sistemaloja.model.ContagemItem;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.service.ValidacaoException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Painel da contagem cíclica de inventário.
 *
 * <p>A contagem segue três passos: abrir, informar item a item e fechar. O estoque só é
 * alterado no fechamento, em uma transação única auditada.
 */
public class PainelContagem extends JPanel {

    private static final long serialVersionUID = 1L;

    private final Aplicacao aplicacao;
    private final JLabel situacao = new JLabel(" ");
    private final JLabel progresso = new JLabel(" ");
    private final JTextField observacao = new JTextField(28);
    private final JButton informar = new JButton("Informar contagem");
    private final JButton fechar = new JButton("Fechar e ajustar");
    private final DefaultTableModel modeloItens = Ui.modeloTabela(
            "Produto", "Estoque atual", "Quantidade contada", "Diferença", "Situação");
    private final JTable tabelaItens = Ui.tabela(modeloItens);
    private final DefaultTableModel modeloHistorico = Ui.modeloTabela(
            "Contagem", "Iniciada em", "Fechada em", "Responsável", "Observação");
    private final JTable tabelaHistorico = Ui.tabela(modeloHistorico);
    private ContagemInventario contagemAberta;
    private List<ContagemItem> itens = new ArrayList<>();

    public PainelContagem(Aplicacao aplicacao) {
        this.aplicacao = aplicacao;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        add(montarCabecalho(), BorderLayout.NORTH);
        add(montarTabelas(), BorderLayout.CENTER);
        add(progresso, BorderLayout.SOUTH);
        carregar();
    }

    private JPanel montarCabecalho() {
        JPanel painel = new JPanel(new BorderLayout(10, 10));
        painel.add(Ui.titulo("Inventário - Contagem cíclica"), BorderLayout.NORTH);

        situacao.setFont(situacao.getFont().deriveFont(java.awt.Font.BOLD, 14f));
        situacao.setForeground(Ui.COR_ESCURA);
        JPanel faixa = new JPanel(new BorderLayout());
        faixa.add(situacao, BorderLayout.CENTER);
        painel.add(faixa, BorderLayout.CENTER);

        JPanel acoes = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        JButton iniciar = new JButton("Iniciar contagem");
        iniciar.setBackground(Ui.COR_PRINCIPAL);
        iniciar.setForeground(Color.WHITE);
        iniciar.addActionListener(e -> iniciarContagem());
        gbc.gridx = 0;
        gbc.gridy = 0;
        acoes.add(iniciar, gbc);

        gbc.gridx = 1;
        acoes.add(informar, gbc);
        informar.addActionListener(e -> informarContagem());

        fechar.setBackground(Ui.COR_ALERTA);
        fechar.setForeground(Color.WHITE);
        fechar.addActionListener(e -> fecharContagem());
        gbc.gridx = 2;
        acoes.add(fechar, gbc);

        gbc.gridx = 3;
        gbc.insets = new Insets(4, 16, 4, 4);
        acoes.add(Ui.rotulo("Observação do fechamento:", observacao), gbc);
        gbc.gridx = 4;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        acoes.add(observacao, gbc);

        painel.add(acoes, BorderLayout.SOUTH);
        return painel;
    }

    private JSplitPane montarTabelas() {
        JPanel painelItens = new JPanel(new BorderLayout(4, 4));
        painelItens.add(Ui.rotulo("Itens da contagem em aberto"), BorderLayout.NORTH);
        painelItens.add(new JScrollPane(tabelaItens), BorderLayout.CENTER);

        JPanel painelHistorico = new JPanel(new BorderLayout(4, 4));
        painelHistorico.add(Ui.rotulo("Contagens anteriores"), BorderLayout.NORTH);
        painelHistorico.add(new JScrollPane(tabelaHistorico), BorderLayout.CENTER);

        JSplitPane divisor = new JSplitPane(JSplitPane.VERTICAL_SPLIT, painelItens, painelHistorico);
        divisor.setResizeWeight(0.72);
        divisor.setBorder(BorderFactory.createEmptyBorder());
        return divisor;
    }

    private void iniciarContagem() {
        List<Produto> ativos;
        try {
            ativos = aplicacao.getProdutos().listarAtivos();
        } catch (RuntimeException ex) {
            Ui.erro(this, "Falha ao carregar os produtos.", ex);
            return;
        }
        if (ativos.isEmpty()) {
            Ui.aviso(this, "Não há produtos ativos para contar.");
            return;
        }
        Optional<List<Long>> ids = DialogoNovaContagem.pedir(
                javax.swing.SwingUtilities.getWindowAncestor(this), ativos);
        if (ids.isEmpty()) {
            return;
        }
        try {
            long contagemId = aplicacao.getInventario().abrirContagem(ids.get());
            observacao.setText("");
            carregar();
            Ui.informacao(this, "Contagem iniciada com " + ids.get().size() + " item(ns).");
        } catch (ValidacaoException ex) {
            Ui.erro(this, ex.getMessage());
        } catch (RuntimeException ex) {
            Ui.erro(this, "Falha ao iniciar a contagem.", ex);
        }
    }

    private void informarContagem() {
        ContagemItem item = itemSelecionado();
        if (item == null) {
            Ui.aviso(this, "Selecione um item da contagem para informar a quantidade.");
            return;
        }
        int estoqueAtual = estoqueAtualDe(item);
        Optional<Integer> quantidade = DialogoContagemItem.pedir(
                javax.swing.SwingUtilities.getWindowAncestor(this), item, estoqueAtual);
        if (quantidade.isEmpty()) {
            return;
        }
        try {
            aplicacao.getInventario().informarContagem(contagemAberta.getId(), item.getProdutoId(),
                    quantidade.get());
            carregar();
        } catch (ValidacaoException ex) {
            Ui.erro(this, ex.getMessage());
        } catch (RuntimeException ex) {
            Ui.erro(this, "Falha ao informar a contagem.", ex);
        }
    }

    private void fecharContagem() {
        if (contagemAberta == null) {
            Ui.aviso(this, "Não há contagem em aberto para fechar.");
            return;
        }
        int contados = (int) itens.stream().filter(ContagemItem::foiContado).count();
        if (contados == 0) {
            Ui.aviso(this, "Informe ao menos uma contagem antes de fechar.");
            return;
        }
        if (!Ui.confirma(this, "Fechar a contagem " + contagemAberta.getNumero() + " e ajustar o estoque de "
                + contados + " item(ns) contados?")) {
            return;
        }
        try {
            ContagemInventario fechada = aplicacao.getInventario().fecharContagem(contagemAberta.getId(),
                    observacao.getText());
            observacao.setText("");
            carregar();
            Ui.informacao(this, "Contagem " + fechada.getNumero() + " fechada e estoque ajustado.");
        } catch (ValidacaoException ex) {
            Ui.erro(this, ex.getMessage());
        } catch (RuntimeException ex) {
            Ui.erro(this, "Falha ao fechar a contagem.", ex);
        }
    }

    private int estoqueAtualDe(ContagemItem item) {
        return aplicacao.getProdutoRepository().buscarPorId(item.getProdutoId())
                .map(Produto::getEstoque)
                .orElse(item.getEstoqueAtual());
    }

    private ContagemItem itemSelecionado() {
        int linha = tabelaItens.getSelectedRow();
        if (linha < 0) {
            return null;
        }
        int indice = tabelaItens.convertRowIndexToModel(linha);
        if (indice < 0 || indice >= itens.size()) {
            return null;
        }
        return itens.get(indice);
    }

    private void carregar() {
        try {
            contagemAberta = aplicacao.getInventario().buscarContagemAberta();
            itens = contagemAberta == null
                    ? new ArrayList<>()
                    : aplicacao.getInventario().listarItens(contagemAberta.getId());
            carregarHistorico();
        } catch (RuntimeException ex) {
            situacao.setText("Falha ao carregar a contagem.");
            Ui.erro(this, "Falha ao carregar a contagem.", ex);
            return;
        }
        modeloItens.setRowCount(0);
        int contados = 0;
        for (ContagemItem item : itens) {
            if (item.foiContado()) {
                contados++;
            }
            modeloItens.addRow(new Object[]{
                    item.getProdutoNome() + " (" + item.getProdutoCodigoBarras() + ")",
                    item.getEstoqueAtual(),
                    item.foiContado() ? item.getQuantidadeContada() : "-",
                    item.previaDeDiferenca() == null ? "-" : formatarPrevisao(item.previaDeDiferenca()),
                    situacaoItem(item)
            });
        }
        if (contagemAberta == null) {
            situacao.setText("Nenhuma contagem em aberto.");
        } else {
            situacao.setText("Contagem " + contagemAberta.getNumero() + " em aberto desde "
                    + Ui.dataHora(contagemAberta.getDataHora().toString())
                    + " - responsável: " + contagemAberta.getUsuarioNome() + ".");
        }
        progresso.setText(contagemAberta == null
                ? "Inicie uma contagem para registrar as quantidades."
                : contados + " de " + itens.size() + " item(ns) contados.");
        boolean aberta = contagemAberta != null;
        informar.setEnabled(aberta);
        fechar.setEnabled(aberta);
        observacao.setEnabled(aberta);
    }

    private void carregarHistorico() {
        modeloHistorico.setRowCount(0);
        for (ContagemInventario contagem : aplicacao.getInventario().listarContagens()) {
            if (contagem.estaAberta()) {
                continue;
            }
            modeloHistorico.addRow(new Object[]{
                    contagem.getNumero(),
                    contagem.getDataHora() == null ? "-" : Ui.dataHora(contagem.getDataHora().toString()),
                    contagem.getDataFechamento() == null ? "-"
                            : Ui.dataHora(contagem.getDataFechamento().toString()),
                    contagem.getUsuarioNome() == null ? "" : contagem.getUsuarioNome(),
                    contagem.getObservacao() == null ? "" : contagem.getObservacao()
            });
        }
    }

    private String situacaoItem(ContagemItem item) {
        if (item.isAjustado()) {
            return "Ajustado";
        }
        return item.foiContado() ? "Contado" : "Não contado";
    }

    private String formatarPrevisao(int previsao) {
        if (previsao > 0) {
            return "+" + previsao;
        }
        return String.valueOf(previsao);
    }

    public int totalLinhas() {
        return modeloItens.getRowCount();
    }

    public Object valor(int linha, int coluna) {
        return modeloItens.getValueAt(linha, coluna);
    }

    public String textoSituacao() {
        return situacao.getText();
    }

    public JLabel componenteSituacao() {
        return situacao;
    }

    public String textoProgresso() {
        return progresso.getText();
    }

    public int totalLinhasHistorico() {
        return modeloHistorico.getRowCount();
    }
}
