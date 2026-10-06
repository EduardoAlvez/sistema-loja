package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.model.StatusVenda;
import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.model.VendaItem;
import com.portfolio.sistemaloja.repository.ProdutoEstoque;
import com.portfolio.sistemaloja.repository.ProdutoVendado;
import com.portfolio.sistemaloja.repository.VendaDoDia;
import com.portfolio.sistemaloja.service.ExportacaoService;
import com.portfolio.sistemaloja.service.RelatorioService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SpinnerDateModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

public class PainelRelatorios extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LogManager.getLogger(PainelRelatorios.class);

    private final transient Aplicacao aplicacao;
    private final JSpinner inicio = dataSpinner();
    private final JSpinner fim = dataSpinner();

    private final JLabel cardVendas = cardValor();
    private final JLabel cardFaturamento = cardValor();
    private final JLabel cardTicket = cardValor();
    private final PainelGrafico grafico = new PainelGrafico();

    private final javax.swing.table.DefaultTableModel modeloVendas =
            Ui.modeloTabela("Número", "Data/Hora", "Cliente", "Operador", "Pagamento", "Total", "Status");
    private final JTable tabelaVendas = Ui.tabela(modeloVendas);
    private final javax.swing.table.DefaultTableModel modeloTop =
            Ui.modeloTabela("Produto", "Qtd. vendida", "Total vendido");
    private final JTable tabelaTop = Ui.tabela(modeloTop);
    private final javax.swing.table.DefaultTableModel modeloEstoque =
            Ui.modeloTabela("Produto", "Estoque", "Preço");
    private final JTable tabelaEstoque = Ui.tabela(modeloEstoque);
    private List<Venda> vendas = List.of();
    private List<ProdutoVendado> maisVendidos = List.of();
    private List<VendaDoDia> porDia = List.of();
    private RelatorioService.ResumoDashboard resumoAtual;
    private LocalDate periodoInicio;
    private LocalDate periodoFim;

    public PainelRelatorios(Aplicacao aplicacao) {
        this.aplicacao = aplicacao;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setBackground(Ui.COR_FUNDO);
        add(montarTopo(), BorderLayout.NORTH);
        add(montarCentro(), BorderLayout.CENTER);
        gerar();
    }

    private JPanel montarTopo() {
        JPanel topo = new JPanel(new BorderLayout());
        topo.setOpaque(false);
        topo.add(Ui.titulo("Relatórios"), BorderLayout.NORTH);

        JPanel periodo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        periodo.setOpaque(false);
        periodo.add(Ui.rotulo("De:"));
        periodo.add(inicio);
        periodo.add(Ui.rotulo("Até:"));
        periodo.add(fim);
        JButton gerar = new JButton("Gerar");
        gerar.setBackground(Ui.COR_PRINCIPAL);
        gerar.setForeground(Color.WHITE);
        gerar.addActionListener(e -> gerar());
        periodo.add(gerar);

        JButton cancelar = new JButton("Cancelar venda selecionada");
        cancelar.setBackground(Ui.COR_ALERTA);
        cancelar.setForeground(Color.WHITE);
        cancelar.addActionListener(e -> cancelarVenda());
        periodo.add(cancelar);

        JButton pdf = new JButton("Exportar PDF");
        pdf.setToolTipText("Gera o relatório do período em PDF");
        pdf.addActionListener(e -> exportar("pdf"));
        JButton excel = new JButton("Exportar Excel");
        excel.setToolTipText("Gera o relatório do período em planilha .xlsx");
        excel.addActionListener(e -> exportar("xlsx"));
        periodo.add(pdf);
        periodo.add(excel);

        topo.add(periodo, BorderLayout.CENTER);
        return topo;
    }

    private JTabbedPane montarCentro() {
        JTabbedPane abas = new JTabbedPane();
        abas.addTab("Resumo", montarResumo());
        abas.addTab("Vendas", new JScrollPane(tabelaVendas));
        abas.addTab("Mais vendidos", new JScrollPane(tabelaTop));
        abas.addTab("Estoque baixo", new JScrollPane(tabelaEstoque));
        return abas;
    }

    private JPanel montarResumo() {
        JPanel painel = new JPanel(new BorderLayout(0, 12));
        painel.setBackground(Ui.COR_FUNDO);
        painel.setBorder(BorderFactory.createEmptyBorder(12, 4, 4, 4));

        JPanel cards = new JPanel(new java.awt.GridLayout(1, 3, 12, 0));
        cards.setOpaque(false);
        cards.add(card("Vendas no período", cardVendas, Ui.COR_PRINCIPAL));
        cards.add(card("Faturamento", cardFaturamento, Ui.COR_OK));
        cards.add(card("Ticket médio", cardTicket, new Color(130, 90, 160)));
        painel.add(cards, BorderLayout.NORTH);

        painel.add(grafico, BorderLayout.CENTER);

        JLabel dica = Ui.rotulo("A aba Vendas mostra o histórico detalhado; selecione uma linha para cancelar "
                + "uma venda (o estoque é devolvido automaticamente).");
        dica.setForeground(new Color(110, 118, 126));
        painel.add(dica, BorderLayout.SOUTH);
        return painel;
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
        return painel;
    }

    private JLabel cardValor() {
        JLabel valor = new JLabel("-");
        valor.setFont(valor.getFont().deriveFont(Font.BOLD, 24f));
        return valor;
    }

    private JSpinner dataSpinner() {
        SpinnerDateModel modelo = new SpinnerDateModel(new Date(), null, null, java.util.Calendar.DAY_OF_MONTH);
        JSpinner spinner = new JSpinner(modelo);
        spinner.setEditor(new JSpinner.DateEditor(spinner, "dd/MM/yyyy"));
        return spinner;
    }

    private LocalDate dataDo(JSpinner spinner) {
        Date data = (Date) spinner.getValue();
        return data.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public final void gerar() {
        LocalDate de = dataDo(inicio);
        LocalDate ate = dataDo(fim);
        try {
            RelatorioService.ResumoDashboard resumo = aplicacao.getRelatorios().resumoPeriodo(de, ate);
            resumoAtual = resumo;
            periodoInicio = de;
            periodoFim = ate;
            cardVendas.setText(String.valueOf(resumo.vendas()));
            cardFaturamento.setText(Ui.moeda(resumo.faturamento()));
            cardTicket.setText(Ui.moeda(resumo.ticketMedio()));

            vendas = aplicacao.getRelatorios().vendasDoPeriodo(de, ate);
            modeloVendas.setRowCount(0);
            for (Venda venda : vendas) {
                modeloVendas.addRow(new Object[]{
                        venda.getNumero(),
                        Ui.dataHora(venda.getDataHora()),
                        venda.getClienteNome() == null ? "-" : venda.getClienteNome(),
                        venda.getUsuarioNome() == null ? "-" : venda.getUsuarioNome(),
                        venda.getFormaPagamento().getDescricao(),
                        Ui.moeda(venda.getTotal()),
                        venda.getStatus().getDescricao()
                });
            }

            List<ProdutoVendado> top = aplicacao.getRelatorios().maisVendidos(de, ate);
            maisVendidos = top;
            modeloTop.setRowCount(0);
            top.forEach(p -> modeloTop.addRow(new Object[]{p.nome(), p.quantidade(), Ui.moeda(p.totalVendido())}));

            porDia = aplicacao.getRelatorios().faturamentoPorDia(de, ate);
            grafico.setDados(porDia);

            List<ProdutoEstoque> baixo = aplicacao.getRelatorios().estoqueBaixo();
            modeloEstoque.setRowCount(0);
            baixo.forEach(p -> modeloEstoque.addRow(new Object[]{p.nome(), p.estoque(), Ui.moeda(p.precoVenda())}));
        } catch (RuntimeException e) {
            Ui.erro(this, e.getMessage(), e);
        }
    }

    private void exportar(String extensao) {
        if (resumoAtual == null || periodoInicio == null) {
            gerar();
        }
        ExportacaoService.DadosRelatorio dados = new ExportacaoService.DadosRelatorio(
                periodoInicio, periodoFim, resumoAtual, vendas, maisVendidos, porDia);

        JFileChooser seletor = new JFileChooser();
        seletor.setDialogTitle("Exportar relatório de vendas");
        seletor.setSelectedFile(new File(aplicacao.getExportacao().nomeSugerido(dados, extensao)));
        seletor.setFileFilter(new FileNameExtensionFilter(
                "Arquivo ." + extensao, extensao));
        if (seletor.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File arquivo = seletor.getSelectedFile();
        if (!arquivo.getName().toLowerCase().endsWith("." + extensao)) {
            arquivo = new File(arquivo.getAbsolutePath() + "." + extensao);
        }
        try {
            if ("pdf".equals(extensao)) {
                aplicacao.getExportacao().exportarPdf(arquivo, dados);
            } else {
                aplicacao.getExportacao().exportarExcel(arquivo, dados);
            }
            Ui.info(this, "Relatório exportado com sucesso:\n" + arquivo.getAbsolutePath());
            LOG.info("Relatorio exportado: {}", arquivo.getAbsolutePath());
        } catch (ExportacaoService.ExportacaoException e) {
            Ui.erro(this, e.getMessage(), e);
        }
    }

    private void cancelarVenda() {
        int linha = tabelaVendas.getSelectedRow();
        if (linha < 0) {
            Ui.info(this, "Selecione uma venda na tabela.");
            return;
        }
        Venda venda = vendas.get(tabelaVendas.convertRowIndexToModel(linha));
        if (venda.getStatus() == StatusVenda.CANCELADA) {
            Ui.info(this, "Esta venda já está cancelada.");
            return;
        }
        if (!Ui.confirma(this, "Cancelar a venda " + venda.getNumero() + "? "
                + "O estoque dos produtos será devolvido.")) {
            return;
        }
        try {
            aplicacao.getVendas().cancelar(venda.getId());
            Ui.info(this, "Venda cancelada e estoque devolvido.");
            gerar();
        } catch (RuntimeException e) {
            Ui.erro(this, "Falha ao cancelar: " + e.getMessage(), e);
        }
    }

    public void mostrarDetalhes(Venda venda) {
        StringBuilder texto = new StringBuilder("Venda " + venda.getNumero() + "\n\n");
        for (VendaItem item : venda.getItens()) {
            texto.append(item.getQuantidade()).append(" x ")
                    .append(item.getProdutoNome()).append(" = ")
                    .append(Ui.moeda(item.getSubtotal())).append('\n');
        }
        texto.append("\nTotal: ").append(Ui.moeda(venda.getTotal()));
        Ui.info(this, texto.toString());
    }
}
