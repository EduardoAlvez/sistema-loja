package com.portfolio.sistemaloja.impressao;

import java.util.List;

public interface Impressoras {

    List<String> nomes();

    ImpressoraCupom selecionar(String nome);
}
