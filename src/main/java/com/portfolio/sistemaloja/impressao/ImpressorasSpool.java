package com.portfolio.sistemaloja.impressao;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class ImpressorasSpool implements Impressoras {

    private final Supplier<PrintService[]> servicos;

    public ImpressorasSpool() {
        this(() -> PrintServiceLookup.lookupPrintServices(null, null));
    }

    ImpressorasSpool(Supplier<PrintService[]> servicos) {
        this.servicos = servicos;
    }

    @Override
    public List<String> nomes() {
        List<String> nomes = new ArrayList<>();
        for (PrintService servico : servicos.get()) {
            nomes.add(servico.getName());
        }
        Collections.sort(nomes, String.CASE_INSENSITIVE_ORDER);
        return nomes;
    }

    @Override
    public ImpressoraCupom selecionar(String nome) {
        PrintService[] disponiveis = servicos.get();
        List<String> nomes = new ArrayList<>();
        for (PrintService servico : disponiveis) {
            nomes.add(servico.getName());
        }
        String escolhido = escolher(nomes, nome);
        for (PrintService servico : disponiveis) {
            if (servico.getName().equals(escolhido)) {
                return new ImpressoraSpool(servico);
            }
        }
        throw new ImpressaoException("Impressora '" + escolhido + "' nao esta mais disponivel.");
    }

    static String escolher(List<String> nomes, String preferido) {
        if (nomes.isEmpty()) {
            throw new ImpressaoException("Nenhuma impressora instalada. Salve o cupom em .bin ou instale uma fila de impressao.");
        }
        if (preferido == null || preferido.isBlank()) {
            return nomes.get(0);
        }
        String alvo = preferido.trim();
        for (String nome : nomes) {
            if (nome.equalsIgnoreCase(alvo)) {
                return nome;
            }
        }
        throw new ImpressaoException("Impressora '" + alvo + "' nao encontrada. Instaladas: "
                + String.join(", ", nomes) + ".");
    }
}
