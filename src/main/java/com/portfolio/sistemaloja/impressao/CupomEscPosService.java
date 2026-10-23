package com.portfolio.sistemaloja.impressao;

import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.service.CupomTermicoService;
import com.portfolio.sistemaloja.service.ValidacaoException;

public class CupomEscPosService {

    public static final int CODEPAGE_CP850 = EscPos.PAGINA_CP850;

    private final CupomTermicoService texto;
    private final com.portfolio.sistemaloja.service.ConfiguracaoService configuracoes;
    private Integer codepageFixo;
    private Integer moduloQrFixo;

    public CupomEscPosService(CupomTermicoService texto) {
        this.texto = texto;
        this.configuracoes = null;
        this.codepageFixo = (int) CODEPAGE_CP850;
        this.moduloQrFixo = EscPos.MODULO_QR_PADRAO;
    }

    public CupomEscPosService(CupomTermicoService texto, ImpressoraConfig config) {
        this.texto = texto;
        this.configuracoes = null;
        this.codepageFixo = (int) config.codepage();
        this.moduloQrFixo = config.moduloQr();
    }

    public CupomEscPosService(CupomTermicoService texto, com.portfolio.sistemaloja.service.ConfiguracaoService configuracoes) {
        this.texto = texto;
        this.configuracoes = configuracoes;
    }

    public CupomEscPosService(CupomTermicoService texto, int moduloQr) {
        this.texto = texto;
        this.configuracoes = null;
        this.codepageFixo = (int) CODEPAGE_CP850;
        this.moduloQrFixo = moduloQr;
        validar(moduloQrFixo);
    }

    private void validar(int moduloQr) {
        if (moduloQr < 1 || moduloQr > 16) {
            throw new ValidacaoException("Modulo do QR deve ficar entre 1 e 16.");
        }
    }

    private ImpressoraConfig impressora() {
        if (configuracoes != null) {
            return configuracoes.atual().impressora();
        }
        return new ImpressoraConfig("", codepageFixo == null ? (int) CODEPAGE_CP850 : codepageFixo,
                moduloQrFixo == null ? EscPos.MODULO_QR_PADRAO : moduloQrFixo);
    }

    public byte[] formatar(Venda venda) {
        ImpressoraConfig cfg = impressora();
        validar(cfg.moduloQr());
        EscPos cupom = new EscPos().iniciar().codepage((byte) cfg.codepage());
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
            cupom.qr(payload, cfg.moduloQr());
            cupom.linha("Pague com o app do seu banco");
        }
        if (texto.pagamentoEmDinheiro(venda)) {
            cupom.abrirGaveta();
        }
        cupom.separador('=').cortarParcial();
        return cupom.bytes();
    }
}
