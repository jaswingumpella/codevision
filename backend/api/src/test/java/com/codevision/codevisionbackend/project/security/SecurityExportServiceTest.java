package com.codevision.codevisionbackend.project.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.codevision.codevisionbackend.analyze.LoggerInsightSummary;
import com.codevision.codevisionbackend.analyze.PiiPciFindingSummary;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SecurityExportServiceTest {

    private SecurityExportService exportService;

    @BeforeEach
    void setUp() {
        exportService = new SecurityExportService();
    }

    @Test
    void buildsCsvForLoggerInsights() {
        List<LoggerInsightSummary> summaries = List.of(new LoggerInsightSummary(
                "com.example.Class",
                "src/main/java/Class.java",
                "INFO",
                42,
                "User {} logged in",
                List.of("userId"),
                true,
                false));

        String csv = new String(exportService.buildLoggerCsv(summaries), StandardCharsets.UTF_8);

        assertThat(csv).contains("className,filePath,logLevel,lineNumber,messageTemplate,variables,piiRisk,pciRisk");
        assertThat(csv).contains("User {} logged in");
        assertThat(csv).contains("true,false");
    }

    @Test
    void buildsCsvForPiiFindings() {
        List<PiiPciFindingSummary> findings =
                List.of(new PiiPciFindingSummary("src/data.txt", 9, "card=4111", "PCI", "HIGH", false));

        String csv = new String(exportService.buildPiiCsv(findings), StandardCharsets.UTF_8);

        assertThat(csv).contains("filePath,lineNumber,snippet,matchType,severity,ignored");
        assertThat(csv).contains("PCI").contains("HIGH");
    }

    @Test
    void producesPdfBytesForLoggerAndPiiReports() {
        byte[] loggerPdf =
                exportService.buildLoggerPdf("demo", List.of(new LoggerInsightSummary("c", null, "INFO", -1, null, List.of(), false, false)));
        byte[] piiPdf = exportService.buildPiiPdf("demo", List.of());

        assertThat(loggerPdf.length).isGreaterThan(0);
        assertThat(piiPdf.length).isGreaterThan(0);
    }

    @Test
    void buildsTimestampedFilenames() {
        String fileName = exportService.buildFileName("logs", "csv", 55L);

        assertThat(fileName).startsWith("logs-project-55-").endsWith(".csv");
    }
}
