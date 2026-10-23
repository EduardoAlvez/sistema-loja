package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.impressao.ImpressaoCupomService;
import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.service.CupomTermicoService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

public class DialogoCupomTermico extends JDialog {

    private static final long serialVersionUID = 1L;

    private final Venda venda;
    private final ImpressaoCupomService impressao;
    private final JComboBox<String> impressoras = new JComboBox<>();
    private final JButton imprimir = new JButton("Imprimir");
    private final JButton salvarBin = new JButton("Salvar .bin");

    public DialogoCupomTermico(Frame pai, Venda venda, CupomTermicoService texto,
            ImpressaoCupomService impressao) {
        super(pai, "Cupom térmico 80mm", true);
        this.venda = venda;
        this.impressao = impressao;
        setLayout(new BorderLayout(8, 8));
        cast();

        for (String nome : impressao.impressoras()) {
            impressoras.addItem(nome);
        }
        preselecionarImpressora();
        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        topo.add(new JLabel("Impressora:"));
        topo.add(impressoras);
        if (impressoras.getItemCount() == 0) {
            JLabel aviso = new JLabel("Nenhuma impressora instalada — salve o .bin.");
            aviso.setForeground(Ui.COR_ALERTA);
            topo.add(aviso);
        }
        add(topo, BorderLayout.NORTH);

        JTextArea area = new JTextArea(texto.formatar(venda));
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setEditable(false);
        area.setCaretPosition(0);
        JScrollPane rolagem = new JScrollPane(area);
        rolagem.setPreferredSize(new Dimension(520, 580));
        add(rolagem, BorderLayout.CENTER);

        imprimir.setEnabled(impressoras.getItemCount() > 0);
        imprimir.setToolTipText(impressoras.getItemCount() == 0
                ? "Instale uma fila de impressão ou salve o cupom em .bin"
                : "Envia os bytes ESC/POS para a fila selecionada");
        imprimir.addActionListener(e -> imprimir());
        salvarBin.addActionListener(e -> salvarBin());
        JButton salvarTxt = new JButton("Salvar .txt");
        salvarTxt.addActionListener(e -> salvarTxt(area.getText()));
        JButton fechar = new JButton("Fechar");
        fechar.addActionListener(e -> dispose());
        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        acoes.add(imprimir);
        acoes.add(salvarBin);
        acoes.add(salvarTxt);
        acoes.add(fechar);
        add(acoes, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(pai);
    }

    private void cast() {
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    private void preselecionarImpressora() {
        String preferida = com.portfolio.sistemaloja.impressao.ImpressoraConfig.padrao().nome();
        if (preferida == null || preferida.isBlank()) {
            return;
        }
        for (int i = 0; i < impressoras.getItemCount(); i++) {
            if (preferida.equals(impressoras.getItemAt(i))) {
                impressoras.setSelectedIndex(i);
                return;
            }
        }
    }

    private void imprimir() {
        String nome = (String) impressoras.getSelectedItem();
        if (nome == null) {
            Ui.erro(this, "Selecione uma impressora.");
            return;
        }
        alternarBotoes(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                impressao.imprimir(venda, nome);
                return null;
            }

            @Override
            protected void done() {
                alternarBotoes(true);
                if (!isDisplayable()) {
                    return;
                }
                try {
                    get();
                    Ui.info(DialogoCupomTermico.this, "Cupom enviado para " + nome + ".");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    Throwable causa = e.getCause() == null ? e : e.getCause();
                    Ui.erro(DialogoCupomTermico.this,
                            "Falha ao imprimir: " + causa.getMessage(), causa);
                }
            }
        }.execute();
    }

    private void alternarBotoes(boolean habilitado) {
        imprimir.setEnabled(habilitado && impressoras.getItemCount() > 0);
        salvarBin.setEnabled(habilitado);
    }

    private void salvarBin() {
        JFileChooser escolha = new JFileChooser();
        escolha.setSelectedFile(new java.io.File("cupom-80mm.bin"));
        if (escolha.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path destino = escolha.getSelectedFile().toPath();
        try {
            Files.write(destino, impressao.bytes(venda));
            Ui.info(this, "Cupom binário salvo em " + destino
                    + ".\nEnvie com: copy /b \"" + destino + "\" \\\\localhost\\NOME_DA_FILA");
        } catch (IOException e) {
            Ui.erro(this, "Falha ao salvar o cupom: " + e.getMessage(), e);
        }
    }

    private void salvarTxt(String conteudo) {
        JFileChooser escolha = new JFileChooser();
        escolha.setSelectedFile(new java.io.File("cupom-80mm.txt"));
        if (escolha.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path destino = escolha.getSelectedFile().toPath();
        try {
            Files.writeString(destino, conteudo, StandardCharsets.UTF_8);
            Ui.info(this, "Cupom salvo em " + destino + ".");
        } catch (IOException e) {
            Ui.erro(this, "Falha ao salvar o cupom: " + e.getMessage(), e);
        }
    }
}
