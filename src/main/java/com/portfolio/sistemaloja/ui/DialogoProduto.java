package com.portfolio.sistemaloja.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.model.Categoria;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.service.ValidacaoException;

public class DialogoProduto extends JDialog {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter DATA_TELA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_ENTRADA = DateTimeFormatter.ofPattern("d/M/uuuu");
    private static final DateTimeFormatter HORA_TELA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter HORA_ENTRADA = DateTimeFormatter.ofPattern("H:mm");

    private final transient Aplicacao aplicacao;
    private final Produto produto;
    private final Runnable aoSalvar;

    private final JTextField codigo = new JTextField(20);
    private final JTextField nome = new JTextField(20);
    private final JComboBox<Categoria> categoria = new JComboBox<>();
    private final JTextField novaCategoria = new JTextField(16);
    private final JTextField custo = new JTextField(10);
    private final JTextField venda = new JTextField(10);
    private final JTextField estoque = new JTextField(6);
    private final JTextField estoqueMinimo = new JTextField(6);
    private final JCheckBox ativo = new JCheckBox("Produto ativo");
    private final JTextField precoPromocional = new JTextField(8);
    private final JTextField promoInicio = new JTextField(8);
    private final JTextField promoFim = new JTextField(8);
    private final JTextField promoHoraInicio = new JTextField(5);
    private final JTextField promoHoraFim = new JTextField(5);
    private final Map<DayOfWeek, JCheckBox> diasPromocao = new LinkedHashMap<>();

    public DialogoProduto(java.awt.Window pai, Aplicacao aplicacao, Produto produto, Runnable aoSalvar) {
        super(pai, produto.getId() == null ? "Novo produto" : "Editar produto", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        this.aplicacao = aplicacao;
        this.produto = produto;
        this.aoSalvar = aoSalvar;
        montar();
        preencher();
        pack();
        setLocationRelativeTo(pai);
        setResizable(false);
    }

    private void montar() {
        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.anchor = GridBagConstraints.WEST;

        adicionar(formulario, gbc, 0, "Código de barras *", codigo);
        adicionar(formulario, gbc, 1, "Nome *", nome);

        gbc.gridx = 0;
        gbc.gridy = 2;
        formulario.add(Ui.rotulo("Categoria"), gbc);
        gbc.gridx = 1;
        JPanel painelCategoria = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        painelCategoria.add(categoria);
        painelCategoria.add(Ui.rotulo("ou nova:"));
        painelCategoria.add(novaCategoria);
        formulario.add(painelCategoria, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        formulario.add(Ui.rotulo("Preço de custo *"), gbc);
        gbc.gridx = 1;
        JPanel painelCusto = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        painelCusto.add(new JLabel("R$"));
        painelCusto.add(custo);
        formulario.add(painelCusto, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        formulario.add(Ui.rotulo("Preço de venda *"), gbc);
        gbc.gridx = 1;
        JPanel painelVenda = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        painelVenda.add(new JLabel("R$"));
        painelVenda.add(venda);
        formulario.add(painelVenda, gbc);

        adicionar(formulario, gbc, 5, "Estoque *", estoque);
        adicionar(formulario, gbc, 6, "Estoque mínimo", estoqueMinimo);

        gbc.gridx = 1;
        gbc.gridy = 7;
        formulario.add(ativo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        formulario.add(montarPromocao(), gbc);
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;

        for (Categoria c : aplicacao.getProdutos().listarCategorias()) {
            categoria.addItem(c);
        }

        JButton salvar = new JButton("Salvar");
        salvar.setBackground(Ui.COR_PRINCIPAL);
        salvar.setForeground(java.awt.Color.WHITE);
        salvar.addActionListener(e -> salvar());
        JButton cancelar = new JButton("Cancelar");
        cancelar.addActionListener(e -> dispose());

        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        rodape.add(cancelar);
        rodape.add(salvar);
        getRootPane().setDefaultButton(salvar);

        setLayout(new BorderLayout());
        add(formulario, BorderLayout.CENTER);
        add(rodape, BorderLayout.SOUTH);
    }

    private void adicionar(JPanel painel, GridBagConstraints gbc, int linha, String rotulo, JTextField campo) {
        gbc.gridx = 0;
        gbc.gridy = linha;
        painel.add(Ui.rotulo(rotulo), gbc);
        gbc.gridx = 1;
        painel.add(campo, gbc);
    }

    private JPanel montarPromocao() {
        JPanel painel = new JPanel(new GridBagLayout());
        painel.setBorder(BorderFactory.createTitledBorder("Promoção (opcional)"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 4, 3, 4);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        painel.add(Ui.rotulo("Preço promocional (R$)"), gbc);
        gbc.gridx = 1;
        precoPromocional.setToolTipText("Vazio = sem promoção");
        painel.add(precoPromocional, gbc);
        gbc.gridx = 2;
        gbc.gridwidth = 3;
        painel.add(Ui.rotulo("vazio = sem promoção"), gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = 1;
        painel.add(Ui.rotulo("Vigência"), gbc);
        gbc.gridx = 1;
        promoInicio.setToolTipText("Início em dd/MM/aaaa; vazio = sem início");
        painel.add(promoInicio, gbc);
        gbc.gridx = 2;
        painel.add(Ui.rotulo("a"), gbc);
        gbc.gridx = 3;
        promoFim.setToolTipText("Fim em dd/MM/aaaa; vazio = sem fim");
        painel.add(promoFim, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        painel.add(Ui.rotulo("Dias"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        JPanel dias = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        String[] rotulos = {"Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom"};
        for (int i = 0; i < rotulos.length; i++) {
            JCheckBox dia = new JCheckBox(rotulos[i]);
            diasPromocao.put(DayOfWeek.of(i + 1), dia);
            dias.add(dia);
        }
        painel.add(dias, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = 3;
        painel.add(Ui.rotulo("Horário"), gbc);
        gbc.gridx = 1;
        promoHoraInicio.setToolTipText("Início em HH:mm; vazio = dia inteiro");
        painel.add(promoHoraInicio, gbc);
        gbc.gridx = 2;
        painel.add(Ui.rotulo("a"), gbc);
        gbc.gridx = 3;
        promoHoraFim.setToolTipText("Fim em HH:mm; vazio = dia inteiro");
        painel.add(promoHoraFim, gbc);
        gbc.gridx = 4;
        painel.add(Ui.rotulo("nenhum dia marcado = todos"), gbc);
        return painel;
    }

    private void preencher() {
        codigo.setText(produto.getCodigoBarras());
        nome.setText(produto.getNome());
        custo.setText(String.valueOf(produto.getPrecoCusto()));
        venda.setText(String.valueOf(produto.getPrecoVenda()));
        estoque.setText(String.valueOf(produto.getEstoque()));
        estoqueMinimo.setText(String.valueOf(produto.getEstoqueMinimo()));
        ativo.setSelected(produto.isAtivo());
        precoPromocional.setText(produto.getPrecoPromocional() == null
                ? "" : produto.getPrecoPromocional().toPlainString());
        promoInicio.setText(produto.getPromoInicio() == null
                ? "" : DATA_TELA.format(produto.getPromoInicio()));
        promoFim.setText(produto.getPromoFim() == null
                ? "" : DATA_TELA.format(produto.getPromoFim()));
        promoHoraInicio.setText(produto.getPromoHoraInicio() == null
                ? "" : HORA_TELA.format(produto.getPromoHoraInicio()));
        promoHoraFim.setText(produto.getPromoHoraFim() == null
                ? "" : HORA_TELA.format(produto.getPromoHoraFim()));
        boolean todosOsDias = produto.getPromoDias().isEmpty();
        diasPromocao.forEach((dia, caixa) ->
                caixa.setSelected(todosOsDias || produto.getPromoDias().contains(dia)));
        if (produto.getCategoriaId() != null) {
            for (int i = 0; i < categoria.getItemCount(); i++) {
                if (categoria.getItemAt(i).getId().equals(produto.getCategoriaId())) {
                    categoria.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private void salvar() {
        try {
            produto.setCodigoBarras(codigo.getText().strip());
            produto.setNome(nome.getText().strip());
            produto.setPrecoCusto(new BigDecimal(custo.getText().strip().replace(',', '.')));
            produto.setPrecoVenda(new BigDecimal(venda.getText().strip().replace(',', '.')));
            produto.setEstoque(Integer.parseInt(estoque.getText().strip()));
            produto.setEstoqueMinimo(estoqueMinimo.getText().isBlank()
                    ? 0 : Integer.parseInt(estoqueMinimo.getText().strip()));
            produto.setAtivo(ativo.isSelected());
            produto.setPrecoPromocional(valorOpcional(precoPromocional.getText()));
            produto.setPromoInicio(dataOpcional(promoInicio.getText()));
            produto.setPromoFim(dataOpcional(promoFim.getText()));
            produto.setPromoHoraInicio(horaOpcional(promoHoraInicio.getText()));
            produto.setPromoHoraFim(horaOpcional(promoHoraFim.getText()));
            produto.setPromoDias(diasSelecionados());

            String nova = novaCategoria.getText().strip();
            if (!nova.isEmpty()) {
                produto.setCategoriaId(aplicacao.getProdutos().salvarCategoria(nova).getId());
            } else {
                Categoria selecionada = (Categoria) categoria.getSelectedItem();
                produto.setCategoriaId(selecionada == null ? null : selecionada.getId());
            }

            aplicacao.getProdutos().salvar(produto);
            aoSalvar.run();
            dispose();
        } catch (NumberFormatException e) {
            Ui.erro(this, "Preço e estoque devem ser números válidos (ex.: 12,90).");
        } catch (DateTimeParseException e) {
            Ui.erro(this, "Datas e horários devem estar no formato 20/10/2026 e 18:00.");
        } catch (ValidacaoException e) {
            Ui.erro(this, e.getMessage());
        } catch (RuntimeException e) {
            Ui.erro(this, "Falha ao salvar: " + e.getMessage(), e);
        }
    }

    private BigDecimal valorOpcional(String texto) {
        return texto == null || texto.isBlank()
                ? null : new BigDecimal(texto.strip().replace(',', '.'));
    }

    private LocalDate dataOpcional(String texto) {
        return texto == null || texto.isBlank()
                ? null : LocalDate.parse(texto.strip(), DATA_ENTRADA);
    }

    private LocalTime horaOpcional(String texto) {
        return texto == null || texto.isBlank()
                ? null : LocalTime.parse(texto.strip(), HORA_ENTRADA);
    }

    private Set<DayOfWeek> diasSelecionados() {
        Set<DayOfWeek> dias = new LinkedHashSet<>();
        diasPromocao.forEach((dia, caixa) -> {
            if (caixa.isSelected()) {
                dias.add(dia);
            }
        });
        return dias.size() == DayOfWeek.values().length ? Set.of() : dias;
    }
}
