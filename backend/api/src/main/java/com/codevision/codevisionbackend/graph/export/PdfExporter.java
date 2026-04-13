package com.codevision.codevisionbackend.graph.export;

import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Exports a {@link KnowledgeGraph} as a PDF document using Apache PDFBox.
 * Contains a title page followed by node and edge listing tables.
 */
@Component
public class PdfExporter implements GraphExporter {

    private static final float MARGIN = 50;
    private static final float FONT_SIZE = 10;
    private static final float TITLE_FONT_SIZE = 24;
    private static final float HEADING_FONT_SIZE = 16;
    private static final float LINE_HEIGHT = 14;

    private final PdfExportProperties properties;

    public PdfExporter(PdfExportProperties properties) {
        this.properties = properties;
    }

    @Override
    public String formatName() {
        return "pdf";
    }

    @Override
    public String fileExtension() {
        return ".pdf";
    }

    @Override
    public String contentType() {
        return "application/pdf";
    }

    @Override
    public byte[] export(KnowledgeGraph graph) {
        try (var document = new PDDocument()) {
            writeTitlePage(document, graph);
            writeNodesSection(document, graph);
            writeEdgesSection(document, graph);

            var baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new ExportException("Failed to export graph as PDF", e);
        }
    }

    private void writeTitlePage(PDDocument document, KnowledgeGraph graph) throws IOException {
        var page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        try (var cs = new PDPageContentStream(document, page)) {
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_BOLD, TITLE_FONT_SIZE);
            cs.newLineAtOffset(MARGIN, page.getMediaBox().getHeight() - 100);
            cs.showText("CodeVision Knowledge Graph");
            cs.endText();

            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, FONT_SIZE);
            cs.newLineAtOffset(MARGIN, page.getMediaBox().getHeight() - 130);
            cs.showText("Nodes: " + graph.nodeCount() + "  |  Edges: " + graph.edgeCount());
            cs.endText();
        }
    }

    private void writeNodesSection(PDDocument document, KnowledgeGraph graph) throws IOException {
        var page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        var cs = new PDPageContentStream(document, page);
        var y = page.getMediaBox().getHeight() - MARGIN;

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, HEADING_FONT_SIZE);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText("Nodes");
        cs.endText();
        y -= LINE_HEIGHT * 2;

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, FONT_SIZE);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(String.format("%-30s %-20s %-40s", "Name", "Type", "Qualified Name"));
        cs.endText();
        y -= LINE_HEIGHT;

        cs.setFont(PDType1Font.HELVETICA, FONT_SIZE);

        for (var entry : graph.getNodes().entrySet()) {
            if (y < MARGIN + LINE_HEIGHT) {
                cs.close();
                page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                cs = new PDPageContentStream(document, page);
                y = page.getMediaBox().getHeight() - MARGIN;
                cs.setFont(PDType1Font.HELVETICA, FONT_SIZE);
            }

            var node = entry.getValue();
            var name = truncate(node.name(), properties.nodeNameWidth());
            var type = truncate(node.type() != null ? node.type().name() : "", properties.nodeTypeWidth());
            var qualName = truncate(node.qualifiedName(), properties.nodeQualifiedNameWidth());

            cs.beginText();
            cs.newLineAtOffset(MARGIN, y);
            cs.showText(String.format("%-30s %-20s %-40s", name, type, qualName));
            cs.endText();
            y -= LINE_HEIGHT;
        }

        cs.close();
    }

    private void writeEdgesSection(PDDocument document, KnowledgeGraph graph) throws IOException {
        var page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        var cs = new PDPageContentStream(document, page);
        var y = page.getMediaBox().getHeight() - MARGIN;

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, HEADING_FONT_SIZE);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText("Edges");
        cs.endText();
        y -= LINE_HEIGHT * 2;

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, FONT_SIZE);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(String.format("%-20s %-25s %-25s %-20s", "Type", "Source", "Target", "Label"));
        cs.endText();
        y -= LINE_HEIGHT;

        cs.setFont(PDType1Font.HELVETICA, FONT_SIZE);

        for (var edge : graph.getEdges()) {
            if (y < MARGIN + LINE_HEIGHT) {
                cs.close();
                page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                cs = new PDPageContentStream(document, page);
                y = page.getMediaBox().getHeight() - MARGIN;
                cs.setFont(PDType1Font.HELVETICA, FONT_SIZE);
            }

            var type = truncate(edge.type() != null ? edge.type().name() : "", properties.edgeTypeWidth());
            var source = truncate(edge.sourceNodeId(), properties.edgeSourceWidth());
            var target = truncate(edge.targetNodeId(), properties.edgeTargetWidth());
            var label = truncate(edge.label(), properties.edgeLabelWidth());

            cs.beginText();
            cs.newLineAtOffset(MARGIN, y);
            cs.showText(String.format("%-20s %-25s %-25s %-20s", type, source, target, label));
            cs.endText();
            y -= LINE_HEIGHT;
        }

        cs.close();
    }

    private String truncate(String value, int maxLen) {
        if (value == null) {
            return "";
        }
        var safe = value.replaceAll("[^\\x20-\\x7E]", "?");
        if (safe.length() > maxLen) {
            return safe.substring(0, maxLen - 2) + "..";
        }
        return safe;
    }
}
