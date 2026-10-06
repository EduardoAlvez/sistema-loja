package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.model.Categoria;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.repository.CategoriaRepository;
import com.portfolio.sistemaloja.repository.ProdutoRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProdutoService {

    static final String SEPARADOR_CSV = ";";
    static final String CABECALHO_CSV =
            "codigo_barras;nome;categoria;preco_custo;preco_venda;estoque;ativo";

    private final ProdutoRepository produtos;
    private final CategoriaRepository categorias;
    private final AuditoriaService auditoria;

    public ProdutoService(ProdutoRepository produtos, CategoriaRepository categorias,
                          AuditoriaService auditoria) {
        this.produtos = produtos;
        this.categorias = categorias;
        this.auditoria = auditoria;
    }

    public List<Produto> listar(String termo) {
        return produtos.listar(termo);
    }

    public List<Produto> listarAtivos() {
        return produtos.listarAtivos();
    }

    public List<Categoria> listarCategorias() {
        return categorias.listar();
    }

    public Produto buscarPorCodigoBarras(String codigo) {
        return produtos.buscarPorCodigoBarras(codigo)
                .orElseThrow(() -> new ValidacaoException("Produto não encontrado para o código " + codigo + "."));
    }

    public Produto salvar(Produto produto) {
        validar(produto);
        if (produtos.codigoExiste(produto.getCodigoBarras(), produto.getId())) {
            throw new ValidacaoException("Já existe um produto com esse código de barras.");
        }
        Produto salvo = produtos.salvar(produto);
        auditoria.registrar("PRODUTO_SALVO", "PRODUTO", salvo.getId(), salvo.getNome());
        return salvo;
    }

    public void remover(long id) {
        produtos.remover(id);
    }

    public Categoria salvarCategoria(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new ValidacaoException("Nome da categoria é obrigatório.");
        }
        String normalizado = nome.strip();
        return categorias.buscarPorNome(normalizado)
                .orElseGet(() -> categorias.salvar(new Categoria(null, normalizado)));
    }

    public record ResultadoCsv(int importadas, List<String> erros) {
    }

    public ResultadoCsv importarCsv(String conteudo) {
        List<String> erros = new ArrayList<>();
        int importadas = 0;
        String[] linhas = conteudo == null
                ? new String[0]
                : conteudo.replace("\r\n", "\n").split("\n", -1);
        for (int i = 0; i < linhas.length; i++) {
            String linha = linhas[i].strip();
            if (i == 0 && linha.startsWith("\uFEFF")) {
                linha = linha.substring(1).strip();
            }
            if (i == 0 && linha.toLowerCase(Locale.ROOT).startsWith("codigo_barras")) {
                continue;
            }
            if (linha.isEmpty()) {
                continue;
            }
            try {
                importarLinha(linha);
                importadas++;
            } catch (ValidacaoException e) {
                erros.add("Linha " + (i + 1) + ": " + e.getMessage());
            }
        }
        if (importadas > 0) {
            auditoria.registrar("CSV_IMPORTADO", "PRODUTO", null,
                    importadas + " produto(s) importado(s), " + erros.size() + " linha(s) com erro");
        }
        return new ResultadoCsv(importadas, List.copyOf(erros));
    }

    public String exportarCsv(List<Produto> produtos) {
        StringBuilder csv = new StringBuilder(CABECALHO_CSV).append('\n');
        for (Produto produto : produtos) {
            csv.append(campoCsv(produto.getCodigoBarras())).append(SEPARADOR_CSV)
                    .append(campoCsv(produto.getNome())).append(SEPARADOR_CSV)
                    .append(campoCsv(produto.getCategoriaNome())).append(SEPARADOR_CSV)
                    .append(produto.getPrecoCusto().toPlainString()).append(SEPARADOR_CSV)
                    .append(produto.getPrecoVenda().toPlainString()).append(SEPARADOR_CSV)
                    .append(produto.getEstoque()).append(SEPARADOR_CSV)
                    .append(produto.isAtivo() ? "sim" : "nao").append('\n');
        }
        return csv.toString();
    }

    private void importarLinha(String linha) {
        List<String> colunas = separarCamposCsv(linha);
        if (colunas.size() != 7) {
            throw new ValidacaoException(
                    "esperado 7 colunas, encontradas " + colunas.size() + ".");
        }
        Produto produto = new Produto();
        produto.setCodigoBarras(colunas.get(0).strip());
        produto.setNome(colunas.get(1).strip());
        String categoria = colunas.get(2).strip();
        produto.setCategoriaId(categoria.isEmpty() ? null : salvarCategoria(categoria).getId());
        produto.setPrecoCusto(valor(colunas.get(3), "preço de custo"));
        produto.setPrecoVenda(valor(colunas.get(4), "preço de venda"));
        produto.setEstoque(inteiro(colunas.get(5)));
        produto.setAtivo(parseAtivo(colunas.get(6)));
        validar(produto);
        if (produtos.codigoExiste(produto.getCodigoBarras(), produto.getId())) {
            throw new ValidacaoException("Já existe um produto com esse código de barras.");
        }
        produtos.salvar(produto);
    }

    static List<String> separarCamposCsv(String linha) {
        List<String> colunas = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean entreAspas = false;
        for (int i = 0; i < linha.length(); i++) {
            char c = linha.charAt(i);
            if (entreAspas) {
                if (c == '"') {
                    if (i + 1 < linha.length() && linha.charAt(i + 1) == '"') {
                        atual.append('"');
                        i++;
                    } else {
                        entreAspas = false;
                    }
                } else {
                    atual.append(c);
                }
            } else if (c == '"') {
                entreAspas = true;
            } else if (c == ';') {
                colunas.add(atual.toString());
                atual.setLength(0);
            } else {
                atual.append(c);
            }
        }
        colunas.add(atual.toString());
        return colunas;
    }

    private static String campoCsv(String valor) {
        if (valor == null) {
            return "";
        }
        if (valor.contains(";") || valor.contains("\"") || valor.contains("\n")) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor;
    }

    private BigDecimal valor(String texto, String campo) {
        try {
            return new BigDecimal(texto.strip().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new ValidacaoException(campo + " inválido: \"" + texto.strip() + "\".");
        }
    }

    private int inteiro(String texto) {
        try {
            return Integer.parseInt(texto.strip());
        } catch (NumberFormatException e) {
            throw new ValidacaoException("estoque inválido: \"" + texto.strip() + "\".");
        }
    }

    private boolean parseAtivo(String texto) {
        return switch (texto.strip().toLowerCase(Locale.ROOT)) {
            case "sim", "true", "1" -> true;
            case "nao", "não", "false", "0" -> false;
            default -> throw new ValidacaoException("ativo deve ser sim ou nao: \"" + texto.strip() + "\".");
        };
    }

    private void validar(Produto produto) {
        if (produto.getNome() == null || produto.getNome().isBlank()) {
            throw new ValidacaoException("Nome do produto é obrigatório.");
        }
        if (produto.getCodigoBarras() == null || produto.getCodigoBarras().isBlank()) {
            throw new ValidacaoException("Código de barras é obrigatório.");
        }
        if (produto.getPrecoVenda() == null || produto.getPrecoVenda().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidacaoException("Preço de venda deve ser maior que zero.");
        }
        if (produto.getPrecoCusto() == null || produto.getPrecoCusto().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidacaoException("Preço de custo não pode ser negativo.");
        }
        if (produto.getEstoque() < 0) {
            throw new ValidacaoException("Estoque não pode ser negativo.");
        }
    }
}
