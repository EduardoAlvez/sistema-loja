package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.model.VendaPausada;
import com.portfolio.sistemaloja.repository.VendaPausadaRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class PausaService {

    private static final Logger LOG = LogManager.getLogger(PausaService.class);

    private final VendaPausadaRepository pausadas;
    private final AuditoriaService auditoria;

    public PausaService(VendaPausadaRepository pausadas, AuditoriaService auditoria) {
        this.pausadas = pausadas;
        this.auditoria = auditoria;
    }

    public VendaPausada pausar(Venda venda, String observacao, Usuario usuario) {
        if (venda.getItens().isEmpty()) {
            throw new ValidacaoException("Adicione pelo menos um item antes de pausar a venda.");
        }
        venda.recalcularTotal();
        VendaPausada pausada = new VendaPausada();
        pausada.setClienteId(venda.getClienteId());
        pausada.setUsuarioId(venda.getUsuarioId() != null ? venda.getUsuarioId()
                : usuario == null ? null : usuario.getId());
        pausada.setFormaPagamento(venda.getFormaPagamento());
        pausada.setDesconto(venda.getDesconto());
        pausada.setTotal(venda.getTotal());
        pausada.setPontosResgatados(venda.getPontosResgatados());
        pausada.setValorPontos(venda.getValorPontos());
        pausada.setObservacao(observacao);
        pausada.getItens().addAll(venda.getItens());
        pausada.getPagamentos().addAll(venda.getPagamentos());
        VendaPausada salva = pausadas.salvar(pausada);
        LOG.info("Venda pausada {} com total {}", salva.getId(), salva.getTotal());
        auditoria.registrar("VENDA_PAUSADA", "VENDA_PAUSADA", salva.getId(),
                "total " + salva.getTotal().toPlainString());
        return salva;
    }

    public List<VendaPausada> listar() {
        return pausadas.listar();
    }

    public VendaPausada buscarPorId(long id) {
        return pausadas.buscarPorId(id)
                .orElseThrow(() -> new ValidacaoException("Venda pausada não encontrada."));
    }

    public void descartar(long id) {
        VendaPausada pausada = buscarPorId(id);
        pausadas.remover(id);
        LOG.info("Venda pausada {} descartada", id);
        auditoria.registrar("VENDA_PAUSADA_DESCARTADA", "VENDA_PAUSADA", id,
                "total " + pausada.getTotal().toPlainString());
    }
}
