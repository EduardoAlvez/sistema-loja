package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.Cliente;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.PromocaoProduto;
import com.portfolio.sistemaloja.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        assertEquals(Ui.moeda(new BigDecimal("1.50")), painel.itemDoCarrinho(0, 3));
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

        assertEquals(Ui.moeda(new BigDecimal("7.00")), painel.itemDoCarrinho(0, 3));
        assertTrue(String.valueOf(painel.itemDoCarrinho(0, 0)).contains("(promo)"),
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
        assertFalse(String.valueOf(painel.itemDoCarrinho(0, 0)).contains("(promo)"),
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
        assertEquals(Ui.moeda(new BigDecimal("7.00")), painel.itemDoCarrinho(0, 3));

        painel.definirQuantidadeNoCarrinho(0, 2);

        assertEquals(Ui.moeda(refrigerante.getPrecoVenda()), painel.itemDoCarrinho(0, 3));
        assertFalse(String.valueOf(painel.itemDoCarrinho(0, 0)).contains("(promo)"),
                String.valueOf(painel.itemDoCarrinho(0, 0)));
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
