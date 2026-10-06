package com.portfolio.sistemaloja;

import com.portfolio.sistemaloja.db.ConexaoFonte;
import com.portfolio.sistemaloja.repository.CategoriaRepository;
import com.portfolio.sistemaloja.repository.ClienteRepository;
import com.portfolio.sistemaloja.repository.ProdutoRepository;
import com.portfolio.sistemaloja.repository.RelatorioRepository;
import com.portfolio.sistemaloja.repository.UsuarioRepository;
import com.portfolio.sistemaloja.repository.VendaRepository;
import com.portfolio.sistemaloja.service.AuthService;
import com.portfolio.sistemaloja.service.ClienteService;
import com.portfolio.sistemaloja.service.ExportacaoService;
import com.portfolio.sistemaloja.service.ProdutoService;
import com.portfolio.sistemaloja.service.RelatorioService;
import com.portfolio.sistemaloja.service.UsuarioService;
import com.portfolio.sistemaloja.service.VendaService;

public class Aplicacao {

    private final AuthService auth;
    private final ProdutoService produtos;
    private final ClienteService clientes;
    private final VendaService vendas;
    private final UsuarioService usuarios;
    private final RelatorioService relatorios;
    private final ExportacaoService exportacao;
    private final ProdutoRepository produtoRepository;

    public Aplicacao(ConexaoFonte fonte) {
        ProdutoRepository produtoRepository = new ProdutoRepository(fonte);
        CategoriaRepository categoriaRepository = new CategoriaRepository(fonte);
        ClienteRepository clienteRepository = new ClienteRepository(fonte);
        VendaRepository vendaRepository = new VendaRepository(fonte);
        UsuarioRepository usuarioRepository = new UsuarioRepository(fonte);
        RelatorioRepository relatorioRepository = new RelatorioRepository(fonte);

        this.auth = new AuthService(usuarioRepository);
        this.produtos = new ProdutoService(produtoRepository, categoriaRepository);
        this.clientes = new ClienteService(clienteRepository);
        this.vendas = new VendaService(vendaRepository, produtoRepository);
        this.usuarios = new UsuarioService(usuarioRepository);
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

    public ProdutoRepository getProdutoRepository() {
        return produtoRepository;
    }
}
