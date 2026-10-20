package com.portfolio.sistemaloja.impressao;

import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.SimpleDoc;
import javax.print.attribute.HashPrintRequestAttributeSet;
import java.io.ByteArrayInputStream;
import java.util.Objects;

public class ImpressoraSpool implements ImpressoraCupom {

    private final PrintService servico;

    public ImpressoraSpool(PrintService servico) {
        this.servico = Objects.requireNonNull(servico, "servico");
    }

    @Override
    public String nome() {
        return servico.getName();
    }

    @Override
    public void imprimir(byte[] dados) {
        Objects.requireNonNull(dados, "dados");
        DocFlavor formato;
        Object conteudo;
        if (servico.isDocFlavorSupported(DocFlavor.BYTE_ARRAY.AUTOSENSE)) {
            formato = DocFlavor.BYTE_ARRAY.AUTOSENSE;
            conteudo = dados;
        } else if (servico.isDocFlavorSupported(DocFlavor.INPUT_STREAM.AUTOSENSE)) {
            formato = DocFlavor.INPUT_STREAM.AUTOSENSE;
            conteudo = new ByteArrayInputStream(dados);
        } else {
            throw new ImpressaoException("A impressora " + nome()
                    + " nao aceita impressao direta. Salve o cupom em .bin e envie com copy /b.");
        }
        DocPrintJob trabalho = servico.createPrintJob();
        try {
            trabalho.print(new SimpleDoc(conteudo, formato, null), new HashPrintRequestAttributeSet());
        } catch (PrintException e) {
            throw new ImpressaoException("Falha ao imprimir em " + nome() + ": " + e.getMessage(), e);
        }
    }
}
