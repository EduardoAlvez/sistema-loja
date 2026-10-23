package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.DevolucaoItem;
import com.portfolio.sistemaloja.model.FormaPagamento;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.model.VendaItem;
import com.portfolio.sistemaloja.model.VendaPagamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VendaServiceTest {

    private Aplicacao app;
    private Usuario operador;

    @BeforeEach
    void preparar() {
        app = BancoTeste.criar("venda" + System.nanoTime());
        operador = app.getAuth().autenticar("operador", "caixa123");
    }

    @Test
    void registrarVendaBaixaEstoqueECalculaTroco() {
        Produto refrigerante = app.getProdutos().buscarPorCodigoBarras("7891000100103");
        int estoqueInicial = refrigerante.getEstoque();

        Venda venda = novaVenda(refrigerante, 3, FormaPagamento.DINHEIRO, new BigDecimal("50.00"));
        Venda registrada = app.getVendas().registrar(venda);

        assertNotNull(registrada.getId());
        assertTrue(registrada.getNumero().startsWith("V"));
        assertEquals(0, new BigDecimal("26.97").compareTo(registrada.getTotal()));
        assertEquals(0, new BigDecimal("23.03").compareTo(registrada.getTroco()));

        Produto aposVenda = app.getProdutos().buscarPorCodigoBarras("7891000100103");
        assertEquals(estoqueInicial - 3, aposVenda.getEstoque());
    }

    @Test
    void naoPermiteVenderAcimaDoEstoque() {
        Produto arroz = app.getProdutos().buscarPorCodigoBarras("7891000200100");
        int estoqueInicial = arroz.getEstoque();

        Venda venda = novaVenda(arroz, estoqueInicial + 1, FormaPagamento.PIX, null);
        assertThrows(ValidacaoException.class, () -> app.getVendas().registrar(venda));

        assertEquals(estoqueInicial, app.getProdutos().buscarPorCodigoBarras("7891000200100").getEstoque());
    }

    @Test
    void naoPermiteCarrinhoVazio() {
        Venda venda = new Venda();
        venda.setUsuarioId(operador.getId());
        venda.setFormaPagamento(FormaPagamento.PIX);
        assertThrows(ValidacaoException.class, () -> app.getVendas().registrar(venda));
    }

    @Test
    void naoPermiteValorPagoMenorQueTotalNoDinheiro() {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        Venda venda = novaVenda(agua, 10, FormaPagamento.DINHEIRO, new BigDecimal("1.00"));
        assertThrows(ValidacaoException.class, () -> app.getVendas().registrar(venda));
    }

    @Test
    void cancelarVendaDevolveEstoque() {
        Produto feijao = app.getProdutos().buscarPorCodigoBarras("7891000200101");
        int estoqueInicial = feijao.getEstoque();

        Venda registrada = app.getVendas().registrar(
                novaVenda(feijao, 5, FormaPagamento.DEBITO, null));
        assertEquals(estoqueInicial - 5, app.getProdutos().buscarPorCodigoBarras("7891000200101").getEstoque());

        app.getVendas().cancelar(registrada.getId());

        assertEquals(estoqueInicial, app.getProdutos().buscarPorCodigoBarras("7891000200101").getEstoque());
        assertEquals("CANCELADA", app.getVendas().buscarPorId(registrada.getId()).getStatus().name());
    }

    @Test
    void cancelarAposDevolucaoParcialNaoDevolveEstoqueEmDobro() {
        Produto feijao = app.getProdutos().buscarPorCodigoBarras("7891000200101");
        int estoqueInicial = feijao.getEstoque();

        Venda registrada = app.getVendas().registrar(
                novaVenda(feijao, 5, FormaPagamento.DEBITO, null));
        VendaItem item = registrada.getItens().get(0);

        app.getDevolucoes().devolver(registrada.getId(),
                List.of(new DevolucaoItem(item.getId(), null, null, 2, null)), false, null, operador);
        assertEquals(estoqueInicial - 3, app.getProdutos().buscarPorCodigoBarras("7891000200101").getEstoque());

        app.getVendas().cancelar(registrada.getId());

        assertEquals(estoqueInicial, app.getProdutos().buscarPorCodigoBarras("7891000200101").getEstoque(),
                "o cancelamento deve devolver apenas as 3 unidades ainda em poder do cliente");
        assertEquals("CANCELADA", app.getVendas().buscarPorId(registrada.getId()).getStatus().name());
    }

    @Test
    void somaQuantidadesDoMesmoProdutoNoCarrinho() {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        Venda venda = new Venda();
        venda.setUsuarioId(operador.getId());
        venda.setFormaPagamento(FormaPagamento.PIX);
        venda.adicionarItem(new VendaItem(agua.getId(), agua.getNome(), 2, agua.getPrecoVenda()));
        venda.adicionarItem(new VendaItem(agua.getId(), agua.getNome(), 3, agua.getPrecoVenda()));

        Venda registrada = app.getVendas().registrar(venda);

        assertEquals(1, registrada.getItens().size());
        assertEquals(5, registrada.getItens().get(0).getQuantidade());
        assertEquals(0, new BigDecimal("12.50").compareTo(registrada.getTotal()));
    }

    @Test
    void descontoAbatidoDoTotalETrocadoNoTroco() {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        Venda venda = novaVenda(agua, 4, FormaPagamento.DINHEIRO, new BigDecimal("10.00"));
        venda.setDesconto(new BigDecimal("1.00"));

        Venda registrada = app.getVendas().registrar(venda);

        assertEquals(0, new BigDecimal("1.00").compareTo(registrada.getDesconto()));
        assertEquals(0, new BigDecimal("9.00").compareTo(registrada.getTotal()));
        assertEquals(0, new BigDecimal("1.00").compareTo(registrada.getTroco()));

        Venda buscada = app.getVendas().buscarPorId(registrada.getId());
        assertEquals(0, new BigDecimal("1.00").compareTo(buscada.getDesconto()));
        assertEquals(0, new BigDecimal("9.00").compareTo(buscada.getTotal()));
    }

    @Test
    void naoPermiteDescontoNegativo() {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        Venda venda = novaVenda(agua, 1, FormaPagamento.PIX, null);
        venda.setDesconto(new BigDecimal("-0.01"));
        assertThrows(ValidacaoException.class, () -> app.getVendas().registrar(venda));
    }

    @Test
    void naoPermiteDescontoMaiorQueOsItens() {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        Venda venda = novaVenda(agua, 1, FormaPagamento.PIX, null);
        venda.setDesconto(new BigDecimal("2.51"));
        assertThrows(ValidacaoException.class, () -> app.getVendas().registrar(venda));
    }

    @Test
    void naoPermiteDescontoIgualAoTotalDosItens() {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        Venda venda = novaVenda(agua, 2, FormaPagamento.PIX, null);
        venda.setDesconto(new BigDecimal("5.00"));
        assertThrows(ValidacaoException.class, () -> app.getVendas().registrar(venda));
    }

    @Test
    void descontoPorItemAbatidoNoSubtotal() {
        Usuario admin = app.getAuth().autenticar("admin", "admin123");
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        Venda venda = new Venda();
        venda.setUsuarioId(admin.getId());
        venda.setFormaPagamento(FormaPagamento.PIX);
        VendaItem item = new VendaItem(agua.getId(), agua.getNome(), 2, agua.getPrecoVenda());
        item.setDesconto(new BigDecimal("1.00"));
        venda.adicionarItem(item);

        Venda registrada = app.getVendas().registrar(venda);

        assertEquals(0, new BigDecimal("1.00").compareTo(registrada.getItens().get(0).getDesconto()));
        assertEquals(0, new BigDecimal("4.00").compareTo(registrada.getItens().get(0).getSubtotal()));
        assertEquals(0, new BigDecimal("4.00").compareTo(registrada.getTotal()));

        Venda buscada = app.getVendas().buscarPorId(registrada.getId());
        assertEquals(0, new BigDecimal("1.00").compareTo(buscada.getItens().get(0).getDesconto()));
    }

    @Test
    void somaDescontosDeItensDiferentesEVenda() {
        Usuario admin = app.getAuth().autenticar("admin", "admin123");
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        Produto feijao = app.getProdutos().buscarPorCodigoBarras("7891000200101");
        Venda venda = new Venda();
        venda.setUsuarioId(admin.getId());
        venda.setFormaPagamento(FormaPagamento.PIX);
        VendaItem itemAgua = new VendaItem(agua.getId(), agua.getNome(), 4, agua.getPrecoVenda());
        itemAgua.setDesconto(new BigDecimal("1.00"));
        VendaItem itemFeijao = new VendaItem(feijao.getId(), feijao.getNome(), 2, feijao.getPrecoVenda());
        itemFeijao.setDesconto(new BigDecimal("0.50"));
        venda.adicionarItem(itemAgua);
        venda.adicionarItem(itemFeijao);
        venda.setDesconto(new BigDecimal("1.50"));

        Venda registrada = app.getVendas().registrar(venda);

        assertEquals(0, new BigDecimal("0.50").compareTo(registrada.getItens().get(1).getDesconto()));
        BigDecimal somaLiquida = registrada.getItens().stream()
                .map(VendaItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, somaLiquida.subtract(new BigDecimal("1.50")).compareTo(registrada.getTotal()));
    }

    @Test
    void mesclaDescontosDoMesmoProdutoNoCarrinho() {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        Venda venda = new Venda();
        venda.setUsuarioId(operador.getId());
        venda.setFormaPagamento(FormaPagamento.PIX);
        VendaItem primeiro = new VendaItem(agua.getId(), agua.getNome(), 2, agua.getPrecoVenda());
        primeiro.setDesconto(new BigDecimal("0.50"));
        VendaItem segundo = new VendaItem(agua.getId(), agua.getNome(), 2, agua.getPrecoVenda());
        segundo.setDesconto(new BigDecimal("0.50"));
        venda.adicionarItem(primeiro);
        venda.adicionarItem(segundo);

        Venda registrada = app.getVendas().registrar(venda);

        assertEquals(1, registrada.getItens().size());
        assertEquals(4, registrada.getItens().get(0).getQuantidade());
        assertEquals(0, new BigDecimal("1.00").compareTo(registrada.getItens().get(0).getDesconto()));
        assertEquals(0, new BigDecimal("9.00").compareTo(registrada.getTotal()));
    }

    @Test
    void naoPermiteDescontoPorItemNegativo() {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        Venda venda = new Venda();
        venda.setUsuarioId(operador.getId());
        venda.setFormaPagamento(FormaPagamento.PIX);
        VendaItem item = new VendaItem(agua.getId(), agua.getNome(), 1, agua.getPrecoVenda());
        item.setDesconto(new BigDecimal("-0.10"));
        venda.adicionarItem(item);
        assertThrows(ValidacaoException.class, () -> app.getVendas().registrar(venda));
    }

    @Test
    void naoPermiteDescontoPorItemIgualAoBruto() {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        Venda venda = new Venda();
        venda.setUsuarioId(operador.getId());
        venda.setFormaPagamento(FormaPagamento.PIX);
        VendaItem item = new VendaItem(agua.getId(), agua.getNome(), 2, agua.getPrecoVenda());
        item.setDesconto(new BigDecimal("5.00"));
        venda.adicionarItem(item);
        assertThrows(ValidacaoException.class, () -> app.getVendas().registrar(venda));
    }

    @Test
    void operadorPodeAplicarDescontoAteDezPorCento() {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        Venda venda = novaVenda(agua, 4, FormaPagamento.PIX, null);
        venda.setDesconto(new BigDecimal("1.00"));

        Venda registrada = app.getVendas().registrar(venda);

        assertEquals(0, new BigDecimal("1.00").compareTo(registrada.getDesconto()));
        assertEquals(0, new BigDecimal("9.00").compareTo(registrada.getTotal()));
    }

    @Test
    void operadorNaoPodeAplicarDescontoAcimaDoLimiteDoPerfil() {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        Venda venda = novaVenda(agua, 4, FormaPagamento.PIX, null);
        venda.setDesconto(new BigDecimal("1.10"));
        ValidacaoException erro = assertThrows(ValidacaoException.class, () -> app.getVendas().registrar(venda));
        assertTrue(erro.getMessage().contains("10%"));
    }

    @Test
    void descontoDeItemContaNoLimiteDoPerfil() {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        Venda venda = new Venda();
        venda.setUsuarioId(operador.getId());
        venda.setFormaPagamento(FormaPagamento.PIX);
        VendaItem item = new VendaItem(agua.getId(), agua.getNome(), 4, agua.getPrecoVenda());
        item.setDesconto(new BigDecimal("1.00"));
        venda.adicionarItem(item);
        venda.setDesconto(new BigDecimal("0.10"));
        ValidacaoException erro = assertThrows(ValidacaoException.class, () -> app.getVendas().registrar(venda));
        assertTrue(erro.getMessage().contains("10%"));
    }

    @Test
    void adminPodeAplicarDescontoAteCinquentaPorCento() {
        Usuario admin = app.getAuth().autenticar("admin", "admin123");
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        Venda venda = novaVenda(agua, 4, FormaPagamento.PIX, null);
        venda.setUsuarioId(admin.getId());
        venda.setDesconto(new BigDecimal("4.50"));

        Venda registrada = app.getVendas().registrar(venda);

        assertEquals(0, new BigDecimal("4.50").compareTo(registrada.getDesconto()));
        assertEquals(0, new BigDecimal("5.50").compareTo(registrada.getTotal()));
    }

    @Test
    void pagamentoDivididoRegistraParcelasCertinho() {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        Venda venda = novaVenda(agua, 4, null, null);
        venda.setFormaPagamento(FormaPagamento.PIX);
        venda.adicionarPagamento(new VendaPagamento(FormaPagamento.DINHEIRO, new BigDecimal("6.00")));
        venda.adicionarPagamento(new VendaPagamento(FormaPagamento.PIX, new BigDecimal("4.00")));

        Venda registrada = app.getVendas().registrar(venda);

        assertEquals(2, registrada.getPagamentos().size());
        assertEquals(0, new BigDecimal("6.00").compareTo(registrada.getPagamentos().get(0).getValor()));
        assertEquals(FormaPagamento.DINHEIRO, registrada.getPagamentos().get(0).getFormaPagamento());
        assertEquals(FormaPagamento.DINHEIRO, registrada.getFormaPagamento());
        assertEquals(0, new BigDecimal("10.00").compareTo(registrada.getValorPago()));
        assertEquals(0, BigDecimal.ZERO.compareTo(registrada.getTroco()));

        Venda buscada = app.getVendas().buscarPorId(registrada.getId());
        assertEquals(2, buscada.getPagamentos().size());
    }

    @Test
    void pagamentoDivididoRegistraTrocoQuandoDinheiroExcede() {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        Venda venda = novaVenda(agua, 4, null, null);
        venda.setFormaPagamento(FormaPagamento.PIX);
        venda.adicionarPagamento(new VendaPagamento(FormaPagamento.PIX, new BigDecimal("6.00")));
        venda.adicionarPagamento(new VendaPagamento(FormaPagamento.DINHEIRO, new BigDecimal("10.00")));

        Venda registrada = app.getVendas().registrar(venda);

        assertEquals(FormaPagamento.DINHEIRO, registrada.getFormaPagamento());
        assertEquals(0, new BigDecimal("16.00").compareTo(registrada.getValorPago()));
        assertEquals(0, new BigDecimal("6.00").compareTo(registrada.getTroco()));
    }

    @Test
    void somaDePagamentosMenorQueTotalEReprovada() {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        Venda venda = novaVenda(agua, 4, null, null);
        venda.setFormaPagamento(FormaPagamento.PIX);
        venda.adicionarPagamento(new VendaPagamento(FormaPagamento.PIX, new BigDecimal("4.00")));
        venda.adicionarPagamento(new VendaPagamento(FormaPagamento.DINHEIRO, new BigDecimal("5.00")));
        assertThrows(ValidacaoException.class, () -> app.getVendas().registrar(venda));
    }

    @Test
    void excedenteSemDinheiroEReprovado() {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        Venda venda = novaVenda(agua, 4, null, null);
        venda.setFormaPagamento(FormaPagamento.PIX);
        venda.adicionarPagamento(new VendaPagamento(FormaPagamento.PIX, new BigDecimal("4.00")));
        venda.adicionarPagamento(new VendaPagamento(FormaPagamento.DEBITO, new BigDecimal("8.00")));
        ValidacaoException erro = assertThrows(ValidacaoException.class,
                () -> app.getVendas().registrar(venda));
        assertTrue(erro.getMessage().contains("excedente"));
    }

    @Test
    void parcelaComValorZeradoEReprovada() {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        Venda venda = novaVenda(agua, 4, null, null);
        venda.setFormaPagamento(FormaPagamento.PIX);
        venda.adicionarPagamento(new VendaPagamento(FormaPagamento.PIX, new BigDecimal("10.00")));
        venda.adicionarPagamento(new VendaPagamento(FormaPagamento.DINHEIRO, BigDecimal.ZERO));
        assertThrows(ValidacaoException.class, () -> app.getVendas().registrar(venda));
    }

    @Test
    void ultimaVendaDoOperadorTrazARegistradaPorUltimo() {
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        app.getVendas().registrar(novaVenda(agua, 1, FormaPagamento.PIX, null));
        Venda ultima = app.getVendas().registrar(novaVenda(agua, 2, FormaPagamento.PIX, null));

        Venda encontrada = app.getVendas().ultimaDoOperador(operador.getId()).orElseThrow();

        assertEquals(ultima.getId(), encontrada.getId());
        assertEquals(1, encontrada.getItens().size());
        assertEquals(0, new BigDecimal("5.00").compareTo(encontrada.getTotal()));
    }

    @Test
    void ultimaVendaDoOperadorVaziaParaQuemNaoVendeu() {
        Usuario admin = app.getAuth().autenticar("admin", "admin123");
        Produto agua = app.getProdutos().buscarPorCodigoBarras("7891000100104");
        app.getVendas().registrar(novaVenda(agua, 1, FormaPagamento.PIX, null));

        assertTrue(app.getVendas().ultimaDoOperador(admin.getId()).isEmpty());
    }

    @Test
    void aplicaPromocaoVigenteSemConsumirOLimiteDoOperador() {
        Aplicacao promo = appNoHorario(LocalDateTime.of(2026, 10, 20, 12, 0));
        Usuario caixa = promo.getAuth().autenticar("operador", "caixa123");
        Produto agua = promo.getProdutos().buscarPorCodigoBarras("7891000100104");
        agua.setPrecoPromocional(new BigDecimal("1.50"));
        agua.setPromoInicio(LocalDate.of(2026, 10, 1));
        agua.setPromoFim(LocalDate.of(2026, 10, 31));
        promo.getProdutos().salvar(agua);

        Venda venda = new Venda();
        venda.setUsuarioId(caixa.getId());
        venda.setFormaPagamento(FormaPagamento.PIX);
        venda.adicionarItem(new VendaItem(agua.getId(), agua.getNome(), 4, agua.getPrecoVenda()));

        Venda registrada = promo.getVendas().registrar(venda);

        assertEquals(0, new BigDecimal("1.50").compareTo(
                registrada.getItens().get(0).getPrecoUnitario()));
        assertEquals(0, new BigDecimal("6.00").compareTo(registrada.getTotal()));
    }

    @Test
    void foraDaVigenciaGravaOPrecoCheio() {
        Aplicacao promo = appNoHorario(LocalDateTime.of(2026, 10, 20, 12, 0));
        Usuario caixa = promo.getAuth().autenticar("operador", "caixa123");
        Produto agua = promo.getProdutos().buscarPorCodigoBarras("7891000100104");
        agua.setPrecoPromocional(new BigDecimal("1.50"));
        agua.setPromoInicio(LocalDate.of(2026, 11, 1));
        agua.setPromoFim(LocalDate.of(2026, 11, 30));
        promo.getProdutos().salvar(agua);

        Venda venda = new Venda();
        venda.setUsuarioId(caixa.getId());
        venda.setFormaPagamento(FormaPagamento.PIX);
        venda.adicionarItem(new VendaItem(agua.getId(), agua.getNome(), 4, agua.getPrecoVenda()));

        Venda registrada = promo.getVendas().registrar(venda);

        assertEquals(0, new BigDecimal("2.50").compareTo(
                registrada.getItens().get(0).getPrecoUnitario()));
        assertEquals(0, new BigDecimal("10.00").compareTo(registrada.getTotal()));
    }

    @Test
    void limiteDoPerfilUsaOPrecoPromocionalComoBase() {
        Aplicacao promo = appNoHorario(LocalDateTime.of(2026, 10, 20, 12, 0));
        Usuario caixa = promo.getAuth().autenticar("operador", "caixa123");
        Produto agua = promo.getProdutos().buscarPorCodigoBarras("7891000100104");
        agua.setPrecoPromocional(new BigDecimal("1.50"));
        agua.setPromoInicio(LocalDate.of(2026, 10, 1));
        agua.setPromoFim(LocalDate.of(2026, 10, 31));
        promo.getProdutos().salvar(agua);

        Venda venda = new Venda();
        venda.setUsuarioId(caixa.getId());
        venda.setFormaPagamento(FormaPagamento.PIX);
        venda.adicionarItem(new VendaItem(agua.getId(), agua.getNome(), 4, agua.getPrecoVenda()));
        venda.setDesconto(new BigDecimal("0.70"));

        ValidacaoException erro = assertThrows(ValidacaoException.class,
                () -> promo.getVendas().registrar(venda));
        assertTrue(erro.getMessage().contains("10%"), erro.getMessage());
    }

    @Test
    void promocaoRecalculaOItemAntesDeValidarODesconto() {
        Aplicacao promo = appNoHorario(LocalDateTime.of(2026, 10, 20, 12, 0));
        Usuario caixa = promo.getAuth().autenticar("operador", "caixa123");
        Produto agua = promo.getProdutos().buscarPorCodigoBarras("7891000100104");
        agua.setPrecoPromocional(new BigDecimal("1.50"));
        agua.setPromoInicio(LocalDate.of(2026, 10, 1));
        agua.setPromoFim(LocalDate.of(2026, 10, 31));
        promo.getProdutos().salvar(agua);

        Venda venda = new Venda();
        venda.setUsuarioId(caixa.getId());
        venda.setFormaPagamento(FormaPagamento.PIX);
        VendaItem item = new VendaItem(agua.getId(), agua.getNome(), 1, agua.getPrecoVenda());
        item.setDesconto(new BigDecimal("2.00"));
        venda.adicionarItem(item);

        ValidacaoException erro = assertThrows(ValidacaoException.class,
                () -> promo.getVendas().registrar(venda));
        assertTrue(erro.getMessage().contains("subtotal"), erro.getMessage());
    }

    private Aplicacao appNoHorario(LocalDateTime momento) {
        ZoneId zona = ZoneId.systemDefault();
        return BancoTeste.criar("vendaPromo" + System.nanoTime(),
                Clock.fixed(momento.atZone(zona).toInstant(), zona));
    }

    private Venda novaVenda(Produto produto, int quantidade, FormaPagamento pagamento, BigDecimal pago) {
        Venda venda = new Venda();
        venda.setUsuarioId(operador.getId());
        venda.setFormaPagamento(pagamento);
        venda.setValorPago(pago);
        venda.adicionarItem(new VendaItem(produto.getId(), produto.getNome(), quantidade, produto.getPrecoVenda()));
        return venda;
    }
}
