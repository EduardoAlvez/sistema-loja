package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.PromocaoProduto;
import com.portfolio.sistemaloja.service.ValidacaoException;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

public class DialogoPromocao extends JDialog {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final transient Aplicacao aplicacao;
    private final transient PromocaoProduto promocao;
    private final transient Runnable aoSalvar;

    private final JComboBox<Produto> produto = new JComboBox<>();
    private final JTextField nome = new JTextField(28);
    private final JTextField quantidadeMinima = new JTextField(6);
    private final JTextField precoPromocional = new JTextField(10);
    private final JTextField dataInicio = new JTextField(10);
    private final JTextField dataFim = new JTextField(10);
    private final JTextField horarioInicio = new JTextField(6);
    private final JTextField horarioFim = new JTextField(6);
    private final JCheckBox ativa = new JCheckBox("Promoção ativa");
    private final Map<DayOfWeek, JCheckBox> dias = new LinkedHashMap<>();

    public DialogoPromocao(Window pai, Aplicacao aplicacao, PromocaoProduto promocao, Runnable aoSalvar) {
        super(pai, promocao.getId() == null ? "Nova promoção" : "Editar promoção", ModalityType.APPLICATION_MODAL);
        this.aplicacao = aplicacao;
        this.promocao = promocao;
        this.aoSalvar = aoSalvar;
        setLayout(new BorderLayout(10, 10));
        add(montarFormulario(), BorderLayout.CENTER);
        add(montarRodape(), BorderLayout.SOUTH);
        preencher();
        pack();
        setLocationRelativeTo(pai);
    }

    private JPanel montarFormulario() {
        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));
        formulario.setBackground(Ui.COR_FUNDO);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int linha = 0;
        linha = campo(formulario, gbc, linha, "Produto *", produto);
        linha = campo(formulario, gbc, linha, "Nome da promoção *", nome);
        linha = campo(formulario, gbc, linha, "Quantidade mínima *", quantidadeMinima);
        linha = campo(formulario, gbc, linha, "Preço promocional (R$) *", precoPromocional);

        gbc.gridx = 0;
        gbc.gridy = linha++;
        formulario.add(Ui.rotulo("Vigência"), gbc);
        gbc.gridx = 1;
        JPanel periodo = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        periodo.setOpaque(false);
        dataInicio.setToolTipText("Início em dd/MM/aaaa; vazio = sem início");
        dataFim.setToolTipText("Fim em dd/MM/aaaa; vazio = sem fim");
        periodo.add(dataInicio);
        periodo.add(Ui.rotulo("a"));
        periodo.add(dataFim);
        formulario.add(periodo, gbc);

        gbc.gridx = 0;
        gbc.gridy = linha++;
        formulario.add(Ui.rotulo("Dias da semana"), gbc);
        gbc.gridx = 1;
        JPanel painelDias = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        painelDias.setOpaque(false);
        String[] rotulos = {"Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom"};
        for (int i = 0; i < rotulos.length; i++) {
            JCheckBox dia = new JCheckBox(rotulos[i]);
            dias.put(DayOfWeek.of(i + 1), dia);
            painelDias.add(dia);
        }
        formulario.add(painelDias, gbc);

        gbc.gridx = 0;
        gbc.gridy = linha++;
        formulario.add(Ui.rotulo("Horário"), gbc);
        gbc.gridx = 1;
        JPanel painelHorario = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        painelHorario.setOpaque(false);
        horarioInicio.setToolTipText("Início em HH:mm; vazio = dia inteiro");
        horarioFim.setToolTipText("Fim em HH:mm; vazio = dia inteiro");
        painelHorario.add(horarioInicio);
        painelHorario.add(Ui.rotulo("a"));
        painelHorario.add(horarioFim);
        formulario.add(painelHorario, gbc);

        gbc.gridx = 0;
        gbc.gridy = linha++;
        gbc.gridwidth = 2;
        JLabel explicacao = new JLabel("A partir da quantidade mínima, cada unidade é vendida pelo preço promocional.");
        explicacao.setFont(explicacao.getFont().deriveFont(Font.PLAIN, 12f));
        explicacao.setForeground(Ui.COR_ESCURA);
        formulario.add(explicacao, gbc);

        gbc.gridx = 0;
        gbc.gridy = linha;
        gbc.gridwidth = 2;
        ativa.setOpaque(false);
        formulario.add(ativa, gbc);

        return formulario;
    }

    private JPanel montarRodape() {
        JButton salvar = new JButton("Salvar");
        salvar.setBackground(Ui.COR_PRINCIPAL);
        salvar.setForeground(java.awt.Color.WHITE);
        salvar.addActionListener(e -> salvar());
        JButton cancelar = new JButton("Cancelar");
        cancelar.addActionListener(e -> dispose());

        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        rodape.setBackground(Ui.COR_FUNDO);
        rodape.add(cancelar);
        rodape.add(salvar);
        getRootPane().setDefaultButton(salvar);
        return rodape;
    }

    private int campo(JPanel painel, GridBagConstraints gbc, int linha, String rotulo, JComponent campo) {
        gbc.gridx = 0;
        gbc.gridy = linha;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        painel.add(Ui.rotulo(rotulo), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        painel.add(campo, gbc);
        return linha + 1;
    }

    private void preencher() {
        for (Produto item : aplicacao.getProdutos().listarAtivos()) {
            produto.addItem(item);
        }
        produto.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice,
                                                           boolean selecionado, boolean foco) {
                Component componente = super.getListCellRendererComponent(
                        lista, valor, indice, selecionado, foco);
                if (valor instanceof Produto item && !item.isAtivo()) {
                    setText(item.getNome() + " (inativo)");
                }
                return componente;
            }
        });
        produto.setToolTipText("Produto que recebe a promoção por quantidade");
        nome.setToolTipText("Nome exibido nos relatórios e no PDV");
        quantidadeMinima.setToolTipText("Quantidade a partir da qual o preço promocional entra em vigor");
        precoPromocional.setToolTipText("Preço unitário cobrado quando a quantidade mínima é atingida");

        nome.setText(promocao.getNome() == null ? "" : promocao.getNome());
        quantidadeMinima.setText(promocao.getQuantidadeMinima() == null
                ? "" : String.valueOf(promocao.getQuantidadeMinima()));
        precoPromocional.setText(promocao.getPrecoPromocional() == null
                ? "" : promocao.getPrecoPromocional().toPlainString());
        dataInicio.setText(promocao.getDataInicio() == null ? "" : DATA.format(promocao.getDataInicio()));
        dataFim.setText(promocao.getDataFim() == null ? "" : DATA.format(promocao.getDataFim()));
        horarioInicio.setText(promocao.getHorarioInicio() == null
                ? "" : HORA.format(promocao.getHorarioInicio()));
        horarioFim.setText(promocao.getHorarioFim() == null ? "" : HORA.format(promocao.getHorarioFim()));
        ativa.setSelected(promocao.getId() == null ? true : promocao.isAtiva());
        dias.forEach((dia, caixa) -> caixa.setSelected(promocao.getDiasSemana().contains(dia)));

        selecionarProduto(promocao.getProdutoId());
    }

    private void selecionarProduto(Long produtoId) {
        if (produtoId == null) {
            return;
        }
        for (int i = 0; i < produto.getItemCount(); i++) {
            Produto item = produto.getItemAt(i);
            if (item.getId().equals(produtoId)) {
                produto.setSelectedIndex(i);
                return;
            }
        }
        Produto original = aplicacao.getProdutoRepository().buscarPorId(produtoId).orElse(null);
        if (original == null) {
            return;
        }
        produto.addItem(original);
        produto.setSelectedIndex(produto.getItemCount() - 1);
        produto.setToolTipText("O produto original da promoção está inativo; escolha outro produto "
                + "apenas se quiser mover a promoção.");
    }

    private void salvar() {
        try {
            Produto selecionado = (Produto) produto.getSelectedItem();
            if (selecionado == null) {
                throw new ValidacaoException("Selecione o produto da promoção.");
            }
            promocao.setProdutoId(selecionado.getId());
            promocao.setNome(nome.getText().strip());
            promocao.setQuantidadeMinima(inteiro(quantidadeMinima.getText(), "Quantidade mínima"));
            promocao.setPrecoPromocional(decimal(precoPromocional.getText(), "Preço promocional"));
            promocao.setDataInicio(data(dataInicio.getText()));
            promocao.setDataFim(data(dataFim.getText()));
            promocao.setHorarioInicio(hora(horarioInicio.getText()));
            promocao.setHorarioFim(hora(horarioFim.getText()));
            promocao.setDiasSemana(diasSelecionados());
            promocao.setAtiva(ativa.isSelected());

            aplicacao.getPromocoes().salvar(promocao);
            aoSalvar.run();
            dispose();
        } catch (NumberFormatException e) {
            Ui.erro(this, "Quantidade e preço devem ser números válidos (ex.: 3 e 7,90).");
        } catch (DateTimeParseException e) {
            Ui.erro(this, "Datas e horários devem estar no formato 20/10/2026 e 18:00.");
        } catch (ValidacaoException e) {
            Ui.erro(this, e.getMessage());
        } catch (RuntimeException e) {
            Ui.erro(this, "Falha ao salvar a promoção: " + e.getMessage(), e);
        }
    }

    private Integer inteiro(String texto, String rotulo) {
        String limpo = texto.strip();
        if (limpo.isEmpty()) {
            throw new ValidacaoException(rotulo + " é obrigatório.");
        }
        return Integer.valueOf(limpo);
    }

    private BigDecimal decimal(String texto, String rotulo) {
        String limpo = texto.strip().replace(',', '.');
        if (limpo.isEmpty()) {
            throw new ValidacaoException(rotulo + " é obrigatório.");
        }
        return new BigDecimal(limpo);
    }

    private LocalDate data(String texto) {
        if (texto.isBlank()) {
            return null;
        }
        return LocalDate.parse(texto.strip(), DATA);
    }

    private LocalTime hora(String texto) {
        if (texto.isBlank()) {
            return null;
        }
        return LocalTime.parse(texto.strip(), HORA);
    }

    private EnumSet<DayOfWeek> diasSelecionados() {
        EnumSet<DayOfWeek> selecionados = EnumSet.noneOf(DayOfWeek.class);
        dias.forEach((dia, caixa) -> {
            if (caixa.isSelected()) {
                selecionados.add(dia);
            }
        });
        return selecionados;
    }
}
