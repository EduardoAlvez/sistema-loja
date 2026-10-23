package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.model.ConfiguracoesAplicacao;
import com.portfolio.sistemaloja.service.ConfiguracaoPix;
import com.portfolio.sistemaloja.service.FidelidadeConfig;
import com.portfolio.sistemaloja.service.ValidacaoException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.util.Set;

public class PainelConfiguracoes extends JPanel {

    private final transient Aplicacao aplicacao;

    private final JTextField pontosPorReal = new JTextField(10);
    private final JTextField valorPorPonto = new JTextField(10);
    private final JTextField pixChave = new JTextField(30);
    private final JTextField pixNome = new JTextField(30);
    private final JTextField pixCidade = new JTextField(20);
    private final JLabel aviso = new JLabel(" ");

    public PainelConfiguracoes(Aplicacao aplicacao) {
        this.aplicacao = aplicacao;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setBackground(Ui.COR_FUNDO);
        add(montarFormulario(), BorderLayout.NORTH);
        carregar();
    }

    private JPanel montarFormulario() {
        JPanel painel = new JPanel(new GridBagLayout());
        painel.setBackground(Ui.COR_FUNDO);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        int linha = 0;
        linha = secao(painel, c, linha, "Fidelidade");
        linha = campo(painel, c, linha, "Pontos por real", pontosPorReal);
        linha = campo(painel, c, linha, "Valor do ponto (R$)", valorPorPonto);
        linha = secao(painel, c, linha, "PIX");
        linha = campo(painel, c, linha, "Chave PIX", pixChave);
        linha = campo(painel, c, linha, "Nome do recebedor", pixNome);
        linha = campo(painel, c, linha, "Cidade", pixCidade);

        c.gridx = 0;
        c.gridy = linha;
        c.gridwidth = 2;
        aviso.setForeground(Ui.COR_ALERTA);
        painel.add(aviso, c);

        JPanel botoes = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        botoes.setBackground(Ui.COR_FUNDO);
        JButton recarregar = new JButton("Recarregar");
        recarregar.addActionListener(e -> carregar());
        JButton salvar = new JButton("Salvar");
        salvar.addActionListener(e -> salvar());
        botoes.add(recarregar);
        botoes.add(salvar);

        JPanel rodape = new JPanel(new BorderLayout());
        rodape.setBackground(Ui.COR_FUNDO);
        rodape.add(botoes, BorderLayout.EAST);

        JPanel conteudo = new JPanel(new BorderLayout());
        conteudo.setBackground(Ui.COR_FUNDO);
        conteudo.add(painel, BorderLayout.NORTH);
        conteudo.add(rodape, BorderLayout.CENTER);

        JPanel externo = new JPanel(new BorderLayout());
        externo.setBackground(Ui.COR_FUNDO);
        externo.add(Ui.titulo("Configurações"), BorderLayout.NORTH);
        externo.add(new JScrollPane(conteudo), BorderLayout.CENTER);
        return externo;
    }

    private int secao(JPanel painel, GridBagConstraints c, int linha, String texto) {
        c.gridx = 0;
        c.gridy = linha++;
        c.gridwidth = 2;
        JLabel titulo = new JLabel(texto);
        titulo.setFont(titulo.getFont().deriveFont(java.awt.Font.BOLD, 14f));
        painel.add(titulo, c);
        c.gridwidth = 1;
        return linha;
    }

    private int campo(JPanel painel, GridBagConstraints c, int linha, String rotulo, JTextField campo) {
        c.gridx = 0;
        c.gridy = linha;
        c.weightx = 0;
        painel.add(Ui.rotulo(rotulo), c);
        c.gridx = 1;
        c.weightx = 1;
        painel.add(campo, c);
        return linha + 1;
    }

    final void carregar() {
        aplicacao.getConfiguracoes().invalidar();
        ConfiguracoesAplicacao cfg = aplicacao.getConfiguracoes().atual();
        pontosPorReal.setText(legivel(cfg.fidelidade().pontosPorReal()));
        valorPorPonto.setText(legivel(cfg.fidelidade().valorPorPonto()));
        pixChave.setText(cfg.pix().chave());
        pixNome.setText(cfg.pix().nomeRecebedor());
        pixCidade.setText(cfg.pix().cidade());

        Set<String> sobrescritas = aplicacao.getConfiguracoes().chavesSobrescritasPorAmbiente();
        pontosPorReal.setEditable(!sobrescritas.contains("pontosPorReal"));
        valorPorPonto.setEditable(!sobrescritas.contains("valorPorPonto"));
        pixChave.setEditable(!sobrescritas.contains("pixChave"));
        pixNome.setEditable(!sobrescritas.contains("pixNome"));
        pixCidade.setEditable(!sobrescritas.contains("pixCidade"));
        aviso.setText(sobrescritas.isEmpty()
                ? " "
                : "Campos desabilitados vêm de propriedade/variável de ambiente e têm prioridade sobre o banco.");
    }

    final void salvar() {
        try {
            FidelidadeConfig fidelidade = new FidelidadeConfig(
                    decimal(pontosPorReal.getText(), "Pontos por real"),
                    decimal(valorPorPonto.getText(), "Valor do ponto"));
            ConfiguracaoPix pix = new ConfiguracaoPix(
                    pixChave.getText().strip(), pixNome.getText().strip(), pixCidade.getText().strip());
            aplicacao.getConfiguracoes().salvarLoja(fidelidade, pix);
            carregar();
            Ui.info(this, "Configurações salvas.");
        } catch (ValidacaoException e) {
            Ui.erro(this, e.getMessage());
        } catch (RuntimeException e) {
            Ui.erro(this, "Falha ao salvar configurações: " + e.getMessage(), e);
        }
    }

    private String legivel(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString();
    }

    private BigDecimal decimal(String texto, String rotulo) {
        try {
            return new BigDecimal(texto.strip().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new ValidacaoException(rotulo + " deve ser um número (ex.: 1 ou 0,05).");
        }
    }

    String pontosPorRealTexto() {
        return pontosPorReal.getText();
    }

    String valorPorPontoTexto() {
        return valorPorPonto.getText();
    }

    boolean pontosPorRealEditavel() {
        return pontosPorReal.isEditable();
    }

    boolean valorPorPontoEditavel() {
        return valorPorPonto.isEditable();
    }
}
