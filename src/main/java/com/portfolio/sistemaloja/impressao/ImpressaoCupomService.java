package com.portfolio.sistemaloja.impressao;

import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.service.ValidacaoException;

import java.util.List;

public class ImpressaoCupomService {

    private final CupomEscPosService cupom;
    private final Impressoras impressoras;

    public ImpressaoCupomService(CupomEscPosService cupom, Impressoras impressoras) {
        this.cupom = cupom;
        this.impressoras = impressoras;
    }

    public List<String> impressoras() {
        return impressoras.nomes();
    }

    public void imprimir(Venda venda, String nomeImpressora) {
        exigirVenda(venda);
        ImpressoraCupom impressora = impressoras.selecionar(nomeImpressora);
        impressora.imprimir(cupom.formatar(venda));
    }

    public byte[] bytes(Venda venda) {
        exigirVenda(venda);
        return cupom.formatar(venda);
    }

    private static void exigirVenda(Venda venda) {
        if (venda == null) {
            throw new ValidacaoException("Informe a venda para imprimir o cupom.");
        }
    }
}
