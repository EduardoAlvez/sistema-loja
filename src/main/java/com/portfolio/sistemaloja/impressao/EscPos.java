package com.portfolio.sistemaloja.impressao;

import com.portfolio.sistemaloja.service.QrAscii;
import com.portfolio.sistemaloja.service.ValidacaoException;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Map;

public class EscPos {

    public static final int LARGURA = 48;
    public static final int MODULO_QR_PADRAO = 4;

    public static final int PAGINA_CP850 = 2;
    public static final int PAGINA_CP1252 = 16;

    private static final Map<Integer, Charset> PAGINAS = Map.of(
            PAGINA_CP850, Charset.forName("Cp850"),
            PAGINA_CP1252, Charset.forName("windows-1252"));
    private static final int LARGURA_MAXIMA_DOTS = 576;
    private static final int QUIET_ZONE_MODULOS = 8;
    private static final int MAXIMO_BYTES_QR = 7089;

    private final ByteArrayOutputStream saida = new ByteArrayOutputStream();
    private Charset charset = PAGINAS.get(PAGINA_CP850);

    public EscPos iniciar() {
        return comando(0x1B, 0x40);
    }

    public EscPos codepage(int pagina) {
        Charset selecionada = PAGINAS.get(pagina);
        if (selecionada == null) {
            throw new ValidacaoException("Pagina de codigo " + pagina + " nao suportada. Use 2 (Cp850) ou 16 (Cp1252).");
        }
        charset = selecionada;
        return comando(0x1B, 0x74, pagina);
    }

    public EscPos alinhar(Alinhamento alinhamento) {
        if (alinhamento == null) {
            throw new ValidacaoException("Informe o alinhamento do texto.");
        }
        return comando(0x1B, 0x61, alinhamento.comando());
    }

    public EscPos negrito(boolean ativo) {
        return comando(0x1B, 0x45, ativo ? 1 : 0);
    }

    public EscPos tamanho(int largura, int altura) {
        if (largura < 1 || largura > 8 || altura < 1 || altura > 8) {
            throw new ValidacaoException("Tamanho de fonte deve ficar entre 1 e 8.");
        }
        return comando(0x1D, 0x21, ((largura - 1) << 4) | (altura - 1));
    }

    public EscPos texto(String texto) {
        if (texto == null) {
            throw new ValidacaoException("Texto nao pode ser nulo.");
        }
        saida.writeBytes(codificar(texto));
        return this;
    }

    public EscPos linha(String texto) {
        return texto(texto).comando(0x0A);
    }

    public EscPos separador(char caractere) {
        return linha(String.valueOf(caractere).repeat(LARGURA));
    }

    public EscPos pularLinhas(int linhas) {
        if (linhas < 0 || linhas > 255) {
            throw new ValidacaoException("Quantidade de linhas deve ficar entre 0 e 255.");
        }
        return comando(0x1B, 0x64, linhas);
    }

    public EscPos qr(String conteudo) {
        return qr(conteudo, MODULO_QR_PADRAO);
    }

    public EscPos qr(String conteudo, int modulo) {
        if (conteudo == null || conteudo.isBlank()) {
            throw new ValidacaoException("Conteudo vazio para gerar o QR Code.");
        }
        if (modulo < 1 || modulo > 16) {
            throw new ValidacaoException("Modulo do QR deve ficar entre 1 e 16.");
        }
        int modulos = QrAscii.modulos(conteudo);
        if ((modulos + QUIET_ZONE_MODULOS) * modulo > LARGURA_MAXIMA_DOTS) {
            throw new ValidacaoException("QR Code nao cabe na bobina de 80mm com modulo " + modulo + ".");
        }
        byte[] dados = conteudo.getBytes(StandardCharsets.UTF_8);
        if (dados.length > MAXIMO_BYTES_QR) {
            throw new ValidacaoException("Conteudo do QR excede o limite de " + MAXIMO_BYTES_QR + " bytes.");
        }
        int tamanho = dados.length + 3;
        comando(0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00);
        comando(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, modulo);
        comando(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 0x31);
        comando(0x1D, 0x28, 0x6B, tamanho & 0xFF, (tamanho >> 8) & 0xFF, 0x31, 0x50, 0x30);
        saida.writeBytes(dados);
        comando(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30);
        return this;
    }

    public EscPos cortarParcial() {
        return comando(0x0A).comando(0x1B, 0x64, 0x03).comando(0x1D, 0x56, 0x01);
    }

    public EscPos abrirGaveta() {
        return comando(0x1B, 0x70, 0x00, 0x32, 0xFA);
    }

    public byte[] bytes() {
        return saida.toByteArray();
    }

    private EscPos comando(int... valores) {
        for (int valor : valores) {
            saida.write(valor);
        }
        return this;
    }

    private byte[] codificar(String texto) {
        if (charset.newEncoder().canEncode(texto)) {
            return texto.getBytes(charset);
        }
        StringBuilder resultado = new StringBuilder(texto.length());
        for (int i = 0; i < texto.length(); ) {
            int codigo = texto.codePointAt(i);
            String caractere = new String(Character.toChars(codigo));
            if (charset.newEncoder().canEncode(caractere)) {
                resultado.append(caractere);
            } else {
                String semAcento = Normalizer.normalize(caractere, Normalizer.Form.NFD)
                        .replaceAll("\\p{M}", "");
                if (!semAcento.isEmpty() && charset.newEncoder().canEncode(semAcento)) {
                    resultado.append(semAcento);
                } else {
                    resultado.append('?');
                }
            }
            i += Character.charCount(codigo);
        }
        return resultado.toString().getBytes(charset);
    }
}
