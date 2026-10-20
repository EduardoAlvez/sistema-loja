package com.portfolio.sistemaloja.impressao;

import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.service.CupomTermicoService;
import com.portfolio.sistemaloja.service.ValidacaoException;

public class CupomEscPosService {

    public static final int CODEPAGE_CP850 = 2;

    private final CupomTermicoService texto;
    private final int moduloQr;

    public CupomEscPosService(CupomTermicoService texto) {
        this(texto, EscPos.MODULO_QR_PADRAO);
    }

    public CupomEscPosService(CupomTermicoService texto, int moduloQr) {
        if (moduloQr < 1 || moduloQr > 16) {
            throw new ValidacaoException("Modulo do QR deve ficar entre 1 e 16.");
        }
        this.texto = texto;
        this.moduloQr = moduloQr;
    }

    public byte[] formatar(Venda venda) {
        EscPos cupom = new EscPos().iniciar().codepage(CODEPAGE_CP850);
        for (String linha : texto.linhas(venda)) {
            cupom.linha(linha);
        }
        String payload = texto.payloadPix(venda);
        if (payload != null) {
            cupom.separador('-');
            cupom.alinhar(Alinhamento.CENTRO).linha("PIX - QR CODE");
            for (String linha : CupomTermicoService.quebrar(payload, EscPos.LARGURA)) {
                cupom.linha(linha);
            }
            cupom.qr(payload, moduloQr);
            cupom.linha("Pague com o app do seu banco");
        }
        if (texto.pagamentoEmDinheiro(venda)) {
            cupom.abrirGaveta();
        }
        cupom.separador('=').cortarParcial();
        return cupom.bytes();
    }
}
