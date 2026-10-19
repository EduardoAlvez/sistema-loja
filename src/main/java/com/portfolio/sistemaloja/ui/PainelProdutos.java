package com.portfolio.sistemaloja.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.model.Categoria;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.service.ProdutoService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PainelProdutos extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LogManager.getLogger(PainelProdutos.class);

    private static final String TODAS = "Todas as categorias";

    private final transient Aplicacao aplicacao;
    private final JTextField busca = new JTextField(24);
    private final JComboBox<Object> filtroCategoria = new JComboBox<>();
    private final JCheckBox apenasEstoqueBaixo = new JCheckBox("Somente estoque baixo");
    private final JLabel alerta = new JLabel();
    private boolean ajustandoFiltro;
    private final javax.swing.table.DefaultTableModel modelo =
            Ui.modeloTabela("Código", "Produto", "Categoria", "Custo", "Venda", "Margem", "Estoque", "Mín.", "Situação");
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
        atualizar.addActionListener(e -> aposSalvar());

        JButton importar = new JButton("Importar CSV");
        importar.setToolTipText("Importa produtos de um arquivo CSV (UTF-8, separador ;)");
        importar.addActionListener(e -> importarCsv());

        JButton exportar = new JButton("Exportar CSV");
        exportar.setToolTipText("Exporta a lista atual em um arquivo CSV");
        exportar.addActionListener(e -> exportarCsv());

        acoes.add(novo);
        acoes.add(editar);
        acoes.add(excluir);
        acoes.add(importar);
        acoes.add(exportar);
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

        recarregarCategorias();
        filtroCategoria.setToolTipText("Mostra apenas os produtos da categoria escolhida");
        filtroCategoria.addActionListener(e -> {
            if (!ajustandoFiltro) {
                atualizar();
            }
        });
        apenasEstoqueBaixo.setOpaque(false);
        apenasEstoqueBaixo.setToolTipText("Mostra apenas produtos no limite do estoque mínimo");
        apenasEstoqueBaixo.addItemListener(e -> atualizar());
        linhaBusca.add(Ui.rotulo("Categoria:"));
        linhaBusca.add(filtroCategoria);
        linhaBusca.add(apenasEstoqueBaixo);
        linhaBusca.add(alerta);

        topo.add(titulo, BorderLayout.NORTH);
        topo.add(linhaBusca, BorderLayout.CENTER);
        return topo;
    }

    private void recarregarCategorias() {
        ajustandoFiltro = true;
        try {
            Long atual = categoriaSelecionada();
            filtroCategoria.removeAllItems();
            filtroCategoria.addItem(TODAS);
            for (Categoria categoria : aplicacao.getProdutos().listarCategorias()) {
                filtroCategoria.addItem(categoria);
            }
            selecionarCategoria(atual);
        } finally {
            ajustandoFiltro = false;
        }
    }

    private void aposSalvar() {
        recarregarCategorias();
        atualizar();
    }

    private Long categoriaSelecionada() {
        Object item = filtroCategoria.getSelectedItem();
        return item instanceof Categoria categoria ? categoria.getId() : null;
    }

    public void selecionarCategoria(Long categoriaId) {
        for (int i = 0; i < filtroCategoria.getItemCount(); i++) {
            Object item = filtroCategoria.getItemAt(i);
            if (item instanceof Categoria categoria && categoria.getId().equals(categoriaId)) {
                filtroCategoria.setSelectedIndex(i);
                return;
            }
        }
        filtroCategoria.setSelectedIndex(0);
    }

    public void somenteEstoqueBaixo(boolean valor) {
        apenasEstoqueBaixo.setSelected(valor);
    }

    public int totalLinhas() {
        return modelo.getRowCount();
    }

    public Object valor(int linha, int coluna) {
        return modelo.getValueAt(linha, coluna);
    }

    public String situacao(int linha) {
        return String.valueOf(modelo.getValueAt(linha, 8));
    }

    public String textoAlerta() {
        return alerta.getText();
    }

    public final void atualizar() {
        linhas = aplicacao.getProdutos().listar(busca.getText(), categoriaSelecionada());
        if (apenasEstoqueBaixo.isSelected()) {
            linhas = linhas.stream().filter(Produto::estoqueBaixo).toList();
        }
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
                    produto.getEstoqueMinimo(),
                    situacaoDe(produto)
            });
        }
        atualizarAlerta();
    }

    private String situacaoDe(Produto produto) {
        if (!produto.isAtivo()) {
            return "Inativo";
        }
        return produto.estoqueBaixo() ? "Estoque baixo" : "Ativo";
    }

    private void atualizarAlerta() {
        int baixos = aplicacao.getProdutos().listarComEstoqueBaixo().size();
        alerta.setText(baixos == 0 ? "" : baixos + " produto(s) com estoque baixo");
        alerta.setForeground(Ui.COR_ALERTA);
    }

    private void importarCsv() {
        JFileChooser seletor = new JFileChooser();
        seletor.setDialogTitle("Importar produtos de CSV");
        seletor.setFileFilter(new FileNameExtensionFilter("Arquivo CSV", "csv"));
        if (seletor.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File arquivo = seletor.getSelectedFile();
        try {
            String conteudo = Files.readString(arquivo.toPath(), StandardCharsets.UTF_8);
            ProdutoService.ResultadoCsv resultado = aplicacao.getProdutos().importarCsv(conteudo);
            atualizar();
            LOG.info("CSV importado de {}: {} produto(s), {} linha(s) com erro",
                    arquivo.getName(), resultado.importadas(), resultado.erros().size());
            Ui.info(this, montarResumoImportacao(resultado));
        } catch (IOException e) {
            Ui.erro(this, "Falha ao ler o arquivo: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            Ui.erro(this, "Falha ao importar: " + e.getMessage(), e);
        }
    }

    private String montarResumoImportacao(ProdutoService.ResultadoCsv resultado) {
        StringBuilder mensagem = new StringBuilder(resultado.importadas() + " produto(s) importado(s).");
        if (!resultado.erros().isEmpty()) {
            mensagem.append("\n\nLinhas com erro (").append(resultado.erros().size()).append("):");
            resultado.erros().stream().limit(10).forEach(erro -> mensagem.append("\n").append(erro));
            if (resultado.erros().size() > 10) {
                mensagem.append("\n... e mais ").append(resultado.erros().size() - 10).append(".");
            }
        }
        return mensagem.toString();
    }

    private void exportarCsv() {
        if (linhas.isEmpty()) {
            Ui.info(this, "Nenhum produto para exportar.");
            return;
        }
        JFileChooser seletor = new JFileChooser();
        seletor.setDialogTitle("Exportar produtos em CSV");
        seletor.setSelectedFile(new File("produtos.csv"));
        seletor.setFileFilter(new FileNameExtensionFilter("Arquivo CSV", "csv"));
        if (seletor.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File arquivo = seletor.getSelectedFile();
        if (!arquivo.getName().toLowerCase(Locale.ROOT).endsWith(".csv")) {
            arquivo = new File(arquivo.getAbsolutePath() + ".csv");
        }
        if (arquivo.exists() && !Ui.confirma(this, "Substituir " + arquivo.getName() + "?")) {
            return;
        }
        try {
            String csv = "\uFEFF" + aplicacao.getProdutos().exportarCsv(linhas);
            Files.writeString(arquivo.toPath(), csv, StandardCharsets.UTF_8);
            LOG.info("CSV exportado para {} ({} produto(s))", arquivo.getName(), linhas.size());
            Ui.info(this, "Produtos exportados:\n" + arquivo.getAbsolutePath());
        } catch (IOException e) {
            Ui.erro(this, "Falha ao gravar o arquivo: " + e.getMessage(), e);
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
                javax.swing.SwingUtilities.getWindowAncestor(this), aplicacao, alvo, this::aposSalvar);
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
