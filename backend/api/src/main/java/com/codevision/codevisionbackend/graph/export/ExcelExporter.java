package com.codevision.codevisionbackend.graph.export;

import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Exports a {@link KnowledgeGraph} as an XLSX file (Excel) using Apache POI.
 * Creates two sheets: "Nodes" and "Edges" with formatted header rows.
 */
@Component
public class ExcelExporter implements GraphExporter {

    @Override
    public String formatName() {
        return "excel";
    }

    @Override
    public String fileExtension() {
        return ".xlsx";
    }

    @Override
    public String contentType() {
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }

    @Override
    public byte[] export(KnowledgeGraph graph) {
        try (var workbook = new XSSFWorkbook()) {
            var headerStyle = createHeaderStyle(workbook);

            // Nodes sheet
            var nodesSheet = workbook.createSheet("Nodes");
            var nodeHeaderRow = nodesSheet.createRow(0);
            var nodeHeaders = new String[]{"ID", "Type", "Name", "Qualified Name"};
            for (var i = 0; i < nodeHeaders.length; i++) {
                var cell = nodeHeaderRow.createCell(i);
                cell.setCellValue(nodeHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            var nodeRowIdx = 1;
            for (var entry : graph.getNodes().entrySet()) {
                var node = entry.getValue();
                var row = nodesSheet.createRow(nodeRowIdx++);
                row.createCell(0).setCellValue(nullSafe(node.id()));
                row.createCell(1).setCellValue(node.type() != null ? node.type().name() : "");
                row.createCell(2).setCellValue(nullSafe(node.name()));
                row.createCell(3).setCellValue(nullSafe(node.qualifiedName()));
            }

            for (var i = 0; i < nodeHeaders.length; i++) {
                nodesSheet.autoSizeColumn(i);
            }

            // Edges sheet
            var edgesSheet = workbook.createSheet("Edges");
            var edgeHeaderRow = edgesSheet.createRow(0);
            var edgeHeaders = new String[]{"ID", "Type", "Source", "Target", "Label"};
            for (var i = 0; i < edgeHeaders.length; i++) {
                var cell = edgeHeaderRow.createCell(i);
                cell.setCellValue(edgeHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            var edgeRowIdx = 1;
            for (var edge : graph.getEdges()) {
                var row = edgesSheet.createRow(edgeRowIdx++);
                row.createCell(0).setCellValue(nullSafe(edge.id()));
                row.createCell(1).setCellValue(edge.type() != null ? edge.type().name() : "");
                row.createCell(2).setCellValue(nullSafe(edge.sourceNodeId()));
                row.createCell(3).setCellValue(nullSafe(edge.targetNodeId()));
                row.createCell(4).setCellValue(nullSafe(edge.label()));
            }

            for (var i = 0; i < edgeHeaders.length; i++) {
                edgesSheet.autoSizeColumn(i);
            }

            var baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new ExportException("Failed to export graph as Excel", e);
        }
    }

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        var style = workbook.createCellStyle();
        var font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}
