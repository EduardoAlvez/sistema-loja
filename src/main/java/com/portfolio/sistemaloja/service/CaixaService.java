package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.model.Caixa;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.repository.CaixaRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.Optional;

public class CaixaService {

    private static final Logger LOG = LogManager.getLogger(CaixaService.class);

    private final CaixaRepository caixas;
    private final AuditoriaService auditoria;

    public CaixaService(CaixaRepository caixas, AuditoriaService auditoria) {
        this.caixas = caixas;
        this.auditoria = auditoria;
    }

    public Caixa abrir(BigDecimal valorAbertura, Usuario usuario) {
        if (usuario == null) {
            throw new ValidacaoException("É necessário estar autenticado para abrir o caixa.");
        }
        if (caixas.buscarAberto().isPresent()) {
            throw new ValidacaoException("Já existe um caixa aberto. Feche-o antes de abrir outro.");
        }
        BigDecimal valor = valorAbertura == null ? BigDecimal.ZERO : valorAbertura;
        if (valor.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidacaoException("O valor de abertura não pode ser negativo.");
        }
        Caixa caixa = new Caixa();
        caixa.setUsuarioAberturaId(usuario.getId());
        caixa.setValorAbertura(valor);
        Caixa aberto = caixas.abrir(caixa);
        LOG.info("Caixa {} aberto por {} com valor {}", aberto.getId(), usuario.getLogin(), valor);
        auditoria.registrar("CAIXA_ABERTO", "CAIXA", aberto.getId(),
                "valor abertura " + valor.toPlainString());
        return aberto;
    }

    public Optional<Caixa> caixaAberto() {
        return caixas.buscarAberto();
    }

    public Caixa buscarPorId(long id) {
        return caixas.buscarPorId(id)
                .orElseThrow(() -> new ValidacaoException("Caixa não encontrado."));
    }
}
