package com.portfolio.sistemaloja.model;

import com.portfolio.sistemaloja.impressao.ImpressoraConfig;
import com.portfolio.sistemaloja.service.ConfiguracaoPix;
import com.portfolio.sistemaloja.service.FidelidadeConfig;

public record ConfiguracoesAplicacao(FidelidadeConfig fidelidade, ConfiguracaoPix pix,
                                     ImpressoraConfig impressora) {

    public ConfiguracoesAplicacao withFidelidade(FidelidadeConfig nova) {
        return new ConfiguracoesAplicacao(nova, pix, impressora);
    }

    public ConfiguracoesAplicacao withPix(ConfiguracaoPix nova) {
        return new ConfiguracoesAplicacao(fidelidade, nova, impressora);
    }

    public ConfiguracoesAplicacao withImpressora(ImpressoraConfig nova) {
        return new ConfiguracoesAplicacao(fidelidade, pix, nova);
    }
}
