package com.portfolio.sistemaloja.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.model.Caixa;
import com.portfolio.sistemaloja.model.Cliente;
import com.portfolio.sistemaloja.model.FormaPagamento;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.TipoLancamentoCaixa;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.model.VendaItem;
import com.portfolio.sistemaloja.model.VendaPagamento;
import com.portfolio.sistemaloja.model.VendaPausada;
import com.portfolio.sistemaloja.service.CaixaService;
import com.portfolio.sistemaloja.service.ValidacaoException;

public class PainelPDV extends JPanel {

    private static final long serialVersionUID = 1L;

    private final transient Aplicacao aplicacao;
    private final transient Usuario usuario;

    private final JTextField busca = new JTextField(24);
    private final JComboBox<Cliente> cliente = new JComboBox<>();
    private final JComboBox<FormaPagamento> pagamento = new JComboBox<>(FormaPagamento.values());
    private final JTextField valorPago = new JTextField(12);
    private final JTextField desconto = new JTextField(8);
    private final JLabel statusPagamento = new JLabel(" ");
    private final JLabel total = new JLabel("R$ 0,00");
    private final JLabel troco = new JLabel("R$ 0,00");
    private final JLabel statusEstoque = new JLabel(" ");
    private final JLabel statusCaixa = new JLabel(" ");
    private final JButton botaoCaixa = new JButton("Abrir caixa");
    private final JButton botaoSangria = new JButton("Sangria");
    private final JButton botaoSuprimento = new JButton("Suprimento");
    private final JButton botaoFecharCaixa = new JButton("Fechar caixa");

    private final javax.swing.table.DefaultTableModel modeloProdutos =
            Ui.modeloTabela("Código", "Produto", "Estoque", "Preço");
    private final JTable tabelaProdutos = Ui.tabela(modeloProdutos);
    private List<Produto> produtos = List.of();

    private final List<VendaItem> carrinho = new ArrayList<>();
    private final List<VendaPagamento> pagamentosDivididos = new ArrayList<>();
    private BigDecimal subtotal = BigDecimal.ZERO;
    private final javax.swing.table.DefaultTableModel modeloCarrinho =
            new javax.swing.table.DefaultTableModel(
                    new Object[]{"Produto", "Qtd", "Desconto", "Preço unit.", "Subtotal"}, 0) {
                @Override
                public boolean isCellEditable(int linha, int coluna) {
                    return coluna == 1 || coluna == 2;
                }
            };
    private final JTable tabelaCarrinho = Ui.tabela(modeloCarrinho);

    public PainelPDV(Aplicacao aplicacao, Usuario usuario) {
        this.aplicacao = aplicacao;
        this.usuario = usuario;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setBackground(Ui.COR_FUNDO);
        add(montarTopo(), BorderLayout.NORTH);
        add(montarCentro(), BorderLayout.CENTER);
        add(montarRodape(), BorderLayout.SOUTH);
        configurarEventos();
        recarregarClientes();
        recarregarProdutos();
        atualizarStatusCaixa();
    }

    public void focarBusca() {
        SwingUtilities.invokeLater(busca::requestFocusInWindow);
    }

    private JPanel montarTopo() {
        JPanel topo = new JPanel(new BorderLayout(10, 8));
        topo.setOpaque(false);

        JPanel titulo = new JPanel(new BorderLayout());
        titulo.setOpaque(false);
        titulo.add(Ui.titulo("PDV - Ponto de Venda"), BorderLayout.WEST);

        JPanel linhaCliente = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        linhaCliente.setOpaque(false);
        statusCaixa.setFont(statusCaixa.getFont().deriveFont(Font.BOLD, 12f));
        linhaCliente.add(statusCaixa);
        botaoCaixa.addActionListener(e -> acaoCaixa());
        linhaCliente.add(botaoCaixa);
        botaoSangria.addActionListener(e -> movimentarCaixa(TipoLancamentoCaixa.SANGRIA));
        linhaCliente.add(botaoSangria);
        botaoSuprimento.addActionListener(e -> movimentarCaixa(TipoLancamentoCaixa.SUPRIMENTO));
        linhaCliente.add(botaoSuprimento);
        botaoFecharCaixa.addActionListener(e -> fecharCaixa());
        linhaCliente.add(botaoFecharCaixa);
        linhaCliente.add(Ui.rotulo("Cliente:"));
        cliente.setPreferredSize(new Dimension(240, cliente.getPreferredSize().height));
        linhaCliente.add(cliente);
        titulo.add(linhaCliente, BorderLayout.EAST);

        JPanel linhaBusca = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        linhaBusca.setOpaque(false);
        linhaBusca.add(Ui.rotulo("Produto (código ou nome):"));
        linhaBusca.add(busca);
        JButton adicionar = new JButton("Adicionar [Enter]");
        adicionar.setBackground(Ui.COR_PRINCIPAL);
        adicionar.setForeground(Color.WHITE);
        adicionar.addActionListener(e -> adicionarPeloCampo());
        linhaBusca.add(adicionar);
        linhaBusca.add(statusEstoque);

        topo.add(titulo, BorderLayout.NORTH);
        topo.add(linhaBusca, BorderLayout.CENTER);
        return topo;
    }

    private JSplitPane montarCentro() {
        JPanel esquerda = painel("Produtos encontrados - duplo clique para adicionar", new JScrollPane(tabelaProdutos));
        JPanel direita = painel("Carrinho", new JScrollPane(tabelaCarrinho));
        JSplitPane divisao = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, esquerda, direita);
        divisao.setResizeWeight(0.45);
        divisao.setDividerSize(8);
        divisao.setBorder(null);
        return divisao;
    }

    private JPanel painel(String titulo, JScrollPane rolagem) {
        JPanel painel = new JPanel(new BorderLayout(0, 6));
        painel.setOpaque(false);
        JLabel rotulo = new JLabel(titulo);
        rotulo.setFont(rotulo.getFont().deriveFont(Font.BOLD, 13f));
        painel.add(rotulo, BorderLayout.NORTH);
        rolagem.setBorder(BorderFactory.createLineBorder(new Color(224, 228, 232)));
        painel.add(rolagem, BorderLayout.CENTER);
        return painel;
    }

    private JPanel montarRodape() {
        JPanel rodape = new JPanel(new BorderLayout(10, 8));
        rodape.setOpaque(false);
        rodape.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(216, 220, 224)),
                BorderFactory.createEmptyBorder(10, 0, 0, 0)));

        JPanel pagamentoPainel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        pagamentoPainel.setOpaque(false);
        pagamentoPainel.add(Ui.rotulo("Pagamento:"));
        pagamento.addActionListener(e -> ajustarValorPago());
        pagamentoPainel.add(pagamento);
        pagamentoPainel.add(Ui.rotulo("Valor pago:"));
        valorPago.setToolTipText("Apenas para dinheiro; nos demais, o valor é o total da venda");
        pagamentoPainel.add(valorPago);
        pagamentoPainel.add(Ui.rotulo("Desconto:"));
        desconto.setToolTipText("Desconto aplicado no total da venda (ex.: 10 ou 10,50). "
                + "Limite do seu perfil: " + usuario.getPerfil().getPercentualMaximoDesconto() + "%");
        desconto.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                atualizarTotais();
            }
        });
        pagamentoPainel.add(desconto);
        statusPagamento.setForeground(Ui.COR_PRINCIPAL);
        pagamentoPainel.add(statusPagamento);

        JPanel totais = new JPanel(new GridLayout(2, 2, 12, 0));
        totais.setOpaque(false);
        JLabel rotuloTotal = Ui.rotulo("TOTAL");
        rotuloTotal.setFont(rotuloTotal.getFont().deriveFont(Font.BOLD, 14f));
        total.setFont(total.getFont().deriveFont(Font.BOLD, 26f));
        total.setForeground(Ui.COR_ESCURA);
        JLabel rotuloTroco = Ui.rotulo("Troco");
        totais.add(rotuloTotal);
        totais.add(rotuloTroco);
        totais.add(total);
        totais.add(troco);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botoes.setOpaque(false);
        JButton limpar = new JButton("Limpar carrinho");
        limpar.addActionListener(e -> limpar());
        JButton dividir = new JButton("Pagamento dividido");
        dividir.addActionListener(e -> abrirPagamentoDividido());
        JButton pausar = new JButton("Pausar venda");
        pausar.addActionListener(e -> pausarVenda());
        JButton retomar = new JButton("Retomar venda");
        retomar.addActionListener(e -> retomarVenda());
        JButton reimprimir = new JButton("Reimprimir cupom");
        reimprimir.addActionListener(e -> reimprimirCupom());
        JButton finalizar = new JButton("Finalizar venda");
        finalizar.setBackground(Ui.COR_OK);
        finalizar.setForeground(Color.WHITE);
        finalizar.setFont(finalizar.getFont().deriveFont(Font.BOLD, 14f));
        finalizar.addActionListener(e -> finalizar());
        botoes.add(limpar);
        botoes.add(dividir);
        botoes.add(pausar);
        botoes.add(retomar);
        botoes.add(reimprimir);
        botoes.add(finalizar);

        JPanel esquerda = new JPanel(new BorderLayout(10, 0));
        esquerda.setOpaque(false);
        esquerda.add(pagamentoPainel, BorderLayout.NORTH);

        rodape.add(esquerda, BorderLayout.WEST);
        rodape.add(totais, BorderLayout.CENTER);
        rodape.add(botoes, BorderLayout.EAST);
        return rodape;
    }

    private void atualizarStatusCaixa() {
        Caixa aberto = aplicacao.getCaixa().caixaAberto().orElse(null);
        botaoSangria.setEnabled(aberto != null);
        botaoSuprimento.setEnabled(aberto != null);
        botaoFecharCaixa.setEnabled(aberto != null);
        if (aberto == null) {
            statusCaixa.setForeground(Ui.COR_ALERTA);
            statusCaixa.setText("Caixa fechado");
            botaoCaixa.setText("Abrir caixa");
        } else {
            statusCaixa.setForeground(Ui.COR_OK);
            statusCaixa.setText("Caixa de " + Ui.dataHora(aberto.getDataAbertura()));
            botaoCaixa.setText("Caixa aberto");
        }
    }

    private void acaoCaixa() {
        Caixa aberto = aplicacao.getCaixa().caixaAberto().orElse(null);
        if (aberto != null) {
            Ui.info(this, "Caixa já aberto por " + aberto.getUsuarioAberturaNome()
                    + " em " + Ui.dataHora(aberto.getDataAbertura())
                    + " com fundo de " + Ui.moeda(aberto.getValorAbertura()) + ".");
            return;
        }
        String valor = javax.swing.JOptionPane.showInputDialog(this,
                "Valor de abertura do caixa:", "0,00");
        if (valor == null) {
            return;
        }
        try {
            aplicacao.getCaixa().abrir(parseMoeda(valor), usuario);
            atualizarStatusCaixa();
        } catch (ValidacaoException e) {
            Ui.erro(this, e.getMessage());
        }
    }

    private void movimentarCaixa(TipoLancamentoCaixa tipo) {
        Caixa aberto = aplicacao.getCaixa().caixaAberto().orElse(null);
        if (aberto == null) {
            Ui.erro(this, "Abra o caixa antes de registrar sangria ou suprimento.");
            return;
        }
        String valor = javax.swing.JOptionPane.showInputDialog(this,
                tipo.getDescricao() + " - valor:", "0,00");
        if (valor == null) {
            return;
        }
        String observacao = javax.swing.JOptionPane.showInputDialog(this,
                tipo.getDescricao() + " - observação (opcional):", "");
        if (observacao == null) {
            return;
        }
        try {
            aplicacao.getCaixa().lancar(tipo, parseMoeda(valor), observacao, usuario);
            Caixa atualizado = aplicacao.getCaixa().caixaAberto().orElse(aberto);
            Ui.info(this, tipo.getDescricao() + " registrado(a). Saldo em espécie: "
                    + Ui.moeda(aplicacao.getCaixa().saldoEspecie(atualizado)));
        } catch (ValidacaoException e) {
            Ui.erro(this, e.getMessage());
        }
    }

    private void fecharCaixa() {
        Caixa aberto = aplicacao.getCaixa().caixaAberto().orElse(null);
        if (aberto == null) {
            Ui.erro(this, "Não há caixa aberto para fechar.");
            return;
        }
        CaixaService.Resumo resumo = aplicacao.getCaixa().resumo(aberto);
        StringBuilder texto = new StringBuilder();
        texto.append("Resumo do caixa (aberto em ").append(Ui.dataHora(aberto.getDataAbertura())).append(")\n");
        texto.append("Vendas no turno: ").append(resumo.quantidadeVendas())
                .append(" - total ").append(Ui.moeda(resumo.totalVendas())).append('\n');
        resumo.porForma().forEach((forma, valor) ->
                texto.append("  ").append(forma.getDescricao()).append(": ")
                        .append(Ui.moeda(valor)).append('\n'));
        texto.append("Fundo inicial: ").append(Ui.moeda(resumo.fundoInicial())).append('\n');
        texto.append("Suprimentos: ").append(Ui.moeda(resumo.suprimentos())).append('\n');
        texto.append("Sangrias: ").append(Ui.moeda(resumo.sangrias())).append('\n');
        texto.append("Saldo esperado em espécie: ").append(Ui.moeda(resumo.saldoEsperado())).append('\n');

        String informado = javax.swing.JOptionPane.showInputDialog(this,
                texto + "\nValor em espécie contado no caixa:",
                resumo.saldoEsperado().toPlainString());
        if (informado == null) {
            return;
        }
        String observacao = javax.swing.JOptionPane.showInputDialog(this, "Observação do fechamento (opcional):", "");
        if (observacao == null) {
            return;
        }
        if (!Ui.confirma(this, "Confirma o fechamento do caixa?")) {
            return;
        }
        try {
            CaixaService.Resumo fechado = aplicacao.getCaixa().fechar(parseMoeda(informado), observacao, usuario);
            StringBuilder relatorio = new StringBuilder(texto);
            relatorio.append("Informado: ").append(Ui.moeda(fechado.valorInformado())).append('\n');
            BigDecimal diferenca = fechado.diferenca();
            relatorio.append("Diferença: ").append(Ui.moeda(diferenca))
                    .append(diferenca.compareTo(BigDecimal.ZERO) == 0 ? " (sem diferença)"
                            : diferenca.compareTo(BigDecimal.ZERO) > 0 ? " (sobra)" : " (falta)");
            atualizarStatusCaixa();
            Ui.info(this, relatorio.toString());
        } catch (ValidacaoException e) {
            Ui.erro(this, e.getMessage());
        }
    }

    private void configurarEventos() {
        busca.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    adicionarPeloCampo();
                }
            }
        });

        busca.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                recarregarProdutos();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                recarregarProdutos();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                recarregarProdutos();
            }
        });

        tabelaProdutos.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tabelaProdutos.getSelectedRow() >= 0) {
                    adicionarProduto(selecionadoNaLista());
                }
            }
        });

        modeloCarrinho.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == 1) {
                atualizarQuantidade(e.getFirstRow());
            } else if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == 2) {
                atualizarDescontoItem(e.getFirstRow());
            }
        });

        valorPago.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                calcularTroco();
            }
        });
    }

    private void adicionarPeloCampo() {
        String termo = busca.getText().strip();
        if (termo.isEmpty()) {
            return;
        }
        Produto porCodigo = null;
        for (Produto produto : produtos) {
            if (produto.getCodigoBarras().equalsIgnoreCase(termo)) {
                porCodigo = produto;
                break;
            }
        }
        if (porCodigo == null) {
            List<Produto> filtrados = aplicacao.getProdutos().listar(termo);
            if (filtrados.size() == 1) {
                porCodigo = filtrados.get(0);
            } else {
                recarregarProdutos();
                if (filtrados.isEmpty()) {
                    statusEstoque.setForeground(Ui.COR_ALERTA);
                    statusEstoque.setText("Nenhum produto encontrado.");
                } else {
                    statusEstoque.setForeground(new Color(110, 118, 126));
                    statusEstoque.setText("Selecione e dê duplo clique para adicionar.");
                }
                return;
            }
        }
        adicionarProduto(porCodigo);
        busca.setText("");
        busca.requestFocusInWindow();
    }

    private Produto selecionadoNaLista() {
        int linha = tabelaProdutos.getSelectedRow();
        if (linha < 0) {
            return null;
        }
        return produtos.get(tabelaProdutos.convertRowIndexToModel(linha));
    }

    private void adicionarProduto(Produto produto) {
        if (produto == null) {
            return;
        }
        for (VendaItem item : carrinho) {
            if (item.getProdutoId().equals(produto.getId())) {
                if (produto.getEstoque() <= item.getQuantidade()) {
                    Ui.erro(this, "Estoque insuficiente para " + produto.getNome()
                            + ". Disponível: " + produto.getEstoque() + ".");
                    return;
                }
                item.setQuantidade(item.getQuantidade() + 1);
                sincronizarCarrinho();
                return;
            }
        }
        if (produto.getEstoque() <= 0) {
            Ui.erro(this, "Produto sem estoque: " + produto.getNome());
            return;
        }
        carrinho.add(new VendaItem(produto.getId(), produto.getNome(), 1, produto.getPrecoVenda()));
        sincronizarCarrinho();
    }

    private void atualizarQuantidade(int linha) {
        if (linha < 0 || linha >= carrinho.size()) {
            return;
        }
        Object valor = modeloCarrinho.getValueAt(linha, 1);
        int quantidade;
        try {
            quantidade = Integer.parseInt(String.valueOf(valor).strip());
        } catch (NumberFormatException e) {
            quantidade = 0;
        }
        VendaItem item = carrinho.get(linha);
        if (quantidade <= 0) {
            carrinho.remove(linha);
        } else {
            Produto produto = aplicacao.getProdutoRepository().buscarPorId(item.getProdutoId()).orElse(null);
            if (produto != null && quantidade > produto.getEstoque()) {
                Ui.erro(this, "Estoque insuficiente para " + produto.getNome()
                        + ". Disponível: " + produto.getEstoque() + ".");
                quantidade = produto.getEstoque();
                if (quantidade <= 0) {
                    carrinho.remove(linha);
                } else {
                    item.setQuantidade(quantidade);
                }
            } else {
                item.setQuantidade(quantidade);
            }
        }
        sincronizarCarrinho();
    }

    private void atualizarDescontoItem(int linha) {
        if (linha < 0 || linha >= carrinho.size()) {
            return;
        }
        Object valor = modeloCarrinho.getValueAt(linha, 2);
        BigDecimal descontoItem = parseMoeda(String.valueOf(valor));
        VendaItem item = carrinho.get(linha);
        if (descontoItem.compareTo(BigDecimal.ZERO) <= 0) {
            item.setDesconto(BigDecimal.ZERO);
        } else if (descontoItem.compareTo(item.getValorBruto()) >= 0) {
            Ui.erro(this, "O desconto de " + item.getProdutoNome() + " não pode alcançar o subtotal do item.");
            item.setDesconto(BigDecimal.ZERO);
        } else {
            item.setDesconto(descontoItem);
        }
        sincronizarCarrinho();
    }

    private void sincronizarCarrinho() {
        modeloCarrinho.setRowCount(0);
        BigDecimal soma = BigDecimal.ZERO;
        for (VendaItem item : carrinho) {
            modeloCarrinho.addRow(new Object[]{
                    item.getProdutoNome(),
                    item.getQuantidade(),
                    Ui.moeda(item.getDesconto()),
                    Ui.moeda(item.getPrecoUnitario()),
                    Ui.moeda(item.getSubtotal())
            });
            soma = soma.add(item.getSubtotal());
        }
        subtotal = soma;
        atualizarTotais();
    }

    private void atualizarTotais() {
        BigDecimal descontoValor = parseMoeda(desconto.getText());
        if (descontoValor.compareTo(subtotal) > 0) {
            descontoValor = subtotal;
        }
        total.setText(Ui.moeda(subtotal.subtract(descontoValor)));
        calcularTroco();
    }

    private void calcularTroco() {
        BigDecimal valorTotal = parseMoeda(total.getText());
        BigDecimal pago = parseMoeda(valorPago.getText());
        if (pagamento.getSelectedItem() == FormaPagamento.DINHEIRO && pago.compareTo(valorTotal) >= 0) {
            troco.setText(Ui.moeda(pago.subtract(valorTotal)));
            troco.setForeground(Ui.COR_OK);
        } else {
            troco.setText(Ui.moeda(BigDecimal.ZERO));
            troco.setForeground(Color.DARK_GRAY);
        }
    }

    private void ajustarValorPago() {
        if (pagamento.getSelectedItem() != FormaPagamento.DINHEIRO) {
            valorPago.setText("");
            valorPago.setEnabled(false);
        } else {
            valorPago.setEnabled(pagamentosDivididos.isEmpty());
        }
        calcularTroco();
    }

    private BigDecimal parseMoeda(String texto) {
        return Ui.parseMoeda(texto);
    }

    private void abrirPagamentoDividido() {
        if (carrinho.isEmpty()) {
            Ui.erro(this, "O carrinho está vazio.");
            return;
        }
        BigDecimal totalVenda = parseMoeda(total.getText());
        if (totalVenda.compareTo(BigDecimal.ZERO) <= 0) {
            Ui.erro(this, "O total da venda deve ser maior que zero.");
            return;
        }
        DialogoPagamentoDividido dialogo = new DialogoPagamentoDividido(
                SwingUtilities.getWindowAncestor(this), totalVenda, (FormaPagamento) pagamento.getSelectedItem());
        dialogo.setVisible(true);
        if (!dialogo.isConfirmado()) {
            return;
        }
        pagamentosDivididos.clear();
        pagamentosDivididos.addAll(dialogo.getParcelas());
        statusPagamento.setText("Dividido em " + pagamentosDivididos.size() + " parcela(s)");
        pagamento.setEnabled(false);
        valorPago.setEnabled(false);
    }

    private Venda montarVenda() {
        Venda venda = new Venda();
        Cliente selecionado = (Cliente) cliente.getSelectedItem();
        if (selecionado != null) {
            venda.setClienteId(selecionado.getId());
        }
        venda.setUsuarioId(usuario.getId());
        venda.setFormaPagamento((FormaPagamento) pagamento.getSelectedItem());
        venda.setDesconto(parseMoeda(desconto.getText()));
        venda.setValorPago(parseMoeda(valorPago.getText()));
        carrinho.forEach(venda::adicionarItem);
        pagamentosDivididos.forEach(venda::adicionarPagamento);
        return venda;
    }

    private void pausarVenda() {
        if (carrinho.isEmpty()) {
            Ui.erro(this, "O carrinho está vazio.");
            return;
        }
        String observacao = javax.swing.JOptionPane.showInputDialog(this,
                "Identificação da venda pausada (opcional):", "");
        if (observacao == null) {
            return;
        }
        try {
            aplicacao.getPausas().pausar(montarVenda(), observacao, usuario);
            limpar();
            Ui.info(this, "Venda pausada. Use Retomar venda para continuar.");
        } catch (ValidacaoException e) {
            Ui.erro(this, e.getMessage());
        }
    }

    private void retomarVenda() {
        List<VendaPausada> pausadas = aplicacao.getPausas().listar();
        if (pausadas.isEmpty()) {
            Ui.info(this, "Nenhuma venda pausada no momento.");
            return;
        }
        if (!carrinho.isEmpty() && !Ui.confirma(this,
                "O carrinho atual será substituído pela venda recuperada. Continuar?")) {
            return;
        }
        String[] opcoes = pausadas.stream().map(this::descreverPausada).toArray(String[]::new);
        Object escolha = javax.swing.JOptionPane.showInputDialog(this, "Selecione a venda pausada:",
                "Retomar venda", javax.swing.JOptionPane.QUESTION_MESSAGE, null, opcoes, opcoes[0]);
        if (escolha == null) {
            return;
        }
        long id = Long.parseLong(((String) escolha).substring(1).split(" ")[0]);
        int acao = javax.swing.JOptionPane.showOptionDialog(this,
                "O que deseja fazer com a venda pausada " + id + "?", "Venda pausada",
                javax.swing.JOptionPane.YES_NO_CANCEL_OPTION, javax.swing.JOptionPane.QUESTION_MESSAGE, null,
                new Object[]{"Recuperar", "Descartar", "Cancelar"}, "Recuperar");
        if (acao == 1) {
            aplicacao.getPausas().descartar(id);
            Ui.info(this, "Venda pausada descartada.");
            return;
        }
        if (acao != 0) {
            return;
        }
        preencherComPausada(aplicacao.getPausas().buscarPorId(id));
        aplicacao.getPausas().descartar(id);
        Ui.info(this, "Venda recuperada no carrinho.");
    }

    private String descreverPausada(VendaPausada pausada) {
        StringBuilder descricao = new StringBuilder("#").append(pausada.getId())
                .append(" - ").append(Ui.dataHora(pausada.getDataHora()))
                .append(" - ").append(pausada.getQuantidadeItens()).append(" item(ns)")
                .append(" - ").append(Ui.moeda(pausada.getTotal()));
        if (pausada.getObservacao() != null && !pausada.getObservacao().isBlank()) {
            descricao.append(" (").append(pausada.getObservacao()).append(')');
        }
        return descricao.toString();
    }

    private void preencherComPausada(VendaPausada pausada) {
        limpar();
        carrinho.addAll(pausada.getItens());
        if (pausada.getDesconto().compareTo(BigDecimal.ZERO) > 0) {
            desconto.setText(pausada.getDesconto().toPlainString().replace('.', ','));
        }
        if (pausada.getClienteId() != null) {
            for (int i = 0; i < cliente.getItemCount(); i++) {
                Cliente item = cliente.getItemAt(i);
                if (item != null && item.getId().equals(pausada.getClienteId())) {
                    cliente.setSelectedIndex(i);
                    break;
                }
            }
        }
        pagamentosDivididos.addAll(pausada.getPagamentos());
        if (!pagamentosDivididos.isEmpty()) {
            statusPagamento.setText("Dividido em " + pagamentosDivididos.size() + " parcela(s)");
            pagamento.setEnabled(false);
            valorPago.setEnabled(false);
        }
        sincronizarCarrinho();
    }

    private void finalizar() {
        if (carrinho.isEmpty()) {
            Ui.erro(this, "O carrinho está vazio.");
            return;
        }
        try {
            Venda registrada = aplicacao.getVendas().registrar(montarVenda());
            mostrarCupom(registrada);
            limpar();
            recarregarProdutos();
        } catch (ValidacaoException e) {
            Ui.erro(this, e.getMessage());
        } catch (RuntimeException e) {
            Ui.erro(this, "Falha ao finalizar a venda: " + e.getMessage(), e);
        }
    }

    private void mostrarCupom(Venda venda) {
        Ui.info(this, aplicacao.getCupom().formatar(venda));
    }

    private void reimprimirCupom() {
        var ultima = aplicacao.getVendas().ultimaDoOperador(usuario.getId());
        if (ultima.isEmpty()) {
            Ui.info(this, "Nenhuma venda registrada por você ainda.");
            return;
        }
        Ui.info(this, aplicacao.getCupom().formatar(ultima.get()));
    }

    private void limpar() {
        carrinho.clear();
        pagamentosDivididos.clear();
        statusPagamento.setText(" ");
        pagamento.setEnabled(true);
        sincronizarCarrinho();
        valorPago.setText("");
        desconto.setText("");
        busca.setText("");
        ajustarValorPago();
    }

    private void recarregarClientes() {
        cliente.removeAllItems();
        cliente.addItem(null);
        for (Cliente c : aplicacao.getClientes().listarAtivos()) {
            cliente.addItem(c);
        }
        cliente.setSelectedIndex(cliente.getItemCount() > 0 ? 0 : -1);
    }

    private void recarregarProdutos() {
        produtos = aplicacao.getProdutos().listar(busca.getText());
        modeloProdutos.setRowCount(0);
        for (Produto produto : produtos) {
            modeloProdutos.addRow(new Object[]{
                    produto.getCodigoBarras(),
                    produto.getNome(),
                    produto.getEstoque(),
                    Ui.moeda(produto.getPrecoVenda())
            });
        }
        if (!produtos.isEmpty()) {
            statusEstoque.setForeground(new Color(110, 118, 126));
            statusEstoque.setText(produtos.size() + " produto(s)");
        } else {
            statusEstoque.setText(" ");
        }
    }
}
