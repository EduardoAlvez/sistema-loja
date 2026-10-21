package com.portfolio.sistemaloja;

import com.portfolio.sistemaloja.db.ConexaoFonte;
import com.portfolio.sistemaloja.impressao.CupomEscPosService;
import com.portfolio.sistemaloja.impressao.ImpressaoCupomService;
import com.portfolio.sistemaloja.impressao.ImpressoraConfig;
import com.portfolio.sistemaloja.impressao.ImpressorasSpool;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.repository.AuditoriaRepository;
import com.portfolio.sistemaloja.repository.CaixaRepository;
import com.portfolio.sistemaloja.repository.CategoriaRepository;
import com.portfolio.sistemaloja.repository.ClienteRepository;
import com.portfolio.sistemaloja.repository.DevolucaoRepository;
import com.portfolio.sistemaloja.repository.FidelidadeRepository;
import com.portfolio.sistemaloja.repository.ProdutoRepository;
import com.portfolio.sistemaloja.repository.RelatorioRepository;
import com.portfolio.sistemaloja.repository.UsuarioRepository;
import com.portfolio.sistemaloja.repository.VendaPausadaRepository;
import com.portfolio.sistemaloja.repository.VendaRepository;
import com.portfolio.sistemaloja.service.AuthService;
import com.portfolio.sistemaloja.service.AuditoriaService;
import com.portfolio.sistemaloja.service.CaixaService;
import com.portfolio.sistemaloja.service.ClienteService;
import com.portfolio.sistemaloja.service.ConfiguracaoPix;
import com.portfolio.sistemaloja.service.CupomService;
import com.portfolio.sistemaloja.service.CupomTermicoService;
import com.portfolio.sistemaloja.service.DevolucaoService;
import com.portfolio.sistemaloja.service.ExportacaoService;
import com.portfolio.sistemaloja.service.FidelidadeConfig;
import com.portfolio.sistemaloja.service.FidelidadeService;
import com.portfolio.sistemaloja.service.PausaService;
import com.portfolio.sistemaloja.service.ProdutoService;
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
        DevolucaoRepository devolucaoRepository = new DevolucaoRepository(fonte);
        UsuarioRepository usuarioRepository = new UsuarioRepository(fonte);
        RelatorioRepository relatorioRepository = new RelatorioRepository(fonte);
        AuditoriaRepository auditoriaRepository = new AuditoriaRepository(fonte);

        this.auditoria = new AuditoriaService(auditoriaRepository, sessao);
        this.auth = new AuthService(usuarioRepository, sessao, auditoria);
        this.produtos = new ProdutoService(produtoRepository, categoriaRepository, auditoria);
        this.clientes = new ClienteService(clienteRepository, auditoria);
        this.fidelidadeRepository = new FidelidadeRepository(fonte);
        this.fidelidade = new FidelidadeService(fidelidadeRepository, FidelidadeConfig.padrao());
        this.vendas = new VendaService(vendaRepository, produtoRepository, usuarioRepository,
                auditoria, relogio, fidelidade);
        this.caixa = new CaixaService(caixaRepository, vendaRepository, auditoria);
        this.pausas = new PausaService(vendaPausadaRepository, auditoria);
        this.cupom = new CupomService();
        this.devolucoes = new DevolucaoService(devolucaoRepository, vendaRepository, auditoria);
        this.cupomTermico = new CupomTermicoService(ConfiguracaoPix.padrao());
        ImpressoraConfig impressoraConfig = ImpressoraConfig.padrao();
        this.impressao = new ImpressaoCupomService(
                new CupomEscPosService(this.cupomTermico, impressoraConfig),
                new ImpressorasSpool());
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
}
