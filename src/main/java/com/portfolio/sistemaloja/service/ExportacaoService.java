package com.portfolio.sistemaloja.service;

import com.portfolio.sistemaloja.grafico.GraficoFaturamento;
import com.portfolio.sistemaloja.model.StatusVenda;
import com.portfolio.sistemaloja.model.Venda;
import com.portfolio.sistemaloja.repository.ProdutoVendado;
import com.portfolio.sistemaloja.repository.VendaDoDia;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openpdf.text.Chunk;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.FontFactory;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfWriter;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class ExportacaoService {

    private static final DateTimeFormatter DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final NumberFormat MOEDA =
            NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private static final Color COR_CABECALHO = new Color(33, 97, 140);
    private static final Color COR_ZEBRA = new Color(240, 244, 248);
    private static final Color COR_ALERTA = new Color(180, 60, 40);

    public record DadosRelatorio(LocalDate inicio, LocalDate fim,
                                 RelatorioService.ResumoDashboard resumo,
                                 List<Venda> vendas,
                                 List<ProdutoVendado> maisVendidos,
                                 List<VendaDoDia> porDia) {
        public DadosRelatorio {
            vendas = vendas == null ? List.of() : List.copyOf(vendas);
            maisVendidos = maisVendidos == null ? List.of() : List.copyOf(maisVendidos);
            porDia = porDia == null ? List.of() : List.copyOf(porDia);
        }
    }

    public String nomeSugerido(DadosRelatorio dados, String extensao) {
        return "relatorio_vendas_" + dados.inicio() + "_a_" + dados.fim() + "." + extensao;
    }

    public File exportarPdf(File destino, DadosRelatorio dados) {
        Document documento = new Document(PageSize.A4, 40, 40, 50, 55);
        try (FileOutputStream saida = new FileOutputStream(destino)) {
            PdfWriter escritor = PdfWriter.getInstance(documento, saida);
            escritor.setPageEvent(new Rodape());
            documento.open();
            documento.add(titulo("Relatório de Vendas", 18));
            documento.add(new Paragraph(periodo(dados)));
            documento.add(new Paragraph("Gerado em " + LocalDateTime.now().format(DATA_HORA)
                    + " pelo Sistema de Gestão de Loja.", fonte(9, org.openpdf.text.Font.NORMAL, Color.GRAY)));
            documento.add(Chunk.NEWLINE);

            documento.add(titulo("Resumo do período", 13));
            documento.add(new Paragraph(resumoTexto(dados.resumo()),
                    fonte(11, org.openpdf.text.Font.NORMAL, Color.BLACK)));
            documento.add(Chunk.NEWLINE);

            documento.add(titulo("Faturamento por dia", 13));
            documento.add(grafico(dados.porDia()));
            documento.add(Chunk.NEWLINE);

            documento.add(titulo("Vendas (" + dados.vendas().size() + ")", 13));
            documento.add(tabelaVendas(dados.vendas()));
            documento.add(Chunk.NEWLINE);

            documento.add(titulo("Mais vendidos", 13));
            documento.add(tabelaMaisVendidos(dados.maisVendidos()));

            documento.close();
            return destino;
        } catch (IOException e) {
            throw new ExportacaoException("Falha ao escrever o arquivo PDF: " + e.getMessage(), e);
        } finally {
            if (documento.isOpen()) {
                documento.close();
            }
        }
    }

    public File exportarExcel(File destino, DadosRelatorio dados) {
        try (XSSFWorkbook planilha = new XSSFWorkbook()) {
            CellStyle cabecalho = estiloCabecalho(planilha);
            CellStyle texto = estiloTexto(planilha);
            CellStyle moeda = estiloMoeda(planilha);
            CellStyle titulo = estiloTitulo(planilha);

            montarResumo(planilha.createSheet("Resumo"), dados, titulo, texto);
            montarVendas(planilha.createSheet("Vendas"), dados.vendas(), cabecalho, texto, moeda);
            montarMaisVendidos(planilha.createSheet("Mais vendidos"), dados.maisVendidos(),
                    cabecalho, texto, moeda);

            try (FileOutputStream saida = new FileOutputStream(destino)) {
                planilha.write(saida);
            }
            return destino;
        } catch (IOException e) {
            throw new ExportacaoException("Falha ao escrever o arquivo Excel: " + e.getMessage(), e);
        }
    }

    private org.openpdf.text.Image grafico(List<VendaDoDia> porDia) throws IOException {
        int largura = 500;
        int altura = 220;
        int escala = 2;
        GraficoFaturamento grafico = new GraficoFaturamento();
        GraficoFaturamento.Desenho desenho = grafico.render(largura, altura, porDia, escala);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ImageIO.write(desenho.imagem(), "png", buffer);
        org.openpdf.text.Image imagem = org.openpdf.text.Image.getInstance(buffer.toByteArray());
        imagem.scaleAbsolute(largura, altura);
        imagem.setAlignment(Element.ALIGN_CENTER);
        return imagem;
    }

    private Paragraph titulo(String texto, int tamanho) {
        Paragraph paragrafo = new Paragraph(texto, fonte(tamanho, org.openpdf.text.Font.BOLD, COR_CABECALHO));
        paragrafo.setSpacingAfter(6);
        return paragrafo;
    }

    private org.openpdf.text.Font fonte(float tamanho, int estilo, Color cor) {
        return FontFactory.getFont(FontFactory.HELVETICA, BaseFont.WINANSI, true,
                tamanho, estilo, cor);
    }

    private String periodo(DadosRelatorio dados) {
        return "Período: " + DATA.format(dados.inicio()) + " a " + DATA.format(dados.fim());
    }

    private String resumoTexto(RelatorioService.ResumoDashboard resumo) {
        return "Vendas: " + resumo.vendas()
                + "   |   Faturamento: " + moeda(resumo.faturamento())
                + "   |   Ticket médio: " + moeda(resumo.ticketMedio())
                + "   |   Clientes: " + resumo.clientes()
                + "   |   Produtos: " + resumo.produtos();
    }

    private String moeda(BigDecimal valor) {
        return MOEDA.format(valor == null ? BigDecimal.ZERO : valor);
    }

    private String dataHora(String dataHoraBanco) {
        if (dataHoraBanco == null) {
            return "-";
        }
        try {
            return DATA_HORA.format(java.time.LocalDateTime
                    .parse(dataHoraBanco.replace(' ', 'T')).withNano(0));
        } catch (java.time.format.DateTimeParseException e) {
            return dataHoraBanco;
        }
    }

    private PdfPTable tabelaVendas(List<Venda> vendas) {
        PdfPTable tabela = new PdfPTable(new float[]{1.4f, 1.8f, 2.2f, 1.8f, 1.6f, 1.3f, 1.3f});
        tabela.setWidthPercentage(100);
        adicionarCabecalho(tabela, "Número", "Data/Hora", "Cliente", "Operador",
                "Pagamento", "Total", "Status");

        boolean zebra = false;
        for (Venda venda : vendas) {
            boolean cancelada = venda.getStatus() == StatusVenda.CANCELADA;
            Color corTexto = cancelada ? COR_ALERTA : Color.BLACK;
            Color fundo = cancelada ? new Color(253, 235, 232) : (zebra ? COR_ZEBRA : Color.WHITE);
            adicionarCelula(tabela, venda.getNumero(), fundo, corTexto, Element.ALIGN_LEFT);
            adicionarCelula(tabela, dataHora(venda.getDataHora()), fundo, corTexto, Element.ALIGN_LEFT);
            adicionarCelula(tabela, texto(venda.getClienteNome()), fundo, corTexto, Element.ALIGN_LEFT);
            adicionarCelula(tabela, texto(venda.getUsuarioNome()), fundo, corTexto, Element.ALIGN_LEFT);
            adicionarCelula(tabela, venda.getFormaPagamento().getDescricao(), fundo, corTexto, Element.ALIGN_LEFT);
            adicionarCelula(tabela, moeda(venda.getTotal()), fundo, corTexto, Element.ALIGN_RIGHT);
            adicionarCelula(tabela, venda.getStatus().getDescricao(), fundo, corTexto, Element.ALIGN_LEFT);
            zebra = !zebra;
        }
        if (vendas.isEmpty()) {
            adicionarCelula(tabela, "Nenhuma venda no período", Color.WHITE, Color.GRAY, Element.ALIGN_LEFT);
            for (int i = 1; i < 7; i++) {
                PdfPCell celula = new PdfPCell(new Phrase(" "));
                celula.setBackgroundColor(Color.WHITE);
                tabela.addCell(celula);
            }
        }
        return tabela;
    }

    private PdfPTable tabelaMaisVendidos(List<ProdutoVendado> produtos) {
        PdfPTable tabela = new PdfPTable(new float[]{4f, 1.5f, 2f});
        tabela.setWidthPercentage(70);
        adicionarCabecalho(tabela, "Produto", "Qtd. vendida", "Total vendido");
        boolean zebra = false;
        for (ProdutoVendado produto : produtos) {
            Color fundo = zebra ? COR_ZEBRA : Color.WHITE;
            adicionarCelula(tabela, produto.nome(), fundo, Color.BLACK, Element.ALIGN_LEFT);
            adicionarCelula(tabela, String.valueOf(produto.quantidade()), fundo, Color.BLACK, Element.ALIGN_CENTER);
            adicionarCelula(tabela, moeda(produto.totalVendido()), fundo, Color.BLACK, Element.ALIGN_RIGHT);
            zebra = !zebra;
        }
        if (produtos.isEmpty()) {
            adicionarCelula(tabela, "Sem vendas no período", Color.WHITE, Color.GRAY, Element.ALIGN_LEFT);
            adicionarCelula(tabela, "-", Color.WHITE, Color.GRAY, Element.ALIGN_CENTER);
            adicionarCelula(tabela, "-", Color.WHITE, Color.GRAY, Element.ALIGN_RIGHT);
        }
        return tabela;
    }

    private String texto(String valor) {
        return valor == null || valor.isBlank() ? "-" : valor;
    }

    private void adicionarCabecalho(PdfPTable tabela, String... colunas) {
        for (String coluna : colunas) {
            PdfPCell celula = new PdfPCell(new Phrase(coluna, fonte(9, org.openpdf.text.Font.BOLD, Color.WHITE)));
            celula.setBackgroundColor(COR_CABECALHO);
            celula.setPadding(5);
            celula.setHorizontalAlignment(Element.ALIGN_LEFT);
            tabela.addCell(celula);
        }
    }

    private void adicionarCelula(PdfPTable tabela, String valor, Color fundo, Color texto, int alinhamento) {
        PdfPCell celula = new PdfPCell(new Phrase(valor, fonte(9, org.openpdf.text.Font.NORMAL, texto)));
        celula.setBackgroundColor(fundo);
        celula.setPadding(4);
        celula.setHorizontalAlignment(alinhamento);
        tabela.addCell(celula);
    }

    private void montarResumo(Sheet aba, DadosRelatorio dados, CellStyle estiloTitulo, CellStyle texto) {
        Row linhaTitulo = aba.createRow(0);
        linhaTitulo.createCell(0).setCellValue("Relatório de Vendas");
        linhaTitulo.getCell(0).setCellStyle(estiloTitulo);

        aba.createRow(1).createCell(0).setCellValue(periodo(dados));
        aba.createRow(2).createCell(0)
                .setCellValue("Gerado em " + LocalDateTime.now().format(DATA_HORA));

        RelatorioService.ResumoDashboard resumo = dados.resumo();
        String[][] metricas = {
                {"Vendas", String.valueOf(resumo.vendas())},
                {"Faturamento", moeda(resumo.faturamento())},
                {"Ticket médio", moeda(resumo.ticketMedio())},
                {"Clientes ativos", String.valueOf(resumo.clientes())},
                {"Produtos ativos", String.valueOf(resumo.produtos())},
                {"Itens com estoque baixo", String.valueOf(resumo.itensEstoqueBaixo())}
        };

        int linha = 4;
        Row cabecalho = aba.createRow(linha++);
        cabecalho.createCell(0).setCellValue("Indicador");
        cabecalho.createCell(1).setCellValue("Valor");
        cabecalho.getCell(0).setCellStyle(estiloTitulo);
        cabecalho.getCell(1).setCellStyle(estiloTitulo);

        for (String[] metrica : metricas) {
            Row registro = aba.createRow(linha++);
            registro.createCell(0).setCellValue(metrica[0]);
            registro.createCell(1).setCellValue(metrica[1]);
            registro.getCell(0).setCellStyle(texto);
            registro.getCell(1).setCellStyle(texto);
        }
        aba.setColumnWidth(0, 28 * 256);
        aba.setColumnWidth(1, 22 * 256);
    }

    private void montarVendas(Sheet aba, List<Venda> vendas, CellStyle cabecalho,
                              CellStyle texto, CellStyle moeda) {
        String[] colunas = {"Número", "Data/Hora", "Cliente", "Operador", "Pagamento", "Total", "Status"};
        Row cabecalhoLinha = aba.createRow(0);
        for (int i = 0; i < colunas.length; i++) {
            cabecalhoLinha.createCell(i).setCellValue(colunas[i]);
            cabecalhoLinha.getCell(i).setCellStyle(cabecalho);
        }
        int numeroLinha = 1;
        for (Venda venda : vendas) {
            Row linha = aba.createRow(numeroLinha++);
            linha.createCell(0).setCellValue(venda.getNumero());
            linha.createCell(1).setCellValue(dataHora(venda.getDataHora()));
            linha.createCell(2).setCellValue(texto(venda.getClienteNome()));
            linha.createCell(3).setCellValue(texto(venda.getUsuarioNome()));
            linha.createCell(4).setCellValue(venda.getFormaPagamento().getDescricao());
            Cell total = linha.createCell(5);
            total.setCellValue(venda.getTotal().doubleValue());
            total.setCellStyle(moeda);
            Cell status = linha.createCell(6);
            status.setCellValue(venda.getStatus().getDescricao());
            for (int i = 0; i < 5; i++) {
                linha.getCell(i).setCellStyle(texto);
            }
            status.setCellStyle(texto);
        }
        aba.createFreezePane(0, 1);
        int[] larguras = {12, 18, 26, 22, 18, 14, 14};
        for (int i = 0; i < larguras.length; i++) {
            aba.setColumnWidth(i, larguras[i] * 256);
        }
    }

    private void montarMaisVendidos(Sheet aba, List<ProdutoVendado> produtos, CellStyle cabecalho,
                                    CellStyle texto, CellStyle moeda) {
        String[] colunas = {"Produto", "Quantidade vendida", "Total vendido"};
        Row cabecalhoLinha = aba.createRow(0);
        for (int i = 0; i < colunas.length; i++) {
            cabecalhoLinha.createCell(i).setCellValue(colunas[i]);
            cabecalhoLinha.getCell(i).setCellStyle(cabecalho);
        }
        int numeroLinha = 1;
        for (ProdutoVendado produto : produtos) {
            Row linha = aba.createRow(numeroLinha++);
            linha.createCell(0).setCellValue(produto.nome());
            linha.getCell(0).setCellStyle(texto);
            linha.createCell(1).setCellValue(produto.quantidade());
            linha.getCell(1).setCellStyle(texto);
            Cell total = linha.createCell(2);
            total.setCellValue(produto.totalVendido().doubleValue());
            total.setCellStyle(moeda);
        }
        aba.createFreezePane(0, 1);
        aba.setColumnWidth(0, 34 * 256);
        aba.setColumnWidth(1, 20 * 256);
        aba.setColumnWidth(2, 18 * 256);
    }

    private org.apache.poi.xssf.usermodel.XSSFColor corPoi(Color cor) {
        return new org.apache.poi.xssf.usermodel.XSSFColor(
                new byte[]{(byte) cor.getRed(), (byte) cor.getGreen(), (byte) cor.getBlue()}, null);
    }

    private CellStyle estiloCabecalho(XSSFWorkbook planilha) {
        CellStyle estilo = planilha.createCellStyle();
        org.apache.poi.ss.usermodel.Font fonte = planilha.createFont();
        fonte.setBold(true);
        fonte.setColor(IndexedColors.WHITE.getIndex());
        estilo.setFont(fonte);
        estilo.setFillForegroundColor(corPoi(COR_CABECALHO));
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        estilo.setAlignment(HorizontalAlignment.CENTER);
        estilo.setVerticalAlignment(VerticalAlignment.CENTER);
        estilo.setBorderBottom(BorderStyle.THIN);
        return estilo;
    }

    private CellStyle estiloTexto(XSSFWorkbook planilha) {
        CellStyle estilo = planilha.createCellStyle();
        estilo.setBorderBottom(BorderStyle.THIN);
        estilo.setVerticalAlignment(VerticalAlignment.CENTER);
        return estilo;
    }

    private CellStyle estiloMoeda(XSSFWorkbook planilha) {
        CellStyle estilo = planilha.createCellStyle();
        DataFormat formato = planilha.createDataFormat();
        estilo.setDataFormat(formato.getFormat("#,##0.00"));
        estilo.setBorderBottom(BorderStyle.THIN);
        return estilo;
    }

    private CellStyle estiloTitulo(XSSFWorkbook planilha) {
        CellStyle estilo = planilha.createCellStyle();
        org.apache.poi.ss.usermodel.Font fonte = planilha.createFont();
        fonte.setBold(true);
        fonte.setColor(IndexedColors.WHITE.getIndex());
        estilo.setFont(fonte);
        estilo.setFillForegroundColor(corPoi(COR_CABECALHO));
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return estilo;
    }

    private static class Rodape extends PdfPageEventHelper {

        @Override
        public void onEndPage(PdfWriter escritor, Document documento) {
            org.openpdf.text.Font fonte = FontFactory.getFont(FontFactory.HELVETICA,
                    BaseFont.WINANSI, true, 8, org.openpdf.text.Font.NORMAL, Color.GRAY);
            Phrase texto = new Phrase("Sistema de Gestão de Loja  -  página "
                    + escritor.getPageNumber(), fonte);
            ColumnText.showTextAligned(escritor.getDirectContent(), Element.ALIGN_CENTER, texto,
                    (documento.left() + documento.right()) / 2, 28, 0);
        }
    }

    public static class ExportacaoException extends RuntimeException {

        public ExportacaoException(String mensagem, Throwable causa) {
            super(mensagem, causa);
        }
    }
}
