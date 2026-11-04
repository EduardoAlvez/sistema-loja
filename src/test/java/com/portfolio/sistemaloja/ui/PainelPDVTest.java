package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.Cliente;
import com.portfolio.sistemaloja.model.ConfiguracoesAplicacao;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.PromocaoProduto;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.service.FidelidadeConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Rectangle;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PainelPDVTest {

    private Aplicacao app;
    private Usuario operador;

    @BeforeEach
    void preparar() {
        app = BancoTeste.criar("painelPdv" + System.nanoTime());
        operador = app.getAuth().autenticar("operador", "caixa123");
    }

    @Test
    void mostraPromocaoEUsaOPrecoVigenteNoCarrinho() {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        agua.setPrecoPromocional(new BigDecimal("1.50"));
        agua.setPromoInicio(LocalDate.of(2000, 1, 1));
        agua.setPromoFim(LocalDate.of(2099, 12, 31));
        app.getProdutos().salvar(agua);

        PainelPDV painel = new PainelPDV(app, operador);
        painel.adicionarProduto(agua);
        painel.adicionarProduto(agua);

        assertEquals(1, painel.itensNoCarrinho());
        assertEquals(2, painel.itemDoCarrinho(0, 1));
        assertEquals(Ui.moeda(new BigDecimal("2.50")), painel.itemDoCarrinho(0, 3),
                "unit deve mostrar o preco de tabela");
        assertEquals(Ui.moeda(new BigDecimal("2.00")), painel.itemDoCarrinho(0, 2),
                "coluna de desconto deve mostrar o ganho da promocao");
        assertEquals(Ui.moeda(new BigDecimal("3.00")), painel.itemDoCarrinho(0, 4));
        assertTrue(String.valueOf(painel.itemDoCarrinho(0, 0))
                .contains("(de " + Ui.moeda(new BigDecimal("2.50"))
                        + " por " + Ui.moeda(new BigDecimal("1.50")) + ")"),
                String.valueOf(painel.itemDoCarrinho(0, 0)));
        assertTrue(painel.descontosRodapeVisiveis());
    }

    @Test
    void semPromocaoMantemOPrecoCheio() {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");

        PainelPDV painel = new PainelPDV(app, operador);
        painel.adicionarProduto(agua);

        assertEquals(Ui.moeda(agua.getPrecoVenda()), painel.itemDoCarrinho(0, 3));
    }

    @Test
    void aplicaPromocaoPorQuantidadeNoCarrinho() {
        Produto refrigerante = app.getProdutos().buscarPorCodigoBarras("7891000100103");
        app.getPromocoes().salvar(promocao(refrigerante, "Refrigerante 3 por 7", 3, "7.00", true));

        PainelPDV painel = new PainelPDV(app, operador);
        painel.adicionarProduto(refrigerante);
        painel.adicionarProduto(refrigerante);
        assertEquals(Ui.moeda(refrigerante.getPrecoVenda()), painel.itemDoCarrinho(0, 3),
                "abaixo da quantidade minima o preco normal deve prevalecer");

        painel.adicionarProduto(refrigerante);

        assertEquals(Ui.moeda(refrigerante.getPrecoVenda()), painel.itemDoCarrinho(0, 3));
        assertEquals(Ui.moeda(new BigDecimal("5.97")), painel.itemDoCarrinho(0, 2),
                "3 por 7 vira desconto de 5,97 no item");
        assertEquals(Ui.moeda(new BigDecimal("21.00")), painel.itemDoCarrinho(0, 4));
        assertTrue(String.valueOf(painel.itemDoCarrinho(0, 0))
                .contains("(de " + Ui.moeda(new BigDecimal("8.99"))
                        + " por " + Ui.moeda(new BigDecimal("7.00")) + ")"),
                String.valueOf(painel.itemDoCarrinho(0, 0)));
    }

    @Test
    void promocaoInativaNaoAlteraOPrecoNemMarcaOCarrinho() {
        Produto refrigerante = app.getProdutos().buscarPorCodigoBarras("7891000100103");
        app.getPromocoes().salvar(promocao(refrigerante, "Refrigerante 3 por 7", 3, "7.00", false));

        PainelPDV painel = new PainelPDV(app, operador);
        painel.adicionarProduto(refrigerante);
        painel.adicionarProduto(refrigerante);
        painel.adicionarProduto(refrigerante);

        assertEquals(Ui.moeda(refrigerante.getPrecoVenda()), painel.itemDoCarrinho(0, 3));
        assertFalse(String.valueOf(painel.itemDoCarrinho(0, 0)).contains("por R$"),
                String.valueOf(painel.itemDoCarrinho(0, 0)));
    }

    @Test
    void editarQuantidadeAbaixoDoMinimoVoltaAoPrecoNormal() {
        Produto refrigerante = app.getProdutos().buscarPorCodigoBarras("7891000100103");
        app.getPromocoes().salvar(promocao(refrigerante, "Refrigerante 3 por 7", 3, "7.00", true));

        PainelPDV painel = new PainelPDV(app, operador);
        painel.adicionarProduto(refrigerante);
        painel.adicionarProduto(refrigerante);
        painel.adicionarProduto(refrigerante);
        assertEquals(Ui.moeda(refrigerante.getPrecoVenda()), painel.itemDoCarrinho(0, 3));
        assertEquals(Ui.moeda(new BigDecimal("5.97")), painel.itemDoCarrinho(0, 2));

        painel.definirQuantidadeNoCarrinho(0, 2);

        assertEquals(Ui.moeda(refrigerante.getPrecoVenda()), painel.itemDoCarrinho(0, 3));
        assertFalse(String.valueOf(painel.itemDoCarrinho(0, 0)).contains("por R$"),
                String.valueOf(painel.itemDoCarrinho(0, 0)));
    }

    @Test
    void descontoManualGanhaRotuloCelulaEDestaqueNoRodape() {
        Produto arroz = app.getProdutos().buscarPorCodigoBarras("7891000200100");
        PainelPDV painel = new PainelPDV(app, operador);
        painel.adicionarProduto(arroz);
        assertFalse(painel.descontosRodapeVisiveis());

        painel.definirDescontoNoCarrinho(0, "3,00");

        assertTrue(String.valueOf(painel.itemDoCarrinho(0, 0)).contains("(com desconto)"),
                String.valueOf(painel.itemDoCarrinho(0, 0)));
        assertEquals(Ui.moeda(new BigDecimal("3.00")), painel.itemDoCarrinho(0, 2));
        assertEquals(Ui.moeda(arroz.getPrecoVenda()), painel.itemDoCarrinho(0, 3));
        assertTrue(painel.descontosRodapeVisiveis());
        assertTrue(painel.textoDescontosRodape().contains(Ui.moeda(new BigDecimal("3.00"))),
                painel.textoDescontosRodape());
    }

    @Test
    void destaqueDeDescontoNaoVazaParaOutrasCelulas() {
        Produto arroz = app.getProdutos().buscarPorCodigoBarras("7891000200100");
        PainelPDV painel = new PainelPDV(app, operador);
        painel.adicionarProduto(arroz);
        painel.definirDescontoNoCarrinho(0, "3,00");

        JLabel desconto = (JLabel) painel.celulaRenderizada(0, 2);
        Color corDesconto = desconto.getForeground();
        int estiloDesconto = desconto.getFont().getStyle();
        String textoDesconto = desconto.getText();

        JLabel subtotal = (JLabel) painel.celulaRenderizada(0, 4);

        assertEquals(Ui.COR_ALERTA, corDesconto);
        assertEquals(Font.BOLD, estiloDesconto);
        assertTrue(textoDesconto.startsWith("\u2212"), textoDesconto);
        assertNotEquals(Ui.COR_ALERTA, subtotal.getForeground());
        assertEquals(Font.PLAIN, subtotal.getFont().getStyle());
    }

    @Test
    void promocaoComAcrescimoManualMantemRotuloDePorEJaSomaNoRodape() {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        agua.setPrecoPromocional(new BigDecimal("1.50"));
        agua.setPromoInicio(LocalDate.of(2000, 1, 1));
        agua.setPromoFim(LocalDate.of(2099, 12, 31));
        app.getProdutos().salvar(agua);

        PainelPDV painel = new PainelPDV(app, operador);
        painel.adicionarProduto(agua);
        painel.adicionarProduto(agua);

        painel.definirDescontoNoCarrinho(0, "2.60");

        String nome = String.valueOf(painel.itemDoCarrinho(0, 0));
        assertTrue(nome.contains("por "), nome);
        assertFalse(nome.contains("com desconto"), nome);
        assertEquals(Ui.moeda(new BigDecimal("2.60")), painel.itemDoCarrinho(0, 2));
        assertEquals(Ui.moeda(new BigDecimal("2.40")), painel.itemDoCarrinho(0, 4));
        assertTrue(painel.textoDescontosRodape().contains(Ui.moeda(new BigDecimal("2.60"))),
                painel.textoDescontosRodape());
    }

    @Test
    void detectaMudancaDoValorDoPontoEntreATelaEAFinalizacao() {
        Cliente cliente = app.getClientes().salvar(new Cliente(null, "Maria Souza", "52998224725", null,
                null, true));
        app.getFidelidadeRepository().aplicarMovimentos(List.of(
                app.getFidelidade().credito(cliente.getId(), null, 100, "Credito de teste")));
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");

        PainelPDV painel = new PainelPDV(app, operador);
        painel.adicionarProduto(agua);
        painel.selecionarCliente(cliente);
        painel.informarPontos(20);
        assertEquals(Ui.moeda(new BigDecimal("1.50")), painel.totalAtual(),
                "20 pontos a 0,05 valem 1,00 de desconto");
        assertFalse(painel.valoresDivergemDaTela());

        ConfiguracoesAplicacao config = app.getConfiguracoes().atual();
        app.getAuth().autenticar("admin", "admin123");
        app.getConfiguracoes().salvarLoja(
                new FidelidadeConfig(config.fidelidade().pontosPorReal(), new BigDecimal("0.04")),
                config.pix());

        assertTrue(painel.valoresDivergemDaTela(),
                "a mudanca do valor do ponto deve ser detectada antes de finalizar");

        painel.reprecificarCarrinho();

        assertFalse(painel.valoresDivergemDaTela());
        assertEquals(Ui.moeda(new BigDecimal("1.70")), painel.totalAtual(),
                "20 pontos a 0,04 valem 0,80 de desconto");
    }

    private PromocaoProduto promocao(Produto produto, String nome, int quantidade, String preco,
                                     boolean ativa) {
        PromocaoProduto promocao = new PromocaoProduto();
        promocao.setNome(nome);
        promocao.setProdutoId(produto.getId());
        promocao.setQuantidadeMinima(quantidade);
        promocao.setPrecoPromocional(new BigDecimal(preco));
        promocao.setAtiva(ativa);
        return promocao;
    }

    @Test
    void mostraOSaldoDoClienteEAplicaOResgateNoTotal() {
        Cliente cliente = app.getClientes().salvar(new Cliente(null, "Maria Souza", "52998224725", null,
                null, true));
        app.getFidelidadeRepository().aplicarMovimentos(List.of(
                app.getFidelidade().credito(cliente.getId(), null, 100, "Credito de teste")));
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");

        PainelPDV painel = new PainelPDV(app, operador);
        painel.adicionarProduto(agua);
        painel.selecionarCliente(cliente);

        assertEquals("Saldo: 100 ponto(s)", painel.textoSaldoPontos());
        assertTrue(painel.resgateDePontosHabilitado());

        painel.informarPontos(20);

        assertEquals(Ui.moeda(new BigDecimal("1.50")), painel.totalAtual());
    }

    @Test
    void semClienteOResgateDePontosFicaDesabilitado() {
        PainelPDV painel = new PainelPDV(app, operador);

        assertFalse(painel.resgateDePontosHabilitado());
        assertEquals(" ", painel.textoSaldoPontos());
    }

    @Test
    void controlesDoPdvNaoSobrepoemNemEstouramATela() {
        PainelPDV painel = new PainelPDV(app, operador);
        painel.setSize(1100, 720);
        aplicarLayout(painel);

        List<Component> controles = controles(painel);
        assertTrue(controles.size() > 15, "controles do PDV nao encontrados");

        for (Component controle : controles) {
            Rectangle area = areaNoPainel(painel, controle);
            assertTrue(area.width > 0 && area.height > 0,
                    "controle sem area util: " + nome(controle) + " / " + classe(controle));
            assertTrue(area.x + area.width <= 1100,
                    "controle estourou a largura da tela: " + nome(controle) + " " + area);
            assertTrue(area.y + area.height <= 720,
                    "controle estourou a altura da tela: " + nome(controle) + " " + area);
        }

        for (int i = 0; i < controles.size(); i++) {
            for (int j = i + 1; j < controles.size(); j++) {
                Component primeiro = controles.get(i);
                Component segundo = controles.get(j);
                if (descendente(primeiro, segundo) || descendente(segundo, primeiro)) {
                    continue;
                }
                Rectangle inter = areaNoPainel(painel, primeiro).intersection(areaNoPainel(painel, segundo));
                assertTrue(inter.width <= 0 || inter.height <= 0,
                        "controles sobrepostos: " + nome(primeiro) + " e " + nome(segundo) + " " + inter);
            }
        }
    }

    private void aplicarLayout(Component componente) {
        componente.doLayout();
        if (componente instanceof Container container) {
            for (Component filho : container.getComponents()) {
                aplicarLayout(filho);
            }
        }
    }

    private List<Component> controles(Component componente) {
        List<Component> lista = new ArrayList<>();
        if (componente instanceof JButton || componente instanceof JTextField
                || componente instanceof JComboBox<?> || componente instanceof JLabel) {
            if (!parteDeWidget(componente)) {
                lista.add(componente);
            }
        }
        if (componente instanceof Container container) {
            for (Component filho : container.getComponents()) {
                lista.addAll(controles(filho));
            }
        }
        return lista;
    }

    private boolean parteDeWidget(Component componente) {
        Component atual = componente.getParent();
        while (atual != null) {
            if (atual instanceof JComboBox<?> || atual instanceof javax.swing.JScrollPane) {
                return true;
            }
            atual = atual.getParent();
        }
        return false;
    }

    private Rectangle areaNoPainel(Component raiz, Component componente) {
        Rectangle area = new Rectangle(componente.getX(), componente.getY(),
                componente.getWidth(), componente.getHeight());
        Component atual = componente.getParent();
        while (atual != null && atual != raiz) {
            area.translate(atual.getX(), atual.getY());
            atual = atual.getParent();
        }
        return area;
    }

    private boolean descendente(Component possivelPai, Component componente) {
        Component atual = componente.getParent();
        while (atual != null) {
            if (atual == possivelPai) {
                return true;
            }
            atual = atual.getParent();
        }
        return false;
    }

    private String nome(Component componente) {
        if (componente instanceof JButton botao) {
            return "botao '" + botao.getText() + "'";
        }
        if (componente instanceof JLabel rotulo) {
            return "rotulo '" + rotulo.getText() + "'";
        }
        return componente.getClass().getSimpleName();
    }

    private String classe(Component componente) {
        return componente.getClass().getName() + " pai=" + (componente.getParent() == null
                ? "null" : componente.getParent().getClass().getName());
    }
}
