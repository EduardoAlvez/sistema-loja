package com.portfolio.sistemaloja;

import com.portfolio.sistemaloja.db.ConexaoFonte;
import com.portfolio.sistemaloja.impressao.CupomEscPosService;
import com.portfolio.sistemaloja.impressao.ImpressaoCupomService;
import com.portfolio.sistemaloja.impressao.ImpressorasSpool;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.repository.AuditoriaRepository;
import com.portfolio.sistemaloja.repository.CaixaRepository;
import com.portfolio.sistemaloja.repository.CategoriaRepository;
import com.portfolio.sistemaloja.repository.ClienteRepository;
import com.portfolio.sistemaloja.repository.ConfiguracaoRepository;
import com.portfolio.sistemaloja.repository.EntradaMercadoriaRepository;
import com.portfolio.sistemaloja.repository.FornecedorRepository;
import com.portfolio.sistemaloja.repository.FidelidadeRepository;
import com.portfolio.sistemaloja.repository.ProdutoRepository;
import com.portfolio.sistemaloja.repository.PromocaoProdutoRepository;
import com.portfolio.sistemaloja.repository.RelatorioRepository;
import com.portfolio.sistemaloja.repository.RecebimentoFiadoRepository;
import com.portfolio.sistemaloja.repository.UsuarioRepository;
import com.portfolio.sistemaloja.repository.VendaPausadaRepository;
import com.portfolio.sistemaloja.repository.VendaRepository;
import com.portfolio.sistemaloja.service.AuthService;
import com.portfolio.sistemaloja.service.AutorizacaoService;
import com.portfolio.sistemaloja.service.AuditoriaService;
import com.portfolio.sistemaloja.service.CaixaService;
import com.portfolio.sistemaloja.service.ClienteService;
import com.portfolio.sistemaloja.service.ConfiguracaoService;
import com.portfolio.sistemaloja.service.CupomService;
import com.portfolio.sistemaloja.service.CupomTermicoService;
import com.portfolio.sistemaloja.service.DevolucaoService;
import com.portfolio.sistemaloja.service.ExportacaoService;
import com.portfolio.sistemaloja.service.CompraService;
import com.portfolio.sistemaloja.service.FornecedorService;
import com.portfolio.sistemaloja.service.FiadoService;
import com.portfolio.sistemaloja.service.FidelidadeService;
import com.portfolio.sistemaloja.service.PausaService;
import com.portfolio.sistemaloja.service.ProdutoService;
import com.portfolio.sistemaloja.service.PromocaoService;
import com.portfolio.sistemaloja.service.RelatorioService;
import com.portfolio.sistemaloja.service.UsuarioService;
import com.portfolio.sistemaloja.service.VendaService;

import java.time.Clock;

public class Aplicacao {

    private final Sessao sessao = new Sessao();
    private final AuthService auth;
    private final ProdutoService produtos;
    private final ClienteService clientes;
    private final VendaService vendas;
    private final CaixaService caixa;
    private final PausaService pausas;
    private final CupomService cupom;
    private final DevolucaoService devolucoes;
    private final CupomTermicoService cupomTermico;
    private final ImpressaoCupomService impressao;
    private final UsuarioService usuarios;
    private final RelatorioService relatorios;
    private final ExportacaoService exportacao;
    private final AuditoriaService auditoria;
    private final ProdutoRepository produtoRepository;
    private final FidelidadeService fidelidade;
    private final FidelidadeRepository fidelidadeRepository;
    private final DevolucaoRepository devolucaoRepository;
    private final ConfiguracaoService configuracoes;
    private final AutorizacaoService autorizacoes;
    private final FiadoService fiados;
    private final PromocaoService promocoes;
    private final FornecedorService fornecedores;
    private final CompraService compras;

    public Aplicacao(ConexaoFonte fonte) {
        this(fonte, Clock.systemDefaultZone());
    }

    public Aplicacao(ConexaoFonte fonte, Clock relogio) {
        ProdutoRepository produtoRepository = new ProdutoRepository(fonte);
        CategoriaRepository categoriaRepository = new CategoriaRepository(fonte);
        ClienteRepository clienteRepository = new ClienteRepository(fonte);
        VendaRepository vendaRepository = new VendaRepository(fonte);
        CaixaRepository caixaRepository = new CaixaRepository(fonte);
        VendaPausadaRepository vendaPausadaRepository = new VendaPausadaRepository(fonte);
        UsuarioRepository usuarioRepository = new UsuarioRepository(fonte);
        RelatorioRepository relatorioRepository = new RelatorioRepository(fonte);
        AuditoriaRepository auditoriaRepository = new AuditoriaRepository(fonte);
        ConfiguracaoRepository configuracaoRepository = new ConfiguracaoRepository(fonte);

        this.auditoria = new AuditoriaService(auditoriaRepository, sessao);
        this.auth = new AuthService(usuarioRepository, sessao, auditoria);
        this.produtos = new ProdutoService(produtoRepository, categoriaRepository, auditoria);
        this.clientes = new ClienteService(clienteRepository, auditoria);
        this.fidelidadeRepository = new FidelidadeRepository(fonte);
        this.configuracoes = new ConfiguracaoService(configuracaoRepository, sessao, fonte);
        this.autorizacoes = new AutorizacaoService(usuarioRepository, sessao, auditoria);
        this.fiados = new FiadoService(new FiadoRepository(fonte), new RecebimentoFiadoRepository(fonte),
                clienteRepository, configuracaoRepository, autorizacoes, auditoria, sessao);
        this.fidelidade = new FidelidadeService(fidelidadeRepository, this.configuracoes);
        this.promocoes = new PromocaoService(new PromocaoProdutoRepository(fonte), produtoRepository,
                auditoria, relogio);
        this.fornecedores = new FornecedorService(new FornecedorRepository(fonte), auditoria);
        this.compras = new CompraService(new EntradaMercadoriaRepository(fonte, produtoRepository), auditoria);
        this.devolucaoRepository = new DevolucaoRepository(fonte, fidelidade);
        this.vendas = new VendaService(vendaRepository, produtoRepository, usuarioRepository,
                auditoria, relogio, fidelidade, autorizacoes, this.promocoes);
        this.caixa = new CaixaService(caixaRepository, vendaRepository, auditoria, autorizacoes);
        this.pausas = new PausaService(vendaPausadaRepository, auditoria);
        this.cupom = new CupomService();
        this.devolucoes = new DevolucaoService(devolucaoRepository, vendaRepository, auditoria);
        this.cupomTermico = new CupomTermicoService(this.configuracoes);
        this.impressao = new ImpressaoCupomService(
                new CupomEscPosService(this.cupomTermico, this.configuracoes),
                new ImpressorasSpool());
        this.usuarios = new UsuarioService(usuarioRepository, auditoria);
        this.relatorios = new RelatorioService(vendaRepository, relatorioRepository);
        this.exportacao = new ExportacaoService();
        this.produtoRepository = produtoRepository;
    }

    public AutorizacaoService getAutorizacoes() {
        return autorizacoes;
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

    public CaixaService getCaixa() {
        return caixa;
    }

    public PausaService getPausas() {
        return pausas;
    }

    public CupomService getCupom() {
        return cupom;
    }

    public DevolucaoService getDevolucoes() {
        return devolucoes;
    }

    public CupomTermicoService getCupomTermico() {
        return cupomTermico;
    }

    public ImpressaoCupomService getImpressao() {
        return impressao;
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

    public FidelidadeService getFidelidade() {
        return fidelidade;
    }

    public FidelidadeRepository getFidelidadeRepository() {
        return fidelidadeRepository;
    }

    public DevolucaoRepository getDevolucaoRepository() {
        return devolucaoRepository;
    }

    public ConfiguracaoService getConfiguracoes() {
        return configuracoes;
    }

    public FiadoService getFiados() {
        return fiados;
    }

    public PromocaoService getPromocoes() {
        return promocoes;
    }

    public FornecedorService getFornecedores() {
        return fornecedores;
    }

    public CompraService getCompras() {
        return compras;
    }
}
