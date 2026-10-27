package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.model.Autorizacao;
import com.portfolio.sistemaloja.model.StatusVenda;
import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.model.VendaItem;
import com.portfolio.sistemaloja.repository.ComparativoPeriodo;
import com.portfolio.sistemaloja.repository.CurvaABC;
import com.portfolio.sistemaloja.repository.EventoAuditoria;
import com.portfolio.sistemaloja.repository.GiroEstoque;
import com.portfolio.sistemaloja.repository.ProdutoEstoque;
import com.portfolio.sistemaloja.repository.ProdutoSemMovimento;
import com.portfolio.sistemaloja.repository.SugestaoCompra;
import com.portfolio.sistemaloja.repository.ProdutoVendado;
import com.portfolio.sistemaloja.repository.ResumoOperador;
import com.portfolio.sistemaloja.repository.VendaDoDia;
import com.portfolio.sistemaloja.repository.VendaHora;
import com.portfolio.sistemaloja.service.AutorizacaoNecessariaException;
import com.portfolio.sistemaloja.service.ExportacaoService;
import com.portfolio.sistemaloja.service.RelatorioService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

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
    private final javax.swing.table.DefaultTableModel modeloOperador =
            Ui.modeloTabela("Operador", "Comissão", "Vendas", "Total vendido", "Comissão a pagar");
    private final JTable tabelaOperador = Ui.tabela(modeloOperador);
    private final javax.swing.table.DefaultTableModel modeloAuditoria =
            Ui.modelo("Data/Hora", "Usuário", "Ação", "Entidade", "Detalhe");
    private final JTable tabelaAuditoria = Ui.tabela(modeloAuditoria);
    private final JTabbedPane abas = new JTabbedPane();
    private List<Venda> vendas = List.of();
    private List<ProdutoVendado> maisVendidos = List.of();
    private List<VendaDoDia> porDia = List.of();
    private RelatorioService.ResumoDashboard resumoAtual;
    private LocalDate periodoInicio;
    private LocalDate periodoFim;

    /* ---------- Indicadores ---------- */
    private final javax.swing.table.DefaultTableModel modeloVendaHora =
            Ui.modeloTabela("Hora", "Qtd. vendas", "Total");
    private final JTable tabelaVendaHora = Ui.tabela(modeloVendaHora);
    private final javax.swing.table.DefaultTableModel modeloCurvaProdutos =
            Ui.modeloTabela("Produto", "Qtd.", "Total", "%", "Classe");
    private final JTable tabelaCurvaProdutos = Ui.tabela(modeloCurvaProdutos);
    private final javax.swing.table.DefaultTableModel modeloCurvaCategorias =
            Ui.modeloTabela("Categoria", "Qtd.", "Total", "%", "Classe");
    private final JTable tabelaCurvaCategorias = Ui.tabela(modeloCurvaCategorias);
    private final javax.swing.table.DefaultTableModel modeloGiroEstoque =
            Ui.modeloTabela("Produto", "Categoria", "Estoque", "Mínimo", "Vendas", "Giro", "Ruptura");
    private final JTable tabelaGiroEstoque = Ui.tabela(modeloGiroEstoque);
    private final javax.swing.table.DefaultTableModel modeloSemMovimento =
            Ui.modeloTabela("Produto", "Estoque", "Última venda");
    private final JTable tabelaSemMovimento = Ui.tabela(modeloSemMovimento);
    private final javax.swing.table.DefaultTableModel modeloComparativo =
            Ui.modeloTabela("Indicador", "Período atual", "Período anterior", "Variação", "%");
    private final JTable tabelaComparativo = Ui.tabela(modeloComparativo);
    private final javax.swing.table.DefaultTableModel modeloSugestao =
            Ui.modeloTabela("Produto", "Estoque", "Estoque mín.", "Vendido no período", "Média/dia", "Qtd. sugerida");
    private final JTable tabelaSugestao = Ui.tabela(modeloSugestao);

    private List<VendaHora> listaVendaHora = List.of();
    private List<CurvaABC> listaCurvaProdutos = List.of();
    private List<CurvaABC> listaCurvaCategorias = List.of();
    private List<GiroEstoque> listaGiroEstoque = List.of();
    private List<ProdutoSemMovimento> listaSemMovimento = List.of();
    private ComparativoPeriodo comparativo = null;

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
        JButton sugestao = new JButton("Sugestão de compra");
        sugestao.addActionListener(e -> sugestaoCompra());
        periodo.add(sugestao);

        topo.add(periodo, BorderLayout.CENTER);
        return topo;
    }

    private JTabbedPane montarCentro() {
        abas.removeAll();
        abas.addTab("Resumo", montarResumo());
        abas.addTab("Vendas", new JScrollPane(tabelaVendas));
        abas.addTab("Mais vendidos", new JScrollPane(tabelaTop));
        abas.addTab("Estoque baixo", new JScrollPane(tabelaEstoque));
        abas.addTab("Sugestão de compra", montarSugestaoCompra());
        abas.addTab("Por operador", new JScrollPane(tabelaOperador));
        abas.addTab("Indicadores", montarIndicadores());
        if (eAdmin()) {
            abas.addTab("Auditoria", new JScrollPane(tabelaAuditoria));
        }
        return abas;
    }

    private boolean eAdmin() {
        return aplicacao.getUsuarioAtual() != null && aplicacao.getUsuarioAtual().isAdmin();
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

            List<ResumoOperador> porOperador = aplicacao.getRelatorios().resumoPorOperador(de, ate);
            modeloOperador.setRowCount(0);
            for (ResumoOperador operador : porOperador) {
                modeloOperador.addRow(new Object[]{
                        operador.nome(),
                        operador.comissaoPercentual().stripTrailingZeros().toPlainString() + "%",
                        operador.vendas(),
                        Ui.moeda(operador.totalVendido()),
                        Ui.moeda(operador.comissao())
                });
            }
            if (!porOperador.isEmpty()) {
                modeloOperador.addRow(new Object[]{"Total", "", "", "",
                        Ui.moeda(aplicacao.getRelatorios().comissaoTotal(de, ate))});
            }

            if (eAdmin()) {
                preencherAuditoria(aplicacao.getAuditoria().listar(de, ate));
            }

            /* ---------- Indicadores ---------- */
            listaVendaHora = aplicacao.getRelatorios().vendasPorHora(de, ate);
            modeloVendaHora.setRowCount(0);
            for (VendaHora h : listaVendaHora) {
                modeloVendaHora.addRow(new Object[]{
                        h.hora() + "h",
                        h.quantidade(),
                        Ui.moeda(h.total())
                });
            }

            listaCurvaProdutos = aplicacao.getRelatorios().curvaABCProdutos(de, ate);
            modeloCurvaProdutos.setRowCount(0);
            for (CurvaABC c : listaCurvaProdutos) {
                modeloCurvaProdutos.addRow(new Object[]{
                        c.nome(),
                        c.quantidade(),
                        Ui.moeda(c.total()),
                        c.percentual() + "%",
                        c.classe()
                });
            }

            listaCurvaCategorias = aplicacao.getRelatorios().curvaABCCategorias(de, ate);
            modeloCurvaCategorias.setRowCount(0);
            for (CurvaABC c : listaCurvaCategorias) {
                modeloCurvaCategorias.addRow(new Object[]{
                        c.nome(),
                        c.quantidade(),
                        Ui.moeda(c.total()),
                        c.percentual() + "%",
                        c.classe()
                });
            }

            listaGiroEstoque = aplicacao.getRelatorios().giroEstoque(de, ate);
            modeloGiroEstoque.setRowCount(0);
            for (GiroEstoque g : listaGiroEstoque) {
                modeloGiroEstoque.addRow(new Object[]{
                        g.nome(),
                        g.categoria(),
                        g.estoque(),
                        g.estoqueMinimo(),
                        g.vendasPeriodo(),
                        Ui.moeda(g.giro()),
                        g.ruptura() ? "Sim" : "Não"
                });
            }

            listaSemMovimento = aplicacao.getRelatorios().produtosSemMovimento(de, ate);
            modeloSemMovimento.setRowCount(0);
            for (ProdutoSemMovimento s : listaSemMovimento) {
                modeloSemMovimento.addRow(new Object[]{
                        s.nome(),
                        s.categoria(),
                        s.estoque(),
                        s.ultimoMovimento() == null ? "-" : Ui.data(s.ultimoMovimento())
                });
            }

            comparativo = aplicacao.getRelatorios().comparativoPeriodo(de, ate);
            modeloComparativo.setRowCount(0);
            modeloComparativo.addRow(new Object[]{
                    comparativo.rotulo(),
                    Ui.moeda(comparativo.total()),
                    comparativo.quantidade(),
                    Ui.moeda(comparativo.variacao()),
                    comparativo.variacaoPercentual() + "%"
            });
        } catch (RuntimeException e) {
            Ui.erro(this, e.getMessage(), e);
        }
    }

    private void preencherAuditoria(List<EventoAuditoria> eventos) {
        modeloAuditoria.setRowCount(0);
        eventos.forEach(evento -> modeloAuditoria.addRow(new Object[]{
                Ui.dataHora(evento.dataHora()),
                evento.usuarioLogin(),
                evento.acao(),
                evento.entidade() == null ? "" : evento.entidade(),
                evento.entidadeId() == null ? "" : evento.entidadeId(),
                evento.detalhe() == null ? "" : evento.detalhe()
        }));
    }

    private JPanel montarIndicadores() {
        JPanel painel = new JPanel();
        painel.setLayout(new BoxLayout(painel, BoxLayout.Y_AXIS));
        painel.setBackground(Ui.COR_FUNDO);
        painel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        painel.add(Ui.titulo("Indicadores de varejo"));
        painel.add(Box.createRigidArea(new java.awt.Dimension(0, 8)));

        painel.add(subsecaoIndicador("Vendas por hora do dia", tabelaVendaHora));
        painel.add(subsecaoIndicador("Curva ABC – produtos", tabelaCurvaProdutos));
        painel.add(subsecaoIndicador("Curva ABC – categorias", tabelaCurvaCategorias));
        painel.add(subsecaoIndicador("Giro de estoque e ruptura", tabelaGiroEstoque));
        painel.add(subsecaoIndicador("Produtos sem movimento", tabelaSemMovimento));
        painel.add(subsecaoIndicador("Comparativo de período", tabelaComparativo));

        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(false);
        container.add(new JScrollPane(painel), BorderLayout.CENTER);
        return container;
    }

    private JPanel subsecaoIndicador(String titulo, JTable tabela) {
        JPanel painel = new JPanel(new BorderLayout(0, 4));
        painel.setBackground(Ui.COR_FUNDO);
        painel.setBorder(BorderFactory.createEmptyBorder(4, 0, 8, 0));
        JLabel rotulo = Ui.rotulo(titulo);
        rotulo.setFont(rotulo.getFont().deriveFont(Font.BOLD, 13f));
        rotulo.setForeground(new Color(110, 118, 126));
        painel.add(rotulo, BorderLayout.NORTH);
        painel.add(new JScrollPane(tabela), BorderLayout.CENTER);
        return painel;
    }

    private void exportar(String extensao) {
        ExportacaoService.DadosRelatorio dados = new ExportacaoService.DadosRelatorio(
                periodoInicio, periodoFim, resumoAtual, vendas, maisVendidos, porDia,
                listaVendaHora, listaCurvaProdutos, listaCurvaCategorias,
                listaGiroEstoque, listaSemMovimento, comparativo);

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
        } catch (AutorizacaoNecessariaException e) {
            Optional<Autorizacao> autorizacao = DialogoAutorizacao.pedir(SwingUtilities.getWindowAncestor(this), aplicacao, e.getMotivo());
            if (autorizacao.isEmpty()) {
                return;
            }
            try {
                aplicacao.getVendas().cancelar(venda.getId(), autorizacao.get());
                Ui.info(this, "Venda cancelada e estoque devolvido.");
                gerar();
            } catch (RuntimeException ex) {
                Ui.erro(this, "Falha ao cancelar: " + ex.getMessage(), ex);
            }
        } catch (RuntimeException ex) {
            Ui.erro(this, "Falha ao cancelar: " + ex.getMessage(), ex);
        }
    }

    private JPanel montarSugestaoCompra() {
        JPanel painel = new JPanel(new BorderLayout());
        painel.setOpaque(false);
        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topo.setOpaque(false);
        JButton atualizar = new JButton("Atualizar sugestão");
        atualizar.addActionListener(e -> carregarSugestao());
        topo.add(atualizar);
        painel.add(topo, BorderLayout.NORTH);
        painel.add(new JScrollPane(tabelaSugestao), BorderLayout.CENTER);
        carregarSugestao();
        return painel;
    }

    private void carregarSugestao() {
        modeloSugestao.setRowCount(0);
        try {
            LocalDate i = ((java.util.Date) inicio.getValue()).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            LocalDate f = ((java.util.Date) fim.getValue()).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            List<SugestaoCompra> lista = aplicacao.getRelatorios().sugestaoCompra(i, f);
            for (SugestaoCompra s : lista) {
                modeloSugestao.addRow(new Object[]{
                        s.nome(), s.estoqueAtual(), s.estoqueMinimo(), s.vendidoPeriodo(), s.mediaDiaria(), s.quantidadeSugerida()
                });
            }
        } catch (Exception ex) {
            // ignora
        }
    }

    private void sugestaoCompra() {
        carregarSugestao();
        abas.setSelectedIndex(3);
    }
}
