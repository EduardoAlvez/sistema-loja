package com.portfolio.sistemaloja.tef;

import com.portfolio.sistemaloja.tef.simulado.OperadoraCartaoSimulada;

/**
 * Factory para obter a implementação ativa de {@link OperadoraCartao}.
 * <p>
 * Por padrão utiliza {@link com.portfolio.sistemaloja.tef.simulado.OperadoraCartaoSimulada}.
 * Em produção pode ser configurada para usar adaptador real via {@link #configurar(OperadoraCartao)}.
 * </p>
 */
public class OperadoraCartaoFactory {

    private static final OperadoraCartaoFactory INSTANCIA = new OperadoraCartaoFactory();
    private OperadoraCartao operadora;

    private OperadoraCartaoFactory() {
        this.operadora = new OperadoraCartaoSimulada();
    }

    public static OperadoraCartaoFactory getInstance() {
        return INSTANCIA;
    }

    public OperadoraCartao getOperadora() {
        return operadora;
    }

    public void configurar(OperadoraCartao operadora) {
        if (operadora != null) {
            this.operadora = operadora;
        }
    }
}
