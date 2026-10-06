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
import com.portfolio.sistemaloja.model.Cliente;
import com.portfolio.sistemaloja.model.FormaPagamento;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.model.VendaItem;
import com.portfolio.sistemaloja.service.ValidacaoException;

public class PainelPDV extends JPanel {

    private static final long serialVersionUID = 1L;

    private final transient Aplicacao aplicacao;
    private final transient Usuario usuario;

    private final JTextField busca = new JTextField(24);
    private final JComboBox<Cliente> cliente = new JComboBox<>();
    private final JComboBox<FormaPagamento> pagamento = new JComboBox<>(FormaPagamento.values());
    private final JTextField valorPago = new JTextField(12);
    private final JLabel total = new JLabel("R$ 0,00");
    private final JLabel troco = new JLabel("R$ 0,00");
    private final JLabel statusEstoque = new JLabel(" ");

    private final javax.swing.table.DefaultTableModel modeloProdutos =
            Ui.modeloTabela("Código", "Produto", "Estoque", "Preço");
    private final JTable tabelaProdutos = Ui.tabela(modeloProdutos);
    private List<Produto> produtos = List.of();

    private final List<VendaItem> carrinho = new ArrayList<>();
    private final javax.swing.table.DefaultTableModel modeloCarrinho =
            new javax.swing.table.DefaultTableModel(
                    new Object[]{"Produto", "Qtd", "Preço unit.", "Subtotal"}, 0) {
                @Override
                public boolean isCellEditable(int linha, int coluna) {
                    return coluna == 1;
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
        JButton finalizar = new JButton("Finalizar venda");
        finalizar.setBackground(Ui.COR_OK);
        finalizar.setForeground(Color.WHITE);
        finalizar.setFont(finalizar.getFont().deriveFont(Font.BOLD, 14f));
        finalizar.addActionListener(e -> finalizar());
        botoes.add(limpar);
        botoes.add(finalizar);

        JPanel esquerda = new JPanel(new BorderLayout(10, 0));
        esquerda.setOpaque(false);
        esquerda.add(pagamentoPainel, BorderLayout.NORTH);

        rodape.add(esquerda, BorderLayout.WEST);
        rodape.add(totais, BorderLayout.CENTER);
        rodape.add(botoes, BorderLayout.EAST);
        return rodape;
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

    private void sincronizarCarrinho() {
        modeloCarrinho.setRowCount(0);
        BigDecimal soma = BigDecimal.ZERO;
        for (VendaItem item : carrinho) {
            modeloCarrinho.addRow(new Object[]{
                    item.getProdutoNome(),
                    item.getQuantidade(),
                    Ui.moeda(item.getPrecoUnitario()),
                    Ui.moeda(item.getSubtotal())
            });
            soma = soma.add(item.getSubtotal());
        }
        total.setText(Ui.moeda(soma));
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
            valorPago.setEnabled(true);
        }
        calcularTroco();
    }

    private BigDecimal parseMoeda(String texto) {
        try {
            String limpo = texto.replaceAll("[^0-9,.-]", "").replace(',', '.');
            return new BigDecimal(limpo.isBlank() ? "0" : limpo);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private void finalizar() {
        if (carrinho.isEmpty()) {
            Ui.erro(this, "O carrinho está vazio.");
            return;
        }
        try {
            Venda venda = new Venda();
            Cliente selecionado = (Cliente) cliente.getSelectedItem();
            if (selecionado != null) {
                venda.setClienteId(selecionado.getId());
            }
            venda.setUsuarioId(usuario.getId());
            venda.setFormaPagamento((FormaPagamento) pagamento.getSelectedItem());
            venda.setValorPago(parseMoeda(valorPago.getText()));
            carrinho.forEach(venda::adicionarItem);

            Venda registrada = aplicacao.getVendas().registrar(venda);
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
        StringBuilder texto = new StringBuilder();
        texto.append("       CUPOM NÃO FISCAL\n");
        texto.append("       ").append(venda.getNumero()).append("\n\n");
        texto.append("Cliente: ")
                .append(venda.getClienteNome() == null ? "Consumidor" : venda.getClienteNome()).append('\n');
        texto.append("Operador: ").append(usuario.getNome()).append('\n');
        texto.append("--------------------------------\n");
        for (VendaItem item : venda.getItens()) {
            texto.append(String.format("%s%n  %d x %s  =  %s%n",
                    item.getProdutoNome(), item.getQuantidade(),
                    Ui.moeda(item.getPrecoUnitario()), Ui.moeda(item.getSubtotal())));
        }
        texto.append("--------------------------------\n");
        texto.append("TOTAL: ").append(Ui.moeda(venda.getTotal())).append('\n');
        texto.append("Pagamento: ").append(venda.getFormaPagamento().getDescricao()).append('\n');
        if (venda.getFormaPagamento() == FormaPagamento.DINHEIRO) {
            texto.append("Pago: ").append(Ui.moeda(venda.getValorPago()))
                    .append("   Troco: ").append(Ui.moeda(venda.getTroco())).append('\n');
        }
        Ui.info(this, texto.toString());
    }

    private void limpar() {
        carrinho.clear();
        sincronizarCarrinho();
        valorPago.setText("");
        busca.setText("");
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
