package com.codevision.codevisionbackend.project.security;

import com.codevision.codevisionbackend.analyze.LoggerInsightSummary;
import com.codevision.codevisionbackend.analyze.PiiPciFindingSummary;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SecurityExportService {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final int PDF_WRAP_WIDTH = 100;

    public byte[] buildLoggerCsv(List<LoggerInsightSummary> logs) {
        StringBuilder builder = new StringBuilder();
        builder.append("className,filePath,logLevel,lineNumber,messageTemplate,variables,piiRisk,pciRisk").append("\n");
        logs.stream()
                .map(this::formatLoggerCsvRow)
                .forEach(row -> builder.append(row).append("\n"));
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] buildPiiCsv(List<PiiPciFindingSummary> findings) {
        StringBuilder builder = new StringBuilder();
        builder.append("filePath,lineNumber,snippet,matchType,severity,ignored").append("\n");
        findings.stream()
                .map(this::formatPiiCsvRow)
                .forEach(row -> builder.append(row).append("\n"));
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] buildLoggerPdf(String projectName, List<LoggerInsightSummary> logs) {
        List<String> lines = new ArrayList<>();
        addTitle(lines, "Logger Insights", projectName);
        if (logs.isEmpty()) {
            lines.add("No log statements were discovered for this project.");
        } else {
            logs.forEach(log -> {
                lines.add("Class: " + safe(log.className()));
                if (StringUtils.hasText(log.filePath())) {
                    lines.add("File: " + log.filePath());
                }
                lines.add("Level: " + log.logLevel() + " (line " + (log.lineNumber() < 0 ? "n/a" : log.lineNumber()) + ")");
                addWrappedLine(lines, "Message: ", log.messageTemplate());
                String vars = log.variables().isEmpty()
                        ? "—"
                        : String.join(", ", log.variables());
                addWrappedLine(lines, "Variables: ", vars);
                lines.add("PII Risk: " + (log.piiRisk() ? "YES" : "NO") + " | PCI Risk: " + (log.pciRisk() ? "YES" : "NO"));
                lines.add("");
            });
        }
        return renderPdf(lines);
    }

    public byte[] buildPiiPdf(String projectName, List<PiiPciFindingSummary> findings) {
        List<String> lines = new ArrayList<>();
        addTitle(lines, "PCI/PII Findings", projectName);
        if (findings.isEmpty()) {
            lines.add("No potential sensitive data matches were detected.");
        } else {
            findings.forEach(finding -> {
                lines.add("File: " + safe(finding.filePath()));
                lines.add("Line: " + (finding.lineNumber() <= 0 ? "n/a" : finding.lineNumber()));
                addWrappedLine(lines, "Snippet: ", finding.snippet());
                lines.add("Type: " + finding.matchType() + " | Severity: " + finding.severity());
                lines.add("Ignored by pattern?: " + (finding.ignored() ? "YES" : "NO"));
                lines.add("");
            });
        }
        return renderPdf(lines);
    }

    public String buildFileName(String prefix, String extension, Long projectId) {
        String timestamp = TIMESTAMP_FORMATTER.format(OffsetDateTime.now());
        return "%s-project-%s-%s.%s".formatted(prefix, projectId != null ? projectId : "unknown", timestamp, extension);
    }

    private String formatLoggerCsvRow(LoggerInsightSummary summary) {
        return List.of(
                        escapeCsv(summary.className()),
                        escapeCsv(summary.filePath()),
                        escapeCsv(summary.logLevel()),
                        escapeCsv(summary.lineNumber()),
                        escapeCsv(summary.messageTemplate()),
                        escapeCsv(String.join(";", summary.variables())),
                        escapeCsv(summary.piiRisk()),
                        escapeCsv(summary.pciRisk()))
                .stream()
                .collect(Collectors.joining(","));
    }

    private String formatPiiCsvRow(PiiPciFindingSummary finding) {
        return List.of(
                        escapeCsv(finding.filePath()),
                        escapeCsv(finding.lineNumber()),
                        escapeCsv(finding.snippet()),
                        escapeCsv(finding.matchType()),
                        escapeCsv(finding.severity()),
                        escapeCsv(finding.ignored()))
                .stream()
                .collect(Collectors.joining(","));
    }

    private String escapeCsv(Object value) {
        if (value == null) {
            return "";
        }
        String str = String.valueOf(value);
        boolean requiresEscape = str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r");
        if (!requiresEscape) {
            return str;
        }
        return "\"" + str.replace("\"", "\"\"") + "\"";
    }

    private void addTitle(List<String> lines, String title, String projectName) {
        lines.add(title.toUpperCase(Locale.ROOT));
        if (StringUtils.hasText(projectName)) {
            lines.add("Project: " + projectName);
        }
        lines.add("");
    }

    private void addWrappedLine(List<String> lines, String label, String text) {
        String normalized = text == null ? "" : text;
        if (!StringUtils.hasText(normalized)) {
            lines.add(label);
            return;
        }
        int cursor = 0;
        boolean firstSegment = true;
        while (cursor < normalized.length()) {
            int end = Math.min(cursor + PDF_WRAP_WIDTH, normalized.length());
            int breakIndex = findBreakIndex(normalized, cursor, end);
            String segment = normalized.substring(cursor, breakIndex).trim();
            lines.add((firstSegment ? label : pad(label.length())) + segment);
            cursor = breakIndex;
            firstSegment = false;
        }
    }

    private int findBreakIndex(String text, int start, int proposedEnd) {
        if (proposedEnd >= text.length()) {
            return text.length();
        }
        int lastSpace = text.lastIndexOf(' ', proposedEnd);
        if (lastSpace <= start) {
            return proposedEnd;
        }
        return lastSpace;
    }

    private String pad(int length) {
        return " ".repeat(Math.max(0, length));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private byte[] renderPdf(List<String> lines) {
        if (lines.isEmpty()) {
            lines = List.of("No data available.");
        }
        try (PDDocument document = new PDDocument()) {
            PDType1Font font = PDType1Font.HELVETICA;
            float fontSize = 11f;
            float leading = 14f;
            float margin = 48f;
            PDRectangle pageSize = PDRectangle.LETTER;
            int maxLinesPerPage = Math.max(1, (int) ((pageSize.getHeight() - (margin * 2)) / leading));
            PDPage page = new PDPage(pageSize);
            document.addPage(page);
            PDPageContentStream contentStream = startPage(document, page, font, fontSize, leading, margin);
            int lineCountOnPage = 0;

            for (String line : lines) {
                if (lineCountOnPage >= maxLinesPerPage) {
                    contentStream.endText();
                    contentStream.close();
                    page = new PDPage(pageSize);
                    document.addPage(page);
                    contentStream = startPage(document, page, font, fontSize, leading, margin);
                    lineCountOnPage = 0;
                }
                contentStream.showText(sanitize(line));
                contentStream.newLine();
                lineCountOnPage++;
            }

            contentStream.endText();
            contentStream.close();

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate PDF export", e);
        }
    }

    private PDPageContentStream startPage(
            PDDocument document,
            PDPage page,
            PDType1Font font,
            float fontSize,
            float leading,
            float margin) throws IOException {
        PDPageContentStream stream = new PDPageContentStream(document, page);
        stream.setFont(font, fontSize);
        stream.setLeading(leading);
        stream.beginText();
        stream.newLineAtOffset(margin, page.getMediaBox().getHeight() - margin);
        return stream;
    }

    private String sanitize(String line) {
        if (line == null) {
            return "";
        }
        return line.replaceAll("[\\t\\r]", " ");
    }
}
