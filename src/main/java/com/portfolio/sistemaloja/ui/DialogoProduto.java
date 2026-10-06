package com.portfolio.sistemaloja.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;

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
    private final JCheckBox ativo = new JCheckBox("Produto ativo");

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

        gbc.gridx = 1;
        gbc.gridy = 6;
        formulario.add(ativo, gbc);

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

    private void preencher() {
        codigo.setText(produto.getCodigoBarras());
        nome.setText(produto.getNome());
        custo.setText(String.valueOf(produto.getPrecoCusto()));
        venda.setText(String.valueOf(produto.getPrecoVenda()));
        estoque.setText(String.valueOf(produto.getEstoque()));
        ativo.setSelected(produto.isAtivo());
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
            produto.setAtivo(ativo.isSelected());

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
        } catch (ValidacaoException e) {
            Ui.erro(this, e.getMessage());
        } catch (RuntimeException e) {
            Ui.erro(this, "Falha ao salvar: " + e.getMessage(), e);
        }
    }
}
