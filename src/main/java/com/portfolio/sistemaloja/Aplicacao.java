package com.portfolio.sistemaloja;

import com.portfolio.sistemaloja.db.ConexaoFonte;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.repository.AuditoriaRepository;
import com.portfolio.sistemaloja.repository.CategoriaRepository;
import com.portfolio.sistemaloja.repository.ClienteRepository;
import com.portfolio.sistemaloja.repository.ProdutoRepository;
import com.portfolio.sistemaloja.repository.RelatorioRepository;
import com.portfolio.sistemaloja.repository.UsuarioRepository;
import com.portfolio.sistemaloja.repository.VendaRepository;
import com.portfolio.sistemaloja.service.AuthService;
import com.portfolio.sistemaloja.service.AuditoriaService;
import com.portfolio.sistemaloja.service.ClienteService;
import com.portfolio.sistemaloja.service.ExportacaoService;
import com.portfolio.sistemaloja.service.ProdutoService;
import com.portfolio.sistemaloja.service.RelatorioService;
import com.portfolio.sistemaloja.service.UsuarioService;
import com.portfolio.sistemaloja.service.VendaService;

public class Aplicacao {

    private final Sessao sessao = new Sessao();
    private final AuthService auth;
    private final ProdutoService produtos;
    private final ClienteService clientes;
    private final VendaService vendas;
    private final UsuarioService usuarios;
    private final RelatorioService relatorios;
    private final ExportacaoService exportacao;
    private final AuditoriaService auditoria;
    private final ProdutoRepository produtoRepository;

    public Aplicacao(ConexaoFonte fonte) {
        ProdutoRepository produtoRepository = new ProdutoRepository(fonte);
        CategoriaRepository categoriaRepository = new CategoriaRepository(fonte);
        ClienteRepository clienteRepository = new ClienteRepository(fonte);
        VendaRepository vendaRepository = new VendaRepository(fonte);
        UsuarioRepository usuarioRepository = new UsuarioRepository(fonte);
        RelatorioRepository relatorioRepository = new RelatorioRepository(fonte);
        AuditoriaRepository auditoriaRepository = new AuditoriaRepository(fonte);

        this.auditoria = new AuditoriaService(auditoriaRepository, sessao);
        this.auth = new AuthService(usuarioRepository, sessao, auditoria);
        this.produtos = new ProdutoService(produtoRepository, categoriaRepository, auditoria);
        this.clientes = new ClienteService(clienteRepository, auditoria);
        this.vendas = new VendaService(vendaRepository, produtoRepository, auditoria);
        this.usuarios = new UsuarioService(usuarioRepository, auditoria);
        this.relatorios = new RelatorioService(vendaRepository, relatorioRepository);
        this.exportacao = new ExportacaoService();
        this.produtoRepository = produtoRepository;
    }

    public AuthService getAuth() {
        return auth;
    }

    public ProdutoService getProdutos() {
        return produtos;
    }

    public ClienteService getClientes() {
        return clientes;
    }

    public VendaService getVendas() {
        return vendas;
    }

    public UsuarioService getUsuarios() {
        return usuarios;
    }

    public RelatorioService getRelatorios() {
        return relatorios;
    }

    public ExportacaoService getExportacao() {
        return exportacao;
    }

    public AuditoriaService getAuditoria() {
        return auditoria;
    }

    public Usuario getUsuarioAtual() {
        return sessao.getAtual();
    }

    public ProdutoRepository getProdutoRepository() {
        return produtoRepository;
    }
}
