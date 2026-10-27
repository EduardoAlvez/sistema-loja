package com.portfolio.sistemaloja.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.model.Autorizacao;
import com.portfolio.sistemaloja.model.ConferenciaCaixa;
import com.portfolio.sistemaloja.model.Caixa;
import com.portfolio.sistemaloja.model.Cliente;
import com.portfolio.sistemaloja.model.Devolucao;
import com.portfolio.sistemaloja.model.DevolucaoItem;
import com.portfolio.sistemaloja.model.FormaPagamento;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.StatusVenda;
import com.portfolio.sistemaloja.model.TipoLancamentoCaixa;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.model.VendaItem;
import com.portfolio.sistemaloja.model.VendaPagamento;
import com.portfolio.sistemaloja.model.VendaPausada;
import com.portfolio.sistemaloja.service.CaixaService;
import com.portfolio.sistemaloja.service.AutorizacaoNecessariaException;
import com.portfolio.sistemaloja.service.ValidacaoException;

public class PainelPDV extends JPanel {

    private static final long serialVersionUID = 1L;

    private final transient Aplicacao aplicacao;
    private final transient Usuario usuario;

    private final JComboBox<Cliente> cliente = new JComboBox<>();
    private JTextField buscaCpfCliente = new JTextField(12);
    private final JCheckBox vendaAPrazo = new JCheckBox("Venda a prazo (fiado)");
    private final JTextField vencimentoFiado = new JTextField(10);
    private final JComboBox<FormaPagamento> pagamento = new JComboBox<>(FormaPagamento.values());
    private final JTextField valorPago = new JTextField(12);
    private final JTextField desconto = new JTextField(8);
    private final JTextField pontosResgate = new JTextField(5);
    private final JLabel statusPagamento = new JLabel(" ");
    private final JLabel saldoPontos = new JLabel(" ");
    private final JLabel total = new JLabel("R$ 0,00");
    private final JLabel troco = new JLabel("R$ 0,00");
    private final JLabel statusEstoque = new JLabel(" ");
    private final JLabel statusCaixa = new JLabel(" ");
    private final JButton botaoCaixa = new JButton("Abrir caixa");
    private final JButton botaoSangria = new JButton("Sangria");
    private final JButton botaoSuprimento = new JButton("Suprimento");
    private final JButton botaoFecharCaixa = new JButton("Fechar caixa");
    private final JButton botaoBuscarProduto = new JButton("Buscar produto [F3]");
    {
        botaoBuscarProduto.setBackground(Ui.COR_PRINCIPAL);
        botaoBuscarProduto.setForeground(Color.WHITE);
        botaoBuscarProduto.addActionListener(e -> abrirBuscaProduto());
    }
    private final Map<Integer, String> atalhos = new LinkedHashMap<>();

    private final List<VendaItem> carrinho = new ArrayList<>();
    private final Set<Long> produtosComPromocao = new HashSet<>();
    private BigDecimal valorPontosMostrado = BigDecimal.ZERO;
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
        instalarAtalhos();
        recarregarClientes();
        atualizarStatusCaixa();
    }

    int itensNoCarrinho() {
        return carrinho.size();
    }

    Object itemDoCarrinho(int linha, int coluna) {
        return modeloCarrinho.getValueAt(linha, coluna);
    }

    void definirQuantidadeNoCarrinho(int linha, int quantidade) {
        modeloCarrinho.setValueAt(quantidade, linha, 1);
    }

    String textoSaldoPontos() {
        return saldoPontos.getText();
    }

    String totalAtual() {
        return total.getText();
    }

    boolean resgateDePontosHabilitado() {
        return pontosResgate.isEnabled();
    }

    void selecionarCliente(Cliente alvo) {
        for (int i = 0; i < cliente.getItemCount(); i++) {
            Cliente item = cliente.getItemAt(i);
            if (item != null && item.getId().equals(alvo.getId())) {
                cliente.setSelectedIndex(i);
                return;
            }
        }
    }

    void informarPontos(int pontos) {
        pontosResgate.setText(pontos > 0 ? String.valueOf(pontos) : "");
        atualizarTotais();
    }

    private JPanel montarTopo() {
        JPanel topo = new JPanel(new BorderLayout(10, 8));
        topo.setOpaque(false);

        JPanel titulo = new JPanel(new BorderLayout());
        titulo.setOpaque(false);
        titulo.add(Ui.titulo("PDV - Ponto de Venda"), BorderLayout.WEST);

        JPanel linhaCliente = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        linhaCliente.setOpaque(false);
        linhaCliente.add(Ui.rotulo("Cliente:"));
        cliente.setPreferredSize(new Dimension(220, cliente.getPreferredSize().height));
        linhaCliente.add(cliente);

        JPanel linhaVendaAPrazo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        linhaVendaAPrazo.setOpaque(false);
        linhaVendaAPrazo.add(vendaAPrazo);
        linhaVendaAPrazo.add(Ui.rotulo("Vencimento:"));
        linhaVendaAPrazo.add(vencimentoFiado);
        vendaAPrazo.addActionListener(e -> vencimentoFiado.setEnabled(vendaAPrazo.isSelected()));
        vencimentoFiado.setEnabled(false);

        JPanel clienteContainer = new JPanel(new BorderLayout());
        clienteContainer.setOpaque(false);
        clienteContainer.add(linhaCliente, BorderLayout.WEST);
        clienteContainer.add(linhaVendaAPrazo, BorderLayout.EAST);
        titulo.add(clienteContainer, BorderLayout.EAST);

        JPanel linhaBuscaCpf = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        linhaBuscaCpf.setOpaque(false);
        linhaBuscaCpf.add(Ui.rotulo("CPF:"));
        buscaCpfCliente.setPreferredSize(new Dimension(170, buscaCpfCliente.getPreferredSize().height));
        linhaBuscaCpf.add(buscaCpfCliente);
        JButton buscarCliente = new JButton("Buscar");
        buscarCliente.addActionListener(e -> buscarClientePorCpf());
        linhaBuscaCpf.add(buscarCliente);
        JButton cadastrarCliente = new JButton("Cadastrar");
        cadastrarCliente.addActionListener(e -> cadastrarClienteRapido());
        linhaBuscaCpf.add(cadastrarCliente);

        JPanel linhaBuscaProduto = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        linhaBuscaProduto.setOpaque(false);
        linhaBuscaProduto.add(botaoBuscarProduto);

        JPanel central = new JPanel(new BorderLayout(0, 6));
        central.setOpaque(false);
        central.add(linhaBuscaProduto, BorderLayout.NORTH);
        central.add(linhaBuscaCpf, BorderLayout.SOUTH);

        JPanel linhaCaixa = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        linhaCaixa.setOpaque(false);
        statusCaixa.setFont(statusCaixa.getFont().deriveFont(Font.BOLD, 12f));
        linhaCaixa.add(statusCaixa);
        botaoCaixa.addActionListener(e -> acaoCaixa());
        linhaCaixa.add(botaoCaixa);
        botaoSangria.addActionListener(e -> movimentarCaixa(TipoLancamentoCaixa.SANGRIA));
        linhaCaixa.add(botaoSangria);
        botaoSuprimento.addActionListener(e -> movimentarCaixa(TipoLancamentoCaixa.SUPRIMENTO));
        linhaCaixa.add(botaoSuprimento);
        botaoFecharCaixa.addActionListener(e -> fecharCaixa());
        linhaCaixa.add(botaoFecharCaixa);
        JButton devolucao = new JButton("Devolução");
        devolucao.addActionListener(e -> abrirDevolucao());
        linhaCaixa.add(devolucao);

        topo.add(titulo, BorderLayout.NORTH);
        topo.add(central, BorderLayout.CENTER);
        topo.add(linhaCaixa, BorderLayout.SOUTH);
        return topo;
    }

    private JPanel montarCentro() {
        JPanel direita = painel("Carrinho", new JScrollPane(tabelaCarrinho));
        return direita;
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
        pagamentoPainel.add(Ui.rotulo("Pontos:"));
        pontosResgate.setToolTipText("Pontos do cliente resgatados como desconto. "
                + "Cada ponto vale " + Ui.moeda(aplicacao.getFidelidade().getConfig().valorPorPonto())
                + ". Limite do seu perfil: " + usuario.getPerfil().getPercentualMaximoDesconto() + "%");
        pontosResgate.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                atualizarTotais();
            }
        });
        pagamentoPainel.add(pontosResgate);
        statusPagamento.setForeground(Ui.COR_PRINCIPAL);

        JPanel statusLinha = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 2));
        statusLinha.setOpaque(false);
        statusLinha.add(statusPagamento);
        saldoPontos.setForeground(new Color(110, 118, 126));
        statusLinha.add(saldoPontos);
        JPanel pagamentoColuna = new JPanel(new GridLayout(2, 1, 0, 2));
        pagamentoColuna.setOpaque(false);
        pagamentoColuna.add(pagamentoPainel);
        pagamentoColuna.add(statusLinha);

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

        JPanel botoes = new JPanel(new GridLayout(2, 4, 8, 6));
        botoes.setOpaque(false);
        JButton limpar = new JButton("Limpar carrinho [F8]");
        limpar.addActionListener(e -> limpar());
        JButton dividir = new JButton("Pagamento dividido [F4]");
        dividir.addActionListener(e -> abrirPagamentoDividido());
        JButton pausar = new JButton("Pausar venda [F5]");
        pausar.addActionListener(e -> pausarVenda());
        JButton retomar = new JButton("Retomar venda [F6]");
        retomar.addActionListener(e -> retomarVenda());
        JButton reimprimir = new JButton("Reimprimir cupom [F7]");
        reimprimir.addActionListener(e -> reimprimirCupom());
        JButton ajuda = new JButton("Atalhos");
        ajuda.addActionListener(e -> mostrarAtalhos());
        JButton cupomTermico = new JButton("Cupom 80mm (PIX)");
        cupomTermico.addActionListener(e -> abrirCupomTermico());
        JButton finalizar = new JButton("Finalizar venda [F12]");
        finalizar.setBackground(Ui.COR_OK);
        finalizar.setForeground(Color.WHITE);
        finalizar.setFont(finalizar.getFont().deriveFont(Font.BOLD, 14f));
        finalizar.addActionListener(e -> finalizar());
        botoes.add(limpar);
        botoes.add(dividir);
        botoes.add(pausar);
        botoes.add(retomar);
        botoes.add(reimprimir);
        botoes.add(cupomTermico);
        botoes.add(ajuda);
        botoes.add(finalizar);

        JPanel linhaSuperior = new JPanel(new BorderLayout(16, 0));
        linhaSuperior.setOpaque(false);
        linhaSuperior.add(pagamentoColuna, BorderLayout.WEST);
        linhaSuperior.add(totais, BorderLayout.EAST);

        rodape.add(linhaSuperior, BorderLayout.NORTH);
        rodape.add(botoes, BorderLayout.SOUTH);
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
        } catch (AutorizacaoNecessariaException e) {
            Optional<Autorizacao> autorizacao = DialogoAutorizacao.pedir(SwingUtilities.getWindowAncestor(this), aplicacao, e.getMotivo());
            if (autorizacao.isEmpty()) {
                return;
            }
            try {
                aplicacao.getCaixa().lancar(tipo, parseMoeda(valor), observacao, usuario, autorizacao.get());
                Caixa atualizado = aplicacao.getCaixa().caixaAberto().orElse(aberto);
                Ui.info(this, tipo.getDescricao() + " registrado(a). Saldo em espécie: "
                        + Ui.moeda(aplicacao.getCaixa().saldoEspecie(atualizado)));
            } catch (ValidacaoException ex) {
                Ui.erro(this, ex.getMessage());
            } catch (RuntimeException ex) {
                Ui.erro(this, "Falha ao registrar " + tipo.getDescricao().toLowerCase() + ": " + ex.getMessage(), ex);
            }
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

        Optional<ConferenciaCaixa> conferencia = DialogoConferenciaCaixa.pedir(
                SwingUtilities.getWindowAncestor(this), aplicacao, aberto);
        if (conferencia.isEmpty()) {
            return;
        }
        ConferenciaCaixa valores = conferencia.get();
        if (!Ui.confirma(this, "Confirma o fechamento do caixa?")) {
            return;
        }
        try {
            CaixaService.Resumo fechado = aplicacao.getCaixa().fechar(valores.valor(), valores.observacao(), usuario);
            StringBuilder relatorio = new StringBuilder(texto);
            relatorio.append("Saldo esperado em especie: ").append(Ui.moeda(fechado.saldoEsperado())).append('\n');
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

        cliente.addActionListener(e -> atualizarSaldoPontos());
    }

    public Map<Integer, String> atalhos() {
        return java.util.Collections.unmodifiableMap(atalhos);
    }

    private void instalarAtalhos() {
        ligar(KeyEvent.VK_F3, "F3 - Buscar produto", this::abrirBuscaProduto);
        ligar(KeyEvent.VK_F4, "F4 - Pagamento dividido", this::abrirPagamentoDividido);
        ligar(KeyEvent.VK_F5, "F5 - Pausar venda", this::pausarVenda);
        ligar(KeyEvent.VK_F6, "F6 - Retomar venda", this::retomarVenda);
        ligar(KeyEvent.VK_F7, "F7 - Reimprimir cupom", this::reimprimirCupom);
        ligar(KeyEvent.VK_F8, "F8 - Limpar carrinho", this::limpar);
        ligar(KeyEvent.VK_F9, "F9 - Sangria", () -> movimentarCaixa(TipoLancamentoCaixa.SANGRIA));
        ligar(KeyEvent.VK_F10, "F10 - Suprimento", () -> movimentarCaixa(TipoLancamentoCaixa.SUPRIMENTO));
        ligar(KeyEvent.VK_F11, "F11 - Abrir caixa", this::acaoCaixa);
        ligar(KeyEvent.VK_F12, "F12 - Finalizar venda", this::finalizar);
    }

    private void ligar(int tecla, String descricao, Runnable acao) {
        String nome = "atalho-" + tecla;
        atalhos.put(tecla, descricao);
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(tecla, 0), nome);
        getActionMap().put(nome, new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                acao.run();
            }
        });
    }

    private void mostrarAtalhos() {
        StringBuilder texto = new StringBuilder("Atalhos do PDV:\n");
        for (String descricao : atalhos.values()) {
            texto.append(descricao).append('\n');
        }
        texto.append("F3 - Buscar produto");
        Ui.info(this, texto.toString());
    }

    private void abrirBuscaProduto() {
        Optional<Produto> produtoSelecionado = DialogoBuscaProduto.pedir(SwingUtilities.getWindowAncestor(this), aplicacao);
        if (produtoSelecionado.isPresent()) {
            adicionarProduto(produtoSelecionado.get());
        }
    }

    private void abrirDevolucao() {
        String numero = javax.swing.JOptionPane.showInputDialog(this,
                "Número da venda a devolver (ex.: V000001):", "");
        if (numero == null || numero.isBlank()) {
            return;
        }
        Optional<Venda> encontrada = aplicacao.getDevolucoes().buscarVendaPorNumero(numero);
        if (encontrada.isEmpty()) {
            Ui.erro(this, "Venda não encontrada: " + numero.strip());
            return;
        }
        Venda venda = encontrada.get();
        if (venda.getStatus() != StatusVenda.CONCLUIDA) {
            Ui.erro(this, "Somente vendas concluídas podem ser devolvidas.");
            return;
        }
        PainelDevolucao painel = new PainelDevolucao(venda, aplicacao.getDevolucoes()::jaDevolvido);
        int opcao = javax.swing.JOptionPane.showConfirmDialog(this, painel,
                "Devolução parcial - " + venda.getNumero(), javax.swing.JOptionPane.OK_CANCEL_OPTION,
                javax.swing.JOptionPane.PLAIN_MESSAGE);
        if (opcao != javax.swing.JOptionPane.OK_OPTION) {
            return;
        }
        try {
            Devolucao devolucao = aplicacao.getDevolucoes().devolver(venda.getId(),
                    painel.itensParaDevolucao(), painel.creditoNoCliente(), null, usuario);
            Ui.info(this, descreverDevolucao(devolucao));
        } catch (ValidacaoException e) {
            Ui.erro(this, e.getMessage());
        }
    }

    private String descreverDevolucao(Devolucao devolucao) {
        StringBuilder texto = new StringBuilder("DEVOLUÇÃO - ").append(devolucao.getVendaNumero()).append("\n\n");
        for (DevolucaoItem item : devolucao.getItens()) {
            texto.append(String.format("%s%n  %d x %s  =  %s%n", item.getProdutoNome(), item.getQuantidade(),
                    Ui.moeda(item.getValorUnitario()), Ui.moeda(item.getSubtotal())));
        }
        texto.append("\nTotal devolvido: ").append(Ui.moeda(devolucao.getValor())).append('\n');
        if (devolucao.isCreditoCliente()) {
            texto.append("Crédito no cliente: ").append(devolucao.getClienteNome()).append('\n');
        }
        if (devolucao.getPontosEstornados() > 0) {
            texto.append("Pontos estornados: ").append(devolucao.getPontosEstornados()).append(" ponto(s)\n");
        }
        return texto.toString();
    }

    private void adicionarPeloCampo() {
        Optional<Produto> produtoSelecionado = DialogoBuscaProduto.pedir(SwingUtilities.getWindowAncestor(this), aplicacao);
        if (produtoSelecionado.isPresent()) {
            adicionarProduto(produtoSelecionado.get());
        }
    }

    void adicionarProduto(Produto produto) {
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
                int quantidade = item.getQuantidade() + 1;
                BigDecimal preco = precoUnitario(produto, quantidade);
                item.setPrecoUnitario(preco);
                item.setQuantidade(quantidade);
                marcarPromocao(produto, preco);
                sincronizarCarrinho();
                return;
            }
        }
        if (produto.getEstoque() <= 0) {
            Ui.erro(this, "Produto sem estoque: " + produto.getNome());
            return;
        }
        BigDecimal preco = precoUnitario(produto, 1);
        marcarPromocao(produto, preco);
        carrinho.add(new VendaItem(produto.getId(), produto.getNome(), 1, preco));
        sincronizarCarrinho();
    }

    private BigDecimal precoUnitario(Produto produto, int quantidade) {
        return aplicacao.getPromocoes().precoUnitarioVigente(produto, quantidade);
    }

    private void marcarPromocao(Produto produto, BigDecimal preco) {
        if (produto.getPrecoVenda() != null && preco.compareTo(produto.getPrecoVenda()) < 0) {
            produtosComPromocao.add(produto.getId());
            return;
        }
        produtosComPromocao.remove(produto.getId());
    }

    boolean reprecificarCarrinho() {
        BigDecimal antes = totalDoCarrinho();
        for (VendaItem item : carrinho) {
            Produto produto = aplicacao.getProdutoRepository().buscarPorId(item.getProdutoId()).orElse(null);
            if (produto == null) {
                continue;
            }
            BigDecimal preco = precoUnitario(produto, item.getQuantidade());
            item.setPrecoUnitario(preco);
            marcarPromocao(produto, preco);
        }
        sincronizarCarrinho();
        return antes.compareTo(totalDoCarrinho()) != 0;
    }

    private BigDecimal totalDoCarrinho() {
        BigDecimal soma = BigDecimal.ZERO;
        for (VendaItem item : carrinho) {
            soma = soma.add(item.getSubtotal());
        }
        return soma;
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
            removerItemDoCarrinho(linha);
        } else {
            Produto produto = aplicacao.getProdutoRepository().buscarPorId(item.getProdutoId()).orElse(null);
            if (produto != null && quantidade > produto.getEstoque()) {
                Ui.erro(this, "Estoque insuficiente para " + produto.getNome()
                        + ". Disponível: " + produto.getEstoque() + ".");
                quantidade = produto.getEstoque();
                if (quantidade <= 0) {
                    removerItemDoCarrinho(linha);
                } else {
                    item.setQuantidade(quantidade);
                    BigDecimal preco = precoUnitario(produto, quantidade);
                    item.setPrecoUnitario(preco);
                    marcarPromocao(produto, preco);
                }
            } else {
                item.setQuantidade(quantidade);
                if (produto != null) {
                    BigDecimal preco = precoUnitario(produto, quantidade);
                    item.setPrecoUnitario(preco);
                    marcarPromocao(produto, preco);
                }
            }
        }
        sincronizarCarrinho();
    }

    private String avisoDeRecalculo(boolean pontosMudaram, boolean precoMudou) {
        StringBuilder aviso = new StringBuilder("Os valores do carrinho foram recalculados");
        if (pontosMudaram && precoMudou) {
            aviso.append(" (o valor do ponto e as promoções vigentes mudaram)");
        } else if (pontosMudaram) {
            aviso.append(" (o valor do ponto mudou)");
        } else {
            aviso.append(" (as promoções vigentes mudaram)");
        }
        return aviso.append(". Confira o total e finalize novamente.").toString();
    }

    private void removerItemDoCarrinho(int linha) {
        VendaItem item = carrinho.remove(linha);
        produtosComPromocao.remove(item.getProdutoId());
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
            String nome = item.getProdutoNome();
            if (produtosComPromocao.contains(item.getProdutoId())) {
                nome = nome + " (promo)";
            }
            modeloCarrinho.addRow(new Object[]{
                    nome,
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
        pontosResgate.setToolTipText("Pontos do cliente resgatados como desconto. Cada ponto vale "
                + Ui.moeda(aplicacao.getFidelidade().getConfig().valorPorPonto())
                + ". Limite do seu perfil: " + usuario.getPerfil().getPercentualMaximoDesconto() + "%");
        BigDecimal descontoValor = parseMoeda(desconto.getText());
        if (descontoValor.compareTo(subtotal) > 0) {
            descontoValor = subtotal;
        }
        BigDecimal valorPontos = valorDosPontosResgatados();
        valorPontosMostrado = valorPontos;
        BigDecimal totalVenda = subtotal.subtract(descontoValor).subtract(valorPontos);
        if (totalVenda.compareTo(BigDecimal.ZERO) < 0) {
            totalVenda = BigDecimal.ZERO;
        }
        total.setText(Ui.moeda(totalVenda));
        calcularTroco();
    }

    boolean valoresDivergemDaTela() {
        return valorDosPontosResgatados().compareTo(valorPontosMostrado) != 0;
    }

    private int pontosResgatados() {
        String texto = pontosResgate.getText().trim();
        if (texto.isEmpty()) {
            return 0;
        }
        try {
            return Math.max(Integer.parseInt(texto), 0);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private BigDecimal valorDosPontosResgatados() {
        return aplicacao.getFidelidade().valorDosPontos(pontosResgatados());
    }

    private void atualizarSaldoPontos() {
        Cliente selecionado = (Cliente) cliente.getSelectedItem();
        pontosResgate.setEnabled(selecionado != null);
        if (selecionado == null) {
            saldoPontos.setText(" ");
            return;
        }
        int saldo = aplicacao.getFidelidade().saldo(selecionado.getId());
        saldoPontos.setText("Saldo: " + saldo + " ponto(s)");
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
        venda.setPontosResgatados(pontosResgatados());
        venda.setValorPontos(valorDosPontosResgatados());
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
        Venda retomada = pausada.paraVenda();
        BigDecimal valorPontosPausado = retomada.getValorPontos();
        limpar();
        carrinho.addAll(retomada.getItens());
        if (retomada.getDesconto().compareTo(BigDecimal.ZERO) > 0) {
            desconto.setText(retomada.getDesconto().toPlainString().replace('.', ','));
        }
        if (retomada.getPontosResgatados() > 0) {
            pontosResgate.setText(String.valueOf(retomada.getPontosResgatados()));
        }
        if (retomada.getClienteId() != null) {
            for (int i = 0; i < cliente.getItemCount(); i++) {
                Cliente item = cliente.getItemAt(i);
                if (item != null && item.getId().equals(retomada.getClienteId())) {
                    cliente.setSelectedIndex(i);
                    break;
                }
            }
        }
        pagamentosDivididos.addAll(retomada.getPagamentos());
        if (!pagamentosDivididos.isEmpty()) {
            statusPagamento.setText("Dividido em " + pagamentosDivididos.size() + " parcela(s)");
            pagamento.setEnabled(false);
            valorPago.setEnabled(false);
        }
        reprecificarCarrinho();
        BigDecimal valorPontosAtual = valorDosPontosResgatados();
        if (valorPontosAtual.compareTo(valorPontosPausado) != 0) {
            Ui.info(this, "O valor do ponto mudou desde a pausa: o resgate agora vale "
                    + Ui.moeda(valorPontosAtual) + ". Confira o total antes de finalizar.");
        }
    }

    private void finalizar() {
        if (carrinho.isEmpty()) {
            Ui.erro(this, "O carrinho está vazio.");
            return;
        }
        boolean pontosMudaram = valoresDivergemDaTela();
        boolean precoMudou = reprecificarCarrinho();
        if (pontosMudaram || precoMudou) {
            Ui.info(this, avisoDeRecalculo(pontosMudaram, precoMudou));
            return;
        }
        Cliente selecionado = (Cliente) cliente.getSelectedItem();
        if (vendaAPrazo.isSelected() && selecionado == null) {
            Ui.erro(this, "Para venda a prazo, selecione um cliente.");
            return;
        }
        if (vendaAPrazo.isSelected() && vencimentoFiado.getText().trim().isEmpty()) {
            Ui.erro(this, "Informe a data de vencimento para a venda a prazo.");
            return;
        }
        try {
            Venda venda = montarVenda();
            if (vendaAPrazo.isSelected()) {
                LocalDate vencimento;
                try {
                    vencimento = LocalDate.parse(vencimentoFiado.getText().trim());
                } catch (java.time.format.DateTimeParseException ex) {
                    throw new ValidacaoException("Data de vencimento inválida. Use o formato AAAA-MM-DD.");
                }
                Venda registrada = aplicacao.getVendas().registrar(venda);
                aplicacao.getFiados().gerarFiado(selecionado.getId(), registrada.getId(),
                        registrada.getTotal(), vencimento,
                        "Venda a prazo - PDV", null);
                Ui.info(this, "Venda a prazo registrada. Fiado gerado para o cliente " + selecionado.getNome());
                limpar();
            } else {
                concluir(aplicacao.getVendas().registrar(venda));
            }
        } catch (AutorizacaoNecessariaException e) {
            Optional<Autorizacao> autorizacao = DialogoAutorizacao.pedir(SwingUtilities.getWindowAncestor(this), aplicacao, e.getMotivo());
            if (autorizacao.isEmpty()) {
                return;
            }
            try {
                Venda venda = montarVenda();
                if (vendaAPrazo.isSelected()) {
                    LocalDate vencimento;
                    try {
                        vencimento = LocalDate.parse(vencimentoFiado.getText().trim());
                    } catch (java.time.format.DateTimeParseException ex) {
                        throw new ValidacaoException("Data de vencimento inválida. Use o formato AAAA-MM-DD.");
                    }
                    Venda registrada = aplicacao.getVendas().registrar(venda, autorizacao.get());
                    aplicacao.getFiados().gerarFiado(selecionado.getId(), registrada.getId(),
                            registrada.getTotal(), vencimento,
                            "Venda a prazo - PDV", autorizacao.get());
                    Ui.info(this, "Venda a prazo registrada. Fiado gerado para o cliente " + selecionado.getNome());
                    limpar();
                } else {
                    concluir(aplicacao.getVendas().registrar(venda, autorizacao.get()));
                }
            } catch (ValidacaoException ex) {
                Ui.erro(this, ex.getMessage());
            } catch (RuntimeException ex) {
                Ui.erro(this, "Falha ao finalizar a venda: " + ex.getMessage(), ex);
            }
        } catch (ValidacaoException e) {
            Ui.erro(this, e.getMessage());
        } catch (RuntimeException e) {
            Ui.erro(this, "Falha ao finalizar a venda: " + e.getMessage(), e);
        }
    }

    private void concluir(Venda registrada) {
        mostrarCupom(registrada);
        limpar();
    }

    private void mostrarCupom(Venda venda) {
        Ui.info(this, aplicacao.getCupom().formatar(venda));
    }

    private void abrirCupomTermico() {
        var ultima = aplicacao.getVendas().ultimaDoOperador(usuario.getId());
        if (ultima.isEmpty()) {
            Ui.info(this, "Nenhuma venda registrada por você ainda.");
            return;
        }
        java.awt.Window janela = SwingUtilities.getWindowAncestor(this);
        Frame pai = janela instanceof Frame frame ? frame : null;
        new DialogoCupomTermico(pai, ultima.get(), aplicacao.getCupomTermico(),
                aplicacao.getImpressao()).setVisible(true);
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
        produtosComPromocao.clear();
        pagamentosDivididos.clear();
        statusPagamento.setText(" ");
        pagamento.setEnabled(true);
        sincronizarCarrinho();
        valorPago.setText("");
        desconto.setText("");
        pontosResgate.setText("");
        ajustarValorPago();
        atualizarSaldoPontos();
    }

    private void recarregarClientes() {
        cliente.removeAllItems();
        cliente.addItem(null);
        for (Cliente c : aplicacao.getClientes().listarAtivos()) {
            cliente.addItem(c);
        }
        cliente.setSelectedIndex(cliente.getItemCount() > 0 ? 0 : -1);
        atualizarSaldoPontos();
    }

    private void buscarClientePorCpf() {
        String cpf = buscaCpfCliente.getText().trim();
        if (cpf.isBlank()) {
            Ui.erro(this, "Informe o CPF para buscar.");
            return;
        }
        // Remove mascara para busca
        String cpfLimpo = cpf.replaceAll("[^0-9]", "");
        Cliente encontrado = null;
        for (int i = 0; i < cliente.getItemCount(); i++) {
            Cliente item = cliente.getItemAt(i);
            if (item != null && item.getCpf() != null) {
                String cpfItemLimpo = item.getCpf().replaceAll("[^0-9]", "");
                if (cpfItemLimpo.equals(cpfLimpo)) {
                    encontrado = item;
                    break;
                }
            }
        }
        if (encontrado != null) {
            cliente.setSelectedItem(encontrado);
            buscaCpfCliente.setText(encontrado.getCpf());
            Ui.info(this, "Cliente encontrado: " + encontrado.getNome());
        } else {
            boolean resposta = Ui.confirma(this, "CPF não encontrado. Deseja cadastrar este cliente com CPF " + cpf + "?");
            if (resposta) {
                cadastrarClienteRapido(cpf);
            }
        }
    }

    private void cadastrarClienteRapido() {
        cadastrarClienteRapido(buscaCpfCliente.getText().trim());
    }

    private void cadastrarClienteRapido(String cpf) {
        String cpfLimpo = cpf.replaceAll("[^0-9]", "");
        if (cpfLimpo.length() != 11) {
            Ui.erro(this, "CPF invalido. Informe 11 digitos.");
            return;
        }

        JTextField telefoneField = new JTextField(15);
        JTextField ruaField = new JTextField(25);
        JTextField nomeField = new JTextField(25);
        nomeField.setText(buscaNomeCliente(cpfLimpo));

        JPanel painel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        painel.add(Ui.rotulo("Nome:"), gbc);
        gbc.gridx = 1;
        painel.add(nomeField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        painel.add(Ui.rotulo("Telefone:"), gbc);
        gbc.gridx = 1;
        painel.add(telefoneField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        painel.add(Ui.rotulo("Rua:"), gbc);
        gbc.gridx = 1;
        painel.add(ruaField, gbc);

        int resultado = JOptionPane.showConfirmDialog(this, painel, "Cadastrar cliente (CPF: " + cpfLimpo + ")",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (resultado != JOptionPane.OK_OPTION) {
            return;
        }

        String nome = nomeField.getText().trim();
        String telefone = telefoneField.getText().trim();
        String rua = ruaField.getText().trim();

        if (nome.isBlank()) {
            Ui.erro(this, "Informe o nome do cliente.");
            return;
        }

        try {
            Cliente novo = new Cliente(null, nome, cpfLimpo, telefone, "", true, BigDecimal.ZERO, 0, BigDecimal.ZERO);
            novo = aplicacao.getClientes().salvar(novo);
            cliente.addItem(novo);
            cliente.setSelectedItem(novo);
            buscaCpfCliente.setText(novo.getCpf());
            Ui.info(this, "Cliente cadastrado: " + novo.getNome());
        } catch (ValidacaoException e) {
            Ui.erro(this, e.getMessage());
        }
    }

    private String buscaNomeCliente(String cpfLimpo) {
        // Tenta buscar o nome pelo CPF em clientes ja cadastrados (caso esteja desativado)
        for (int i = 0; i < cliente.getItemCount(); i++) {
            Cliente item = cliente.getItemAt(i);
            if (item != null && item.getCpf() != null && item.getCpf().replaceAll("[^0-9]", "").equals(cpfLimpo)) {
                return item.getNome();
            }
        }
        return "";
    }

}
