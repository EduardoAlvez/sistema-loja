package com.portfolio.sistemaloja.impressao;

import com.portfolio.sistemaloja.service.ValidacaoException;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EscPosTest {

    private static final Charset CP850 = Charset.forName("Cp850");

    @Test
    void iniciarEnviaEscArroba() {
        assertEquals("1b40", hex(new EscPos().iniciar().bytes()));
    }

    @Test
    void selecionaCodepageCp850() {
        assertEquals("1b7402", hex(new EscPos().codepage(2).bytes()));
        assertThrows(ValidacaoException.class, () -> new EscPos().codepage(-1));
        assertThrows(ValidacaoException.class, () -> new EscPos().codepage(3));
    }

    @Test
    void selecionaCodepageWindows1252() {
        assertEquals("1b741080", hex(new EscPos().codepage(16).texto("€").bytes()));
    }

    @Test
    void alinhaTexto() {
        assertEquals("1b6100", hex(new EscPos().alinhar(Alinhamento.ESQUERDA).bytes()));
        assertEquals("1b6101", hex(new EscPos().alinhar(Alinhamento.CENTRO).bytes()));
        assertEquals("1b6102", hex(new EscPos().alinhar(Alinhamento.DIREITA).bytes()));
        assertThrows(ValidacaoException.class, () -> new EscPos().alinhar(null));
    }

    @Test
    void ligaEDesligaNegrito() {
        assertEquals("1b4501", hex(new EscPos().negrito(true).bytes()));
        assertEquals("1b4500", hex(new EscPos().negrito(false).bytes()));
    }

    @Test
    void defineTamanhoDaFonte() {
        assertEquals("1d2100", hex(new EscPos().tamanho(1, 1).bytes()));
        assertEquals("1d2111", hex(new EscPos().tamanho(2, 2).bytes()));
        assertEquals("1d2130", hex(new EscPos().tamanho(4, 1).bytes()));
        assertThrows(ValidacaoException.class, () -> new EscPos().tamanho(0, 1));
        assertThrows(ValidacaoException.class, () -> new EscPos().tamanho(1, 9));
    }

    @Test
    void textoUsaCp850EMantemAcentos() {
        byte[] bytes = new EscPos().texto("Água").bytes();

        assertArrayEquals("Água".getBytes(CP850), bytes);
        assertEquals("Água", new String(bytes, CP850));
        assertEquals((byte) 0xB5, bytes[0]);
    }

    @Test
    void caractereForaDoCp850ViraInterrogacao() {
        String texto = new String(new EscPos().texto("Preço € 10").bytes(), CP850);

        assertEquals("Preço ? 10", texto);
    }

    @Test
    void linhaAcrescentaQuebraDeLinha() {
        assertEquals("4f690a", hex(new EscPos().linha("Oi").bytes()));
    }

    @Test
    void separadorOcupaALarguraDoCupom() {
        byte[] bytes = new EscPos().separador('=').bytes();

        assertEquals(49, bytes.length);
        assertEquals("=".repeat(48) + "\n", new String(bytes, CP850));
    }

    @Test
    void pulaLinhas() {
        assertEquals("1b6403", hex(new EscPos().pularLinhas(3).bytes()));
        assertThrows(ValidacaoException.class, () -> new EscPos().pularLinhas(-1));
        assertThrows(ValidacaoException.class, () -> new EscPos().pularLinhas(256));
    }

    @Test
    void qrUsaModeloDoisEccMeTamanhoDoConteudo() {
        String payload = "PIX-123456";
        byte[] bytes = new EscPos().qr(payload).bytes();

        String esperado = "1d286b040031413200"
                + "1d286b0300314304"
                + "1d286b0300314531"
                + "1d286b0d00315030"
                + hex(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                + "1d286b0300315130";
        assertEquals(esperado, hex(bytes));
    }

    @Test
    void qrCalculaTamanhoGrandeEmDoisBytes() {
        String payload = "A".repeat(300);
        String bytes = hex(new EscPos().qr(payload).bytes());

        assertTrue(bytes.contains("1d286b2f01315030" + "41".repeat(300)), bytes);
    }

    @Test
    void qrRecusaConteudoInvalidoOuLargoDemais() {
        assertThrows(ValidacaoException.class, () -> new EscPos().qr(" "));
        assertThrows(ValidacaoException.class, () -> new EscPos().qr("PIX", 0));
        assertThrows(ValidacaoException.class, () -> new EscPos().qr("PIX", 17));
        assertThrows(ValidacaoException.class, () -> new EscPos().qr("A".repeat(300), 16));
    }

    @Test
    void cortaParcialDepoisDeAvancarOPapel() {
        assertEquals("0a1b64031d5601", hex(new EscPos().cortarParcial().bytes()));
    }

    @Test
    void abreAGaveta() {
        assertEquals("1b700032fa", hex(new EscPos().abrirGaveta().bytes()));
    }

    private static String hex(byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }
}
