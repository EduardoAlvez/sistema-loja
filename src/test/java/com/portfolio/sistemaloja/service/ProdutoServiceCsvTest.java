package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.repository.EventoAuditoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProdutoServiceCsvTest {

    private static final String CABECALHO = ProdutoService.CABECALHO_CSV;

    private Aplicacao app;

    @BeforeEach
    void preparar() {
        app = BancoTeste.criar("csv" + System.nanoTime());
    }

    @Test
    void importaProdutosValidosComBOMECabecalho() {
        String conteudo = "\uFEFF" + CABECALHO + "\n"
                + "7891111111111;Arroz Tipo 1;Mercearia;5,50;8.99;10;sim\n"
                + "7892222222222;Leite Integral;;2,00;3,49;25;nao\n";

        ProdutoService.ResultadoCsv resultado = app.getProdutos().importarCsv(conteudo);

        assertEquals(2, resultado.importadas());
        assertTrue(resultado.erros().isEmpty());

        Produto arroz = app.getProdutos().buscarPorCodigoBarras("7891111111111");
        assertEquals(new BigDecimal("5.50"), arroz.getPrecoCusto());
        assertTrue(arroz.isAtivo());
        assertTrue(arroz.getCategoriaId() != null, "categoria nova deve ser criada");

        Produto leite = app.getProdutos().buscarPorCodigoBarras("7892222222222");
        assertFalse(leite.isAtivo());
        assertNull(leite.getCategoriaId(), "categoria vazia fica sem categoria");

        List<String> categorias = app.getProdutos().listarCategorias().stream()
                .map(c -> c.getNome()).toList();
        assertTrue(categorias.contains("Mercearia"));
    }

    @Test
    void reportaLinhasInvalidasEImportaAsValidas() {
        String conteudo = CABECALHO + "\n"
                + "7891111111111;Arroz;;5,50;8,99;10;sim\n"
                + "7892222222222;;Mercearia;5,50;8,99;10;sim\n"
                + "7893333333333;Feijao;;5,50;abc;10;sim\n"
                + "7894444444444;Cafe;;5,50;8,99;10;sim\n";

        ProdutoService.ResultadoCsv resultado = app.getProdutos().importarCsv(conteudo);

        assertEquals(2, resultado.importadas());
        assertEquals(2, resultado.erros().size());
        assertTrue(resultado.erros().get(0).startsWith("Linha 3:"),
                "erro deve apontar a linha do arquivo: " + resultado.erros().get(0));
        assertTrue(resultado.erros().get(0).contains("Nome do produto é obrigatório"));
        assertTrue(resultado.erros().get(1).startsWith("Linha 4:"));
        assertTrue(resultado.erros().get(1).contains("preço de venda inválido"));
        assertFalse(resultado.erros().get(1).contains("For input string"),
                "mensagem técnica não deve vazar para o usuário");

        assertTrue(resultado.erros().stream().noneMatch(e -> e.contains("Linha 2:")));
        assertNotNull(app.getProdutos().buscarPorCodigoBarras("7891111111111"));
        assertNotNull(app.getProdutos().buscarPorCodigoBarras("7894444444444"));
        assertThrows(ValidacaoException.class,
                () -> app.getProdutos().buscarPorCodigoBarras("7892222222222"),
                "linha invalida nao pode ser importada");
    }

    @Test
    void registraUmUnicoEventoDeAuditoriaParaAImportacaoInteira() {
        String conteudo = CABECALHO + "\n"
                + "7891111111111;Arroz;;5,50;8,99;10;sim\n"
                + "7892222222222;Feijao;;5,50;8,99;10;sim\n"
                + "linha com erro demais\n";

        app.getProdutos().importarCsv(conteudo);

        assertEquals(1, eventos("CSV_IMPORTADO").size(), "deve haver um unico evento");
        assertTrue(eventos("CSV_IMPORTADO").get(0).detalhe().contains("2 produto(s)"));
        assertTrue(eventos("CSV_IMPORTADO").get(0).detalhe().contains("1 linha(s)"));
        assertTrue(eventos("PRODUTO_SALVO").isEmpty(),
                "importacao nao deve gerar um evento por produto");
    }

    @Test
    void naoRegistraAuditoriaQuandoNadaEImportado() {
        String conteudo = CABECALHO + "\nso uma linha invalida\n";

        ProdutoService.ResultadoCsv resultado = app.getProdutos().importarCsv(conteudo);

        assertEquals(0, resultado.importadas());
        assertEquals(1, resultado.erros().size());
        assertTrue(eventos("CSV_IMPORTADO").isEmpty());
    }

    @Test
    void exportaComCabecalhoEAspasParaCamposComPontoEVirgula() {
        Produto tricky = novo("7891111111111", "Refrigerante \"Cola\"; 2L");
        app.getProdutos().salvar(tricky);

        String csv = app.getProdutos().exportarCsv(app.getProdutos().listar(""));

        assertTrue(csv.startsWith(CABECALHO + "\n"));
        assertTrue(csv.contains("\"Refrigerante \"\"Cola\"\"; 2L\""),
                "campo com ; e aspas deve vir entre aspas: " + csv);
        assertTrue(csv.contains(";;"), "produto sem categoria sai com coluna vazia");

        for (String linha : csv.split("\n")) {
            assertEquals(7, ProdutoService.separarCamposCsv(linha).size(),
                    "toda linha deve ter 7 colunas: " + linha);
        }
    }

    @Test
    void exportaOsProdutosImportadosComPrecosComPonto() {
        String conteudo = CABECALHO + "\n"
                + "7891111111111;Arroz Tipo 1;Mercearia;5,50;8.99;10;sim\n";
        app.getProdutos().importarCsv(conteudo);

        String csv = app.getProdutos().exportarCsv(app.getProdutos().listar(""));

        assertTrue(csv.contains("7891111111111;Arroz Tipo 1;Mercearia;5.50;8.99;10;sim"),
                "linha exportada deve refletir o importado: " + csv);
    }

    private Produto novo(String codigo, String nome) {
        Produto produto = new Produto();
        produto.setCodigoBarras(codigo);
        produto.setNome(nome);
        produto.setPrecoCusto(new BigDecimal("5.50"));
        produto.setPrecoVenda(new BigDecimal("8.99"));
        produto.setEstoque(10);
        produto.setAtivo(true);
        return produto;
    }

    private List<EventoAuditoria> eventos(String acao) {
        return app.getAuditoria().listar(LocalDate.now(), LocalDate.now()).stream()
                .filter(e -> e.acao().equals(acao))
                .toList();
    }
}
