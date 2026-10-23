package com.portfolio.sistemaloja.model;

import com.portfolio.sistemaloja.impressao.ImpressoraConfig;
import com.portfolio.sistemaloja.service.ConfiguracaoPix;
import com.portfolio.sistemaloja.service.FidelidadeConfig;

public record ConfiguracoesAplicacao(FidelidadeConfig fidelidade, ConfiguracaoPix pix,
                                     ImpressoraConfig impressora) {
}
