package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.Cliente;
import com.portfolio.sistemaloja.model.FormaPagamento;
import com.portfolio.sistemaloja.model.PerfilUsuario;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.model.VendaItem;
import com.portfolio.sistemaloja.repository.EventoAuditoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuditoriaAcoesTest {

    private Aplicacao app;

    @BeforeEach
    void preparar() {
        app = BancoTeste.criar("acoes" + System.nanoTime());
    }

    @Test
    void loginOkRegistraAuditoria() {
        Usuario admin = app.getAuth().autenticar("admin", "admin123");

        List<EventoAuditoria> eventos = hoje();
        assertEquals(1, eventos.size());
        assertEquals("LOGIN_OK", eventos.get(0).acao());
        assertEquals("admin", eventos.get(0).usuarioLogin());
        assertEquals("USUARIO", eventos.get(0).entidade());
        assertEquals(admin.getId(), eventos.get(0).entidadeId());
    }

    @Test
    void loginFalhaRegistraTentativaSemSessao() {
        assertThrows(ValidacaoException.class,
                () -> app.getAuth().autenticar("admin", "senha-errada"));

        List<EventoAuditoria> eventos = hoje();
        assertEquals(1, eventos.size());
        assertEquals("LOGIN_FALHA", eventos.get(0).acao());
        assertEquals("admin", eventos.get(0).usuarioLogin());
        assertEquals("Login ou senha invalidos", eventos.get(0).detalhe());
    }

    @Test
    void vendaRegistradaECanceladaRegistramAuditoria() {
        app.getAuth().autenticar("operador", "caixa123");
        Produto feijao = app.getProdutos().buscarPorCodigoBarras("7891000200101");

        Venda venda = new Venda();
        venda.setUsuarioId(app.getUsuarioAtual().getId());
        venda.setFormaPagamento(FormaPagamento.PIX);
        venda.adicionarItem(new VendaItem(feijao.getId(), feijao.getNome(), 2, feijao.getPrecoVenda()));
        Venda registrada = app.getVendas().registrar(venda);
        app.getVendas().cancelar(registrada.getId());

        List<EventoAuditoria> eventos = hoje();
        assertEquals(3, eventos.size());
        assertEquals("VENDA_CANCELADA", eventos.get(0).acao());
        assertEquals(registrada.getId(), eventos.get(0).entidadeId());
        assertEquals("VENDA_REGISTRADA", eventos.get(1).acao());
        assertEquals(registrada.getId(), eventos.get(1).entidadeId());
        assertEquals("LOGIN_OK", eventos.get(2).acao());
    }

    @Test
    void produtoSalvoRegistraAuditoria() {
        app.getAuth().autenticar("admin", "admin123");
        Produto produto = new Produto();
        produto.setNome("Café Torrado 500g");
        produto.setCodigoBarras("7891000999991");
        produto.setPrecoCusto(new BigDecimal("15.00"));
        produto.setPrecoVenda(new BigDecimal("24.90"));
        produto.setEstoque(10);
        produto.setAtivo(true);

        Produto salvo = app.getProdutos().salvar(produto);

        List<EventoAuditoria> eventos = hoje();
        assertEquals("PRODUTO_SALVO", eventos.get(0).acao());
        assertEquals("PRODUTO", eventos.get(0).entidade());
        assertEquals(salvo.getId(), eventos.get(0).entidadeId());
        assertEquals("Café Torrado 500g", eventos.get(0).detalhe());
        assertEquals("admin", eventos.get(0).usuarioLogin());
    }

    @Test
    void clienteSalvoRegistraAuditoria() {
        app.getAuth().autenticar("admin", "admin123");
        Cliente cliente = new Cliente();
        cliente.setNome("João da Silva");
        cliente.setCpf("52998224725");
        cliente.setTelefone("");
        cliente.setEmail("");
        cliente.setAtivo(true);

        Cliente salvo = app.getClientes().salvar(cliente);

        List<EventoAuditoria> eventos = hoje();
        assertEquals("CLIENTE_SALVO", eventos.get(0).acao());
        assertEquals(salvo.getId(), eventos.get(0).entidadeId());
        assertEquals("João da Silva", eventos.get(0).detalhe());
    }

    @Test
    void usuarioCriadoRegistraAuditoria() {
        app.getAuth().autenticar("admin", "admin123");

        Usuario criado = app.getUsuarios()
                .criar("Novo Operador", "novo", "senha123", PerfilUsuario.OPERADOR);

        List<EventoAuditoria> eventos = hoje();
        assertEquals("USUARIO_CRIADO", eventos.get(0).acao());
        assertEquals(criado.getId(), eventos.get(0).entidadeId());
        assertEquals("novo", eventos.get(0).detalhe());
    }

    private List<EventoAuditoria> hoje() {
        LocalDate hoje = LocalDate.now();
        return app.getAuditoria().listar(hoje, hoje);
    }
}
