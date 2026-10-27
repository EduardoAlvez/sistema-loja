package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.model.EntradaItem;
import com.portfolio.sistemaloja.model.EntradaMercadoria;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.repository.EntradaMercadoriaRepository;

import java.time.LocalDate;
import java.util.List;

public class CompraService {

    private final EntradaMercadoriaRepository entradas;
    private final AuditoriaService auditoria;

    public CompraService(EntradaMercadoriaRepository entradas, AuditoriaService auditoria) {
        this.entradas = entradas;
        this.auditoria = auditoria;
    }

    public List<EntradaMercadoria> listar(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null) {
            throw new ValidacaoException("Informe o período para consultar as entradas.");
        }
        if (inicio.isAfter(fim)) {
            throw new ValidacaoException("A data inicial deve ser anterior à final.");
        }
        return entradas.listar(inicio, fim);
    }

    public EntradaMercadoria buscarPorId(long id) {
        EntradaMercadoria entrada = entradas.buscarPorId(id);
        if (entrada == null) {
            throw new ValidacaoException("Entrada de mercadoria não encontrada.");
        }
        return entrada;
    }

    public EntradaMercadoria registrar(EntradaMercadoria entrada, Usuario usuario) {
        if (entrada == null) {
            throw new ValidacaoException("Entrada de mercadoria inválida.");
        }
        if (entrada.getItens() == null || entrada.getItens().isEmpty()) {
            throw new ValidacaoException("Adicione pelo menos um item à entrada.");
        }
        for (EntradaItem item : entrada.getItens()) {
            if (item.getProdutoId() == null) {
                throw new ValidacaoException("Produto inválido na entrada.");
            }
            if (item.getQuantidade() <= 0) {
                throw new ValidacaoException("Quantidade deve ser maior que zero.");
            }
            if (item.getValorUnitario() == null || item.getValorUnitario().signum() <= 0) {
                throw new ValidacaoException("Valor unitário deve ser maior que zero.");
            }
        }
        if (usuario != null) {
            entrada.setUsuarioId(usuario.getId());
            entrada.setUsuarioNome(usuario.getNome());
        }
        EntradaMercadoria salvo = entradas.registrar(entrada);
        auditoria.registrar("ENTRADA_MERCADORIA_REGISTRADA", "ENTRADA_MERCADORIA", salvo.getId(),
                "Entrada registrada com " + salvo.getItens().size() + " item(ns)");
        return salvo;
    }
}
