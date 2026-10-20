package com.portfolio.sistemaloja.impressao;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ImpressoraMemoria implements ImpressoraCupom {

    private final String nome;
    private final List<byte[]> trabalhos = new ArrayList<>();

    public ImpressoraMemoria() {
        this("Impressora de memoria");
    }

    public ImpressoraMemoria(String nome) {
        this.nome = Objects.requireNonNull(nome, "nome");
    }

    @Override
    public String nome() {
        return nome;
    }

    @Override
    public void imprimir(byte[] dados) {
        Objects.requireNonNull(dados, "dados");
        trabalhos.add(dados.clone());
    }

    public int total() {
        return trabalhos.size();
    }

    public byte[] ultimoTrabalho() {
        if (trabalhos.isEmpty()) {
            throw new IllegalStateException("Nenhum trabalho impresso.");
        }
        return trabalhos.get(trabalhos.size() - 1).clone();
    }

    public List<byte[]> trabalhos() {
        List<byte[]> copia = new ArrayList<>();
        for (byte[] trabalho : trabalhos) {
            copia.add(trabalho.clone());
        }
        return List.copyOf(copia);
    }
}
