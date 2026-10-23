package com.portfolio.sistemaloja.ui;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.Autorizacao;
import com.portfolio.sistemaloja.model.MotivoAutorizacao;
import com.portfolio.sistemaloja.service.ValidacaoException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PainelAutorizacaoTest {

    @Test
    void autorizaComCredenciaisValidas() {
        Aplicacao app = BancoTeste.criar("painelaut" + System.nanoTime());
        app.getAuth().autenticar("operador", "caixa123");

        PainelAutorizacao painel = new PainelAutorizacao(app.getAutorizacoes(),
                MotivoAutorizacao.SANGRIA);
        painel.definirLogin("admin");
        painel.definirPin("1234");

        Autorizacao autorizacao = painel.autorizar();

        assertNotNull(autorizacao);
        assertEquals(MotivoAutorizacao.SANGRIA, autorizacao.motivo());
    }

    @Test
    void rejeitaPinInvalido() {
        Aplicacao app = BancoTeste.criar("painelautinvalido" + System.nanoTime());
        app.getAuth().autenticar("operador", "caixa123");

        PainelAutorizacao painel = new PainelAutorizacao(app.getAutorizacoes(),
                MotivoAutorizacao.CANCELAMENTO_VENDA);
        painel.definirLogin("admin");
        painel.definirPin("0000");

        assertThrows(ValidacaoException.class, painel::autorizar);
    }
}
