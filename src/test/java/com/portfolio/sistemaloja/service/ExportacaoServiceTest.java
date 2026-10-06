package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.Aplicacao;
import com.portfolio.sistemaloja.BancoTeste;
import com.portfolio.sistemaloja.model.FormaPagamento;
import com.portfolio.sistemaloja.service.ExportacaoService.DadosRelatorio;
import com.portfolio.sistemaloja.model.Produto;
import com.portfolio.sistemaloja.model.Usuario;
import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.model.VendaItem;
import com.portfolio.sistemaloja.repository.ProdutoVendado;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportacaoServiceTest {

    private Aplicacao app;
    private Usuario operador;
    private LocalDate hoje = LocalDate.now();

    @BeforeEach
    void preparar() {
        app = BancoTeste.criar("export" + System.nanoTime());
        operador = app.getAuth().autenticar("operador", "caixa123");
        registrarVenda("7891000100103", 2);
        registrarVenda("7891000100104", 5);
    }

    @Test
    void exportaPdfComCabecalhoETabelas() throws Exception {
        Path arquivo = Files.createTempFile("relatorio", ".pdf");
        app.getExportacao().exportarPdf(arquivo.toFile(), dados());

        assertTrue(Files.size(arquivo) > 0, "PDF não pode ficar vazio");
        byte[] cabecalho = Files.readAllBytes(arquivo);
        assertEquals('%', (char) cabecalho[0]);
        assertEquals('P', (char) cabecalho[1]);
        assertEquals('D', (char) cabecalho[2]);
        assertEquals('F', (char) cabecalho[3]);
        Files.deleteIfExists(arquivo);
    }

    @Test
    void exportaExcelComAsTrasAbasEStruturaEsperada() throws Exception {
        Path arquivo = Files.createTempFile("relatorio", ".xlsx");
        app.getExportacao().exportarExcel(arquivo.toFile(), dados());

        try (FileInputStream entrada = new FileInputStream(arquivo.toFile());
             XSSFWorkbook planilha = new XSSFWorkbook(entrada)) {
            assertEquals("Resumo", planilha.getSheetName(0));
            assertEquals("Vendas", planilha.getSheetName(1));
            assertEquals("Mais vendidos", planilha.getSheetName(2));

            Sheet vendas = planilha.getSheet("Vendas");
            assertEquals("Número", vendas.getRow(0).getCell(0).getStringCellValue());
            assertEquals(3, vendas.getPhysicalNumberOfRows(), "cabeçalho + 2 vendas");

            double totalMaisRecente = vendas.getRow(1).getCell(5).getNumericCellValue();
            double totalAnterior = vendas.getRow(2).getCell(5).getNumericCellValue();
            assertEquals(30.48, totalMaisRecente + totalAnterior, 0.001,
                    "soma dos totais das 2 vendas");

            Sheet resumo = planilha.getSheet("Resumo");
            assertEquals("Relatório de Vendas", resumo.getRow(0).getCell(0).getStringCellValue());

            Sheet top = planilha.getSheet("Mais vendidos");
            assertTrue(top.getLastRowNum() >= 1, "deve listar ao menos um produto vendido");
        }
        Files.deleteIfExists(arquivo);
    }

    @Test
    void exportaRelatorioSemVendasSemLancarErro() throws Exception {
        app.getRelatorios().resumoPeriodo(hoje, hoje);
        Aplicacao bancoLimpo = BancoTeste.criar("exportvazio" + System.nanoTime());
        DadosRelatorio vazio = new DadosRelatorio(hoje, hoje,
                bancoLimpo.getRelatorios().resumoPeriodo(hoje, hoje),
                bancoLimpo.getRelatorios().vendasDoPeriodo(hoje, hoje), List.of(), List.of());

        Path pdf = Files.createTempFile("vazio", ".pdf");
        Path xlsx = Files.createTempFile("vazio", ".xlsx");
        bancoLimpo.getExportacao().exportarPdf(pdf.toFile(), vazio);
        bancoLimpo.getExportacao().exportarExcel(xlsx.toFile(), vazio);

        assertTrue(Files.size(pdf) > 0);
        assertTrue(Files.size(xlsx) > 0);
        Files.deleteIfExists(pdf);
        Files.deleteIfExists(xlsx);
    }

    @Test
    void exportaPdfIncluiOGraficoDeFaturamento() throws Exception {
        DadosRelatorio comGrafico = dados();
        DadosRelatorio semImagem = new DadosRelatorio(hoje, hoje, comGrafico.resumo(),
                comGrafico.vendas(), comGrafico.maisVendidos(), List.of());

        Path comGraficoPdf = Files.createTempFile("comgrafico", ".pdf");
        Path semImagemPdf = Files.createTempFile("semimagem", ".pdf");
        app.getExportacao().exportarPdf(comGraficoPdf.toFile(), comGrafico);
        app.getExportacao().exportarPdf(semImagemPdf.toFile(), semImagem);

        String conteudo = new String(Files.readAllBytes(comGraficoPdf), java.nio.charset.StandardCharsets.ISO_8859_1);
        assertTrue(conteudo.contains("/Image"), "o PDF deve conter a imagem do gráfico");
        assertTrue(Files.size(comGraficoPdf) > Files.size(semImagemPdf),
                "PDF com gráfico deve ser maior que sem imagem");
        Files.deleteIfExists(comGraficoPdf);
        Files.deleteIfExists(semImagemPdf);
    }

    @Test
    void sugereNomeDeArquivoComOPeriodo() {
        DadosRelatorio dados = dados();
        String nome = app.getExportacao().nomeSugerido(dados, "pdf");
        assertTrue(nome.startsWith("relatorio_vendas_" + hoje));
        assertTrue(nome.endsWith(".pdf"));
    }

    @Test
    void falhaComCaminhoInexistente() {
        File impossivel = new File("pasta/que/nao/existe/relatorio.pdf");
        assertThrows(ExportacaoService.ExportacaoException.class,
                () -> app.getExportacao().exportarPdf(impossivel, dados()));
    }

    private ExportacaoService.DadosRelatorio dados() {
        List<Venda> vendas = app.getRelatorios().vendasDoPeriodo(hoje, hoje);
        List<ProdutoVendado> top = app.getRelatorios().maisVendidos(hoje, hoje);
        RelatorioService.ResumoDashboard resumo = app.getRelatorios().resumoPeriodo(hoje, hoje);
        return new ExportacaoService.DadosRelatorio(hoje, hoje, resumo, vendas, top,
                app.getRelatorios().faturamentoPorDia(hoje, hoje));
    }

    private void registrarVenda(String codigo, int quantidade) {
        Produto produto = app.getProdutos().buscarPorCodigoBarras(codigo);
        Venda venda = new Venda();
        venda.setUsuarioId(operador.getId());
        venda.setFormaPagamento(FormaPagamento.PIX);
        venda.adicionarItem(new VendaItem(produto.getId(), produto.getNome(),
                quantidade, produto.getPrecoVenda()));
        app.getVendas().registrar(venda);
    }
}
