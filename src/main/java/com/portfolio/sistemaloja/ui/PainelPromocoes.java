package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.model.PromocaoProduto;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class PainelPromocoes extends JPanel {

    private static final long serialVersionUID = 1L;

    private final transient Aplicacao aplicacao;
    private final DefaultTableModel modelo = Ui.modeloTabela(
            "Produto", "Promoção", "Qtd. mín.", "Preço promo", "Vigência", "Dias", "Horário", "Situação");
    private final JTable tabela = Ui.tabela(modelo);
    private final JLabel alerta = new JLabel();
    private List<PromocaoProduto> linhas = List.of();

    public PainelPromocoes(Aplicacao aplicacao) {
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
        titulo.add(Ui.titulo("Promoções por quantidade"), BorderLayout.WEST);
        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acoes.setOpaque(false);

        JButton nova = new JButton("Nova");
        nova.setBackground(Ui.COR_PRINCIPAL);
        nova.setForeground(java.awt.Color.WHITE);
        nova.setToolTipText("Cria uma promoção que ativa a partir de uma quantidade");
        nova.addActionListener(e -> editar(null));

        JButton editar = new JButton("Editar");
        editar.addActionListener(e -> editar(selecionada()));

        JButton remover = new JButton("Remover");
        remover.setBackground(Ui.COR_ALERTA);
        remover.setForeground(java.awt.Color.WHITE);
        remover.addActionListener(e -> remover());

        JButton atualizar = new JButton("Atualizar");
        atualizar.addActionListener(e -> atualizar());

        acoes.add(nova);
        acoes.add(editar);
        acoes.add(remover);
        acoes.add(atualizar);
        titulo.add(acoes, BorderLayout.EAST);

        alerta.setForeground(Ui.COR_ALERTA);
        JPanel rodape = new JPanel(new BorderLayout());
        rodape.setOpaque(false);
        rodape.add(alerta, BorderLayout.WEST);

        topo.add(titulo, BorderLayout.NORTH);
        topo.add(rodape, BorderLayout.SOUTH);
        return topo;
    }

    public final void atualizar() {
        linhas = aplicacao.getPromocoes().listar();
        modelo.setRowCount(0);
        for (PromocaoProduto promocao : linhas) {
            modelo.addRow(new Object[]{
                    promocao.getProdutoNome() == null ? "-" : promocao.getProdutoNome(),
                    promocao.getNome(),
                    promocao.getQuantidadeMinima(),
                    Ui.moeda(promocao.getPrecoPromocional()),
                    vigencia(promocao),
                    dias(promocao),
                    horario(promocao),
                    situacao(promocao)
            });
        }
        int ativas = (int) linhas.stream().filter(PromocaoProduto::isAtiva).count();
        alerta.setText(ativas + " promoção(ões) ativas");
    }

    private String vigencia(PromocaoProduto promocao) {
        LocalDate inicio = promocao.getDataInicio();
        LocalDate fim = promocao.getDataFim();
        if (inicio == null && fim == null) {
            return "Sempre";
        }
        if (inicio == null) {
            return "até " + Ui.data(fim);
        }
        if (fim == null) {
            return "a partir de " + Ui.data(inicio);
        }
        return Ui.data(inicio) + " a " + Ui.data(fim);
    }

    private String dias(PromocaoProduto promocao) {
        if (promocao.getDiasSemana().isEmpty()) {
            return "Todos";
        }
        StringBuilder texto = new StringBuilder();
        for (DayOfWeek dia : promocao.getDiasSemana()) {
            if (texto.length() > 0) {
                texto.append(", ");
            }
            texto.append(dia.getDisplayName(java.time.format.TextStyle.SHORT,
                    java.util.Locale.forLanguageTag("pt-BR")));
        }
        return texto.toString();
    }

    private String horario(PromocaoProduto promocao) {
        LocalTime inicio = promocao.getHorarioInicio();
        LocalTime fim = promocao.getHorarioFim();
        if (inicio == null || fim == null) {
            return "Dia inteiro";
        }
        return inicio.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                + " a " + fim.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
    }

    private String situacao(PromocaoProduto promocao) {
        if (!promocao.isAtiva()) {
            return "Inativa";
        }
        return aplicacao.getPromocoes().dentroDaProgramacao(promocao) ? "Em vigor" : "Programada";
    }

    public int totalLinhas() {
        return modelo.getRowCount();
    }

    public Object valor(int linha, int coluna) {
        return modelo.getValueAt(linha, coluna);
    }

    public String textoAlerta() {
        return alerta.getText();
    }

    private PromocaoProduto selecionada() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) {
            Ui.info(this, "Selecione uma promoção na tabela.");
            return null;
        }
        return linhas.get(tabela.convertRowIndexToModel(linha));
    }

    private void editar(PromocaoProduto promocao) {
        PromocaoProduto alvo = promocao == null ? new PromocaoProduto() : promocao;
        Window pai = javax.swing.SwingUtilities.getWindowAncestor(this);
        DialogoPromocao dialogo = new DialogoPromocao(pai, aplicacao, alvo, this::atualizar);
        dialogo.setVisible(true);
    }

    private void remover() {
        PromocaoProduto promocao = selecionada();
        if (promocao == null) {
            return;
        }
        if (!Ui.confirma(this, "Remover a promoção \"" + promocao.getNome() + "\"?")) {
            return;
        }
        try {
            aplicacao.getPromocoes().remover(promocao.getId());
            atualizar();
        } catch (RuntimeException e) {
            Ui.erro(this, e.getMessage(), e);
        }
    }
}
