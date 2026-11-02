package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.Cliente;
import com.portfolio.sistemaloja.model.Fiado;
import com.portfolio.sistemaloja.model.FormaPagamento;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.model.VendaItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JTable;
import java.awt.Component;
import java.awt.Container;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lista de recebiveis do painel de fiado. O painel lista PENDENTES (ABERTO/PARCIAL/
 * INADIMPLENTE, issues #44/#43): um fiado com recebimento parcial nao pode
 * desaparecer da fila de cobranca, e o abrir do painel marca a vencencia no banco —
 * a tela nao deriva status nenhum, so mostra o que existe.
 */
class PainelFiadoTest {

    private Aplicacao app;
    private Usuario admin;
    private Cliente cliente;

    @BeforeEach
    void preparar() {
        app = BancoTeste.criar("painelfiado" + System.nanoTime());
        admin = app.getAuth().autenticar("admin", "admin123");
        BancoTeste.abrirCaixa(app, admin);
        app.getConfiguracoes().definirFiadoHabilitado(true);
        cliente = app.getClientes().salvar(new Cliente(null, "Joao Lima", "52998224725",
                "11988887777", "joao@exemplo.com", true));
        app.getFiados().definirLimiteCredito(cliente.getId(), new BigDecimal("500.00"), admin.getId());
    }

    @Test
    void semFiadosListaVazia() {
        PainelFiado painel = new PainelFiado(app);

        assertEquals(0, tabela(painel).getRowCount());
    }

    @Test
    void mostraFiadoAbertoComValoresFormatados() {
        Fiado fiado = gerarFiado(LocalDate.now().plusDays(15));

        PainelFiado painel = new PainelFiado(app);
        JTable tabela = tabela(painel);

        assertEquals(1, tabela.getRowCount());
        assertEquals(cliente.getId(), tabela.getValueAt(0, 0));
        assertEquals("ABERTO", tabela.getValueAt(0, 7));
        assertTrue(tabela.getValueAt(0, 2).toString().contains("100"),
                tabela.getValueAt(0, 2).toString());
        assertEquals(fiado.dataVencimento(), tabela.getValueAt(0, 6));
    }

    @Test
    void vencidoEhInadimplenteNoBancoENaTela() {
        Fiado fiado = gerarFiado(LocalDate.now().minusDays(3));

        PainelFiado painel = new PainelFiado(app);

        assertEquals(1, tabela(painel).getRowCount());
        assertEquals("INADIMPLENTE", tabela(painel).getValueAt(0, 7));
        assertEquals("INADIMPLENTE",
                app.getFiados().listarPorCliente(cliente.getId()).get(0).status(),
                "abrir o painel marca a vencencia no banco; a tela nao deriva mais nada");
        assertEquals("INADIMPLENTE", app.getFiados().listarPendentes().get(0).status(),
                "seguem na fila de cobranca");
        org.junit.jupiter.api.Assertions.assertNotNull(fiado.id());
    }

    @Test
    void parcialApareceNaListaESegueRecebivel() {
        Fiado fiado = gerarFiado(LocalDate.now().plusDays(10));
        app.getFiados().receber(fiado.id(), new BigDecimal("20.00"), "PIX", admin.getId(), null);

        PainelFiado painel = new PainelFiado(app);
        assertEquals(1, tabela(painel).getRowCount(), "PARCIAL nao pode sumir da fila");
        assertEquals("PARCIAL", tabela(painel).getValueAt(0, 7));

        app.getFiados().receber(fiado.id(), new BigDecimal("80.00"), "PIX", admin.getId(), "resto");
        painel = new PainelFiado(app);
        assertEquals(0, tabela(painel).getRowCount(), "QUITADO sai da fila de pendentes");
        assertEquals("QUITADO", app.getFiados().listarPorCliente(cliente.getId()).get(0).status());
    }

    private Fiado gerarFiado(LocalDate vencimento) {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        Venda venda = new Venda();
        venda.setUsuarioId(admin.getId());
        venda.setFormaPagamento(FormaPagamento.PIX);
        venda.adicionarItem(new VendaItem(agua.getId(), agua.getNome(), 1, agua.getPrecoVenda()));
        long vendaId = app.getVendas().registrar(venda).getId();
        return app.getFiados().gerarFiado(cliente.getId(), vendaId,
                new BigDecimal("100.00"), vencimento, null, null);
    }

    private JTable tabela(Container raiz) {
        JTable achada = buscarTabela(raiz);
        if (achada == null) {
            throw new IllegalStateException("nenhuma tabela encontrada no PainelFiado");
        }
        return achada;
    }

    private JTable buscarTabela(Container raiz) {
        for (Component filho : raiz.getComponents()) {
            if (filho instanceof JTable tabela) {
                return tabela;
            }
            if (filho instanceof Container container) {
                JTable achada = buscarTabela(container);
                if (achada != null) {
                    return achada;
                }
            }
        }
        return null;
    }
}
