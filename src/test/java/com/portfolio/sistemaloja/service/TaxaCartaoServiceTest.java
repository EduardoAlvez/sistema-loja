package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.TaxaCartao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cobertura do calculo de taxa de cartao. A busca por bandeira/tipo/parcelas tinha um
 * SQL com coluna inexistente e placeholder desalinhado: antes destes testes todo
 * calcular() morria em RepositorioException ou devolvia taxa zero para sempre.
 */
class TaxaCartaoServiceTest {

    private Aplicacao app;

    @BeforeEach
    void preparar() {
        app = BancoTeste.criar("taxacartao" + System.nanoTime());
    }

    @Test
    void calculaTaxaPercentualMaisFixa() {
        app.getTaxasCartao().salvar(taxa("VISA", "CREDITO", 1, 1, "3.5000", "0.49"));

        TaxaCartaoService.CalculoTaxa calculo =
                app.getTaxasCartao().calcular(new BigDecimal("100.00"), "visa", "CREDITO", 1);

        assertEquals(0, new BigDecimal("3.99").compareTo(calculo.valorTaxa()), calculo.valorTaxa().toString());
        assertEquals(0, new BigDecimal("96.01").compareTo(calculo.valorLiquido()),
                calculo.valorLiquido().toString());
        assertNotNull(calculo.taxa());
        assertEquals("VISA", calculo.taxa().getBandeira());
    }

    @Test
    void arredondaMeioParaCimaEmEscalaDeDois() {
        app.getTaxasCartao().salvar(taxa("MASTERCARD", "CREDITO", 1, 1, "4.5000", "0"));

        TaxaCartaoService.CalculoTaxa calculo =
                app.getTaxasCartao().calcular(new BigDecimal("87.30"), "MASTERCARD", "CREDITO", 1);

        // 87.30 * 0.045 = 3.9285 -> HALF_UP 2 casas = 3.93
        assertEquals(0, new BigDecimal("3.93").compareTo(calculo.valorTaxa()), calculo.valorTaxa().toString());
        assertEquals(0, new BigDecimal("83.37").compareTo(calculo.valorLiquido()),
                calculo.valorLiquido().toString());
    }

    @Test
    void semTaxaCadastradaTaxaEZeroELiquidoEBruto() {
        TaxaCartaoService.CalculoTaxa calculo =
                app.getTaxasCartao().calcular(new BigDecimal("50.00"), "ELO", "DEBITO", 1);

        assertEquals(0, BigDecimal.ZERO.compareTo(calculo.valorTaxa()));
        assertEquals(0, new BigDecimal("50.00").compareTo(calculo.valorLiquido()));
        assertNull(calculo.taxa());
    }

    @Test
    void faixaDeParcelasCasaSomenteDentroDoIntervalo() {
        app.getTaxasCartao().salvar(taxa("VISA", "CREDITO", 10, 12, "4.2000", "0.99"));

        assertNotNull(app.getTaxasCartao().calcular(new BigDecimal("100.00"), "VISA", "CREDITO", 11).taxa());
        assertNull(app.getTaxasCartao().calcular(new BigDecimal("100.00"), "VISA", "CREDITO", 7).taxa());
    }

    @Test
    void taxaInativaNaoCasa() {
        TaxaCartao inativa = taxa("VISA", "DEBITO", 1, 1, "2.9900", "0.50");
        inativa.setAtivo(false);
        app.getTaxasCartao().salvar(inativa);

        assertNull(app.getTaxasCartao().calcular(new BigDecimal("100.00"), "VISA", "DEBITO", 1).taxa());
    }

    @Test
    void valorNuloOuNaoPositivoDevolveTaxaZero() {
        assertEquals(0, BigDecimal.ZERO.compareTo(
                app.getTaxasCartao().calcular(null, "VISA", "CREDITO", 1).valorTaxa()));
        assertEquals(0, BigDecimal.ZERO.compareTo(
                app.getTaxasCartao().calcular(new BigDecimal("-5.00"), "VISA", "CREDITO", 1).valorTaxa()));
    }

    @Test
    void salvarRejeitaConfiguracaoInvalida() {
        assertThrows(ValidacaoException.class,
                () -> app.getTaxasCartao().salvar(taxa(" ", "CREDITO", 1, 1, "1.0000", "0")));
        assertThrows(ValidacaoException.class,
                () -> app.getTaxasCartao().salvar(taxa("VISA", " ", 1, 1, "1.0000", "0")));
        assertThrows(ValidacaoException.class,
                () -> app.getTaxasCartao().salvar(taxa("VISA", "CREDITO", 0, 1, "1.0000", "0")));
        assertThrows(ValidacaoException.class,
                () -> app.getTaxasCartao().salvar(taxa("VISA", "CREDITO", 5, 2, "1.0000", "0")));
        assertThrows(ValidacaoException.class,
                () -> app.getTaxasCartao().salvar(taxa("VISA", "CREDITO", 1, 1, "-1.0000", "0")));
        assertThrows(ValidacaoException.class,
                () -> app.getTaxasCartao().salvar(taxa("VISA", "CREDITO", 1, 1, "1.0000", "-0.01")));
    }

    @Test
    void buscaECaseInsensitiveEListaApenasAtivas() {
        app.getTaxasCartao().salvar(taxa("visa", "debito", 1, 1, "1.0000", "0"));
        TaxaCartao inativa = taxa("amex", "CREDITO", 1, 1, "2.0000", "0");
        inativa.setAtivo(false);
        app.getTaxasCartao().salvar(inativa);

        assertNotNull(app.getTaxasCartao().calcular(new BigDecimal("10.00"), "VISA", "DEBITO", 1).taxa());
        assertTrue(app.getTaxasCartao().listar(true).stream().allMatch(TaxaCartao::isAtivo));
        assertEquals(2, app.getTaxasCartao().listar(false).size());
    }

    private TaxaCartao taxa(String bandeira, String tipo, int min, int max,
                            String percentual, String fixa) {
        TaxaCartao taxa = new TaxaCartao();
        taxa.setBandeira(bandeira);
        taxa.setTipoCartao(tipo);
        taxa.setParcelasMin(min);
        taxa.setParcelasMax(max);
        taxa.setTaxaPercentual(new BigDecimal(percentual));
        taxa.setTaxaFixa(new BigDecimal(fixa));
        return taxa;
    }
}
