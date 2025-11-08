package com.codevision.codevisionbackend.project.export;

import com.codevision.codevisionbackend.analyze.ApiEndpointSummary;
import com.codevision.codevisionbackend.analyze.GherkinFeatureSummary;
import com.codevision.codevisionbackend.analyze.GherkinScenarioSummary;
import com.codevision.codevisionbackend.analyze.LoggerInsightSummary;
import com.codevision.codevisionbackend.analyze.MetadataDump;
import com.codevision.codevisionbackend.analyze.ParsedDataResponse;
import com.codevision.codevisionbackend.analyze.PiiPciFindingSummary;
import com.codevision.codevisionbackend.analyze.AssetInventory;
import com.codevision.codevisionbackend.analyze.DbAnalysisSummary;
import com.codevision.codevisionbackend.analyze.DbAnalysisSummary.DbEntitySummary;
import com.codevision.codevisionbackend.analyze.DbAnalysisSummary.DaoOperationDetails;
import com.codevision.codevisionbackend.analyze.DiagramSummary;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
public class ExportService {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z").withZone(ZoneId.systemDefault());

    public String buildConfluenceHtml(ParsedDataResponse snapshot) {
        if (snapshot == null) {
            return "";
        }

        StringBuilder html = new StringBuilder(32_000);
        html.append("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\"/>")
                .append("<title>")
                .append(escape(snapshot.projectName(), "Project Documentation"))
                .append("</title>")
                .append("<style>")
                .append("body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;margin:0;padding:2.5rem;color:#122041;background:#f7f9ff;}")
                .append("h1,h2,h3{color:#0d1533;margin-top:2rem;}")
                .append("section{margin-bottom:2.5rem;padding:1.5rem;background:#fff;border-radius:18px;box-shadow:0 18px 40px rgba(21,34,72,.08);}")
                .append("table{width:100%;border-collapse:collapse;margin:1rem 0;font-size:0.95rem;}")
                .append("th,td{border:1px solid #dfe3f0;padding:0.6rem;text-align:left;vertical-align:top;}")
                .append("th{background:#eef1fb;color:#122041;font-weight:600;}")
                .append("code{background:#f4f6ff;padding:0.2rem 0.4rem;border-radius:6px;}")
                .append("pre{background:#0f172a;color:#e2e8f0;padding:1rem;border-radius:12px;overflow:auto;font-size:0.9rem;}")
                .append(".muted{color:#5a6385;font-style:italic;margin-top:0.4rem;}")
                .append(".badge{display:inline-block;padding:0.15rem 0.5rem;border-radius:999px;font-size:0.75rem;margin-right:0.35rem;background:#eef1fb;color:#2e3c6a;}")
                .append("</style></head><body>");

        appendOverviewSection(html, snapshot);
        appendApiSection(html, snapshot.apiEndpoints());
        appendDiagramSection(html, snapshot.diagrams());
        appendDatabaseSection(html, snapshot.dbAnalysis());
        appendLoggerSection(html, snapshot.loggerInsights());
        appendPiiSection(html, snapshot.piiPciScan());
        appendGherkinSection(html, snapshot.gherkinFeatures());
        appendTechStackSection(html, snapshot);
        appendFooter(html, snapshot);

        html.append("</body></html>");
        return html.toString();
    }

    private void appendOverviewSection(StringBuilder html, ParsedDataResponse snapshot) {
        int totalClasses = snapshot.classes() != null ? snapshot.classes().size() : 0;
        long controllerCount = snapshot.classes() == null
                ? 0
                : snapshot.classes().stream()
                        .filter(cls -> "CONTROLLER".equalsIgnoreCase(cls.stereotype()))
                        .count();
        long entityCount = snapshot.dbAnalysis() != null && snapshot.dbAnalysis().entities() != null
                ? snapshot.dbAnalysis().entities().size()
                : 0;
        AssetInventory assets = snapshot.assets();
        long imageCount = assets != null && assets.images() != null ? assets.images().size() : 0;

        html.append("<section><h1>1. Project Overview</h1>")
                .append("<p><strong>Project:</strong> ")
                .append(escape(snapshot.projectName(), "n/a"))
                .append("<br/><strong>Repository:</strong> ")
                .append(escape(snapshot.repoUrl(), "n/a"))
                .append("<br/><strong>Analyzed:</strong> ")
                .append(formatTimestamp(snapshot.analyzedAt()))
                .append("</p>");

        html.append("<table><thead><tr>")
                .append("<th>Total Classes</th><th>Controllers</th><th>Entities</th><th>Image Assets</th>")
                .append("</tr></thead><tbody><tr><td>")
                .append(totalClasses)
                .append("</td><td>")
                .append(controllerCount)
                .append("</td><td>")
                .append(entityCount)
                .append("</td><td>")
                .append(imageCount)
                .append("</td></tr></tbody></table>");

        if (snapshot.buildInfo() != null) {
            html.append("<p><strong>Maven:</strong> ")
                    .append(escape(snapshot.buildInfo().groupId(), ""))
                    .append(":")
                    .append(escape(snapshot.buildInfo().artifactId(), ""))
                    .append(":")
                    .append(escape(snapshot.buildInfo().version(), ""))
                    .append(" — Java ")
                    .append(escape(snapshot.buildInfo().javaVersion(), ""))
                    .append("</p>");
        }

        MetadataDump metadataDump = snapshot.metadataDump();
        if (metadataDump != null) {
            html.append("<p class=\"muted\">Metadata artifacts captured: ")
                    .append(count(metadataDump.openApiSpecs()))
                    .append(" OpenAPI, ")
                    .append(count(metadataDump.wsdlDocuments()))
                    .append(" WSDL, ")
                    .append(count(metadataDump.xsdDocuments()))
                    .append(" XSD.</p>");
        }

        html.append("</section>");
    }

    private void appendApiSection(StringBuilder html, List<ApiEndpointSummary> endpoints) {
        html.append("<section><h1>2. API Endpoints</h1>");
        if (endpoints == null || endpoints.isEmpty()) {
            html.append("<p class=\"muted\">No endpoints were detected in this analysis.</p></section>");
            return;
        }
        Map<String, List<ApiEndpointSummary>> grouped = endpoints.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        endpoint -> endpoint.protocol() == null ? "UNKNOWN" : endpoint.protocol().toUpperCase(Locale.ROOT),
                        Collectors.toList()));
        grouped.forEach((protocol, list) -> {
            html.append("<h2>").append(escape(protocol)).append(" Endpoints</h2>");
            List<List<String>> rows = list.stream()
                    .sorted(Comparator.comparing(endpoint -> defaultString(endpoint.pathOrOperation())))
                    .map(endpoint -> {
                        String specs = endpoint.specArtifacts() == null || endpoint.specArtifacts().isEmpty()
                                ? "—"
                                : endpoint.specArtifacts().stream()
                                        .map(artifact -> artifact.type() + ": " + artifact.name())
                                        .collect(Collectors.joining(", "));
                        return List.of(
                                escape(defaultString(endpoint.httpMethod(), protocol.equals("SOAP") ? "—" : "")),
                                escape(defaultString(endpoint.pathOrOperation())),
                                escape(defaultString(endpoint.controllerClass())),
                                escape(defaultString(endpoint.controllerMethod())),
                                escape(specs));
                    })
                    .toList();
            renderTable(html, List.of("Method", "Path / Operation", "Class", "Handler", "Specs"), rows);
        });
        html.append("</section>");
    }

    private void appendDiagramSection(StringBuilder html, List<DiagramSummary> diagrams) {
        html.append("<section><h1>3. Diagrams</h1>");
        if (diagrams == null || diagrams.isEmpty()) {
            html.append("<p class=\"muted\">No diagrams were generated for this project.</p></section>");
            return;
        }
        Map<String, List<DiagramSummary>> grouped = diagrams.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        diagram -> defaultString(diagram.diagramType(), "OTHER"), Collectors.toList()));
        grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    html.append("<h2>").append(escape(entry.getKey())).append("</h2>");
                    entry.getValue().forEach(diagram -> {
                        html.append("<h3>")
                                .append(escape(defaultString(diagram.title(), entry.getKey())))
                                .append("</h3>");
                        if (diagram.metadata() != null && diagram.metadata().containsKey("pathOrOperation")) {
                            html.append("<p class=\"muted\">Flow: ")
                                    .append(escape(String.valueOf(diagram.metadata().get("httpMethod"))))
                                    .append(" ")
                                    .append(escape(String.valueOf(diagram.metadata().get("pathOrOperation"))))
                                    .append("</p>");
                        }
                        if (diagram.plantumlSource() != null && !diagram.plantumlSource().isBlank()) {
                            html.append("<pre><code>")
                                    .append(escape(diagram.plantumlSource()))
                                    .append("</code></pre>");
                        }
                        if (diagram.mermaidSource() != null && !diagram.mermaidSource().isBlank()) {
                            html.append("<pre><code>")
                                    .append(escape(diagram.mermaidSource()))
                                    .append("</code></pre>");
                        }
                    });
                });
        html.append("</section>");
    }

    private void appendDatabaseSection(StringBuilder html, DbAnalysisSummary dbAnalysis) {
        html.append("<section><h1>4. Database Analysis</h1>");
        if (dbAnalysis == null
                || ((dbAnalysis.entities() == null || dbAnalysis.entities().isEmpty())
                        && (dbAnalysis.operationsByClass() == null || dbAnalysis.operationsByClass().isEmpty()))) {
            html.append("<p class=\"muted\">No database metadata was captured.</p></section>");
            return;
        }
        List<List<String>> entityRows = dbAnalysis.entities() == null
                ? List.of()
                : dbAnalysis.entities().stream()
                        .map(entity -> List.of(
                                escape(defaultString(entity.entityName())),
                                escape(defaultString(entity.tableName(), "—")),
                                escape(entity.primaryKeys() == null
                                        ? "—"
                                        : String.join(", ", entity.primaryKeys())),
                                escape(formatClassesForEntity(dbAnalysis.classesByEntity(), entity.entityName()))))
                        .toList();
        html.append("<h2>Entities and Interacting Classes</h2>");
        renderTable(html, List.of("Entity", "Table", "Primary Keys", "Classes Using It"), entityRows);

        html.append("<h2>DAO / Repository Operations</h2>");
        List<List<String>> operationRows = new ArrayList<>();
        if (dbAnalysis.operationsByClass() != null) {
            dbAnalysis.operationsByClass().forEach((repository, ops) -> {
                if (ops == null) {
                    return;
                }
                ops.forEach(operation -> operationRows.add(List.of(
                        escape(defaultString(repository)),
                        escape(defaultString(operation.methodName())),
                        escape(defaultString(operation.operationType())),
                        escape(defaultString(operation.target())),
                        escape(defaultString(operation.querySnippet(), "—")))));
            });
        }
        renderTable(
                html,
                List.of("Class", "Method", "Operation", "Target", "Query Snippet"),
                operationRows);
        html.append("</section>");
    }

    private void appendLoggerSection(StringBuilder html, List<LoggerInsightSummary> insights) {
        html.append("<section><h1>5. Logger Insights</h1>");
        if (insights == null || insights.isEmpty()) {
            html.append("<p class=\"muted\">No log statements captured.</p></section>");
            return;
        }
        long piiRisk = insights.stream().filter(LoggerInsightSummary::piiRisk).count();
        long pciRisk = insights.stream().filter(LoggerInsightSummary::pciRisk).count();
        html.append("<p>Total log statements: <strong>")
                .append(insights.size())
                .append("</strong> &middot; Flagged for PII: <strong>")
                .append(piiRisk)
                .append("</strong> &middot; Flagged for PCI: <strong>")
                .append(pciRisk)
                .append("</strong></p>");

        List<List<String>> flaggedRows = insights.stream()
                .filter(entry -> entry.piiRisk() || entry.pciRisk())
                .limit(25)
                .map(entry -> List.of(
                        escape(defaultString(entry.className())),
                        escape(defaultString(entry.logLevel())),
                        escape(defaultString(entry.messageTemplate())),
                        escape(defaultString(entry.filePath())),
                        escape(entry.piiRisk() ? "PII" : entry.pciRisk() ? "PCI" : "—")))
                .toList();
        if (flaggedRows.isEmpty()) {
            html.append("<p class=\"muted\">No potentially sensitive log entries detected.</p>");
        } else {
            html.append("<h2>Flagged Statements (first 25)</h2>");
            renderTable(
                    html,
                    List.of("Class", "Level", "Message", "Source", "Risk"),
                    flaggedRows);
        }
        html.append("</section>");
    }

    private void appendPiiSection(StringBuilder html, List<PiiPciFindingSummary> findings) {
        html.append("<section><h1>6. PCI / PII Scan</h1>");
        if (findings == null || findings.isEmpty()) {
            html.append("<p class=\"muted\">No sensitive-data matches detected.</p></section>");
            return;
        }
        Map<String, Long> severityCounts = findings.stream()
                .collect(Collectors.groupingBy(
                        finding -> defaultString(finding.severity(), "UNSPECIFIED"), Collectors.counting()));
        html.append("<p>")
                .append(severityCounts.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(entry -> "<span class=\"badge\">" + escape(entry.getKey()) + ": " + entry.getValue()
                                + "</span>")
                        .collect(Collectors.joining(" ")))
                .append("</p>");

        List<List<String>> rows = findings.stream()
                .limit(50)
                .map(finding -> List.of(
                        escape(defaultString(finding.filePath())),
                        escape(finding.lineNumber() > 0 ? Integer.toString(finding.lineNumber()) : "—"),
                        escape(defaultString(finding.snippet())),
                        escape(defaultString(finding.matchType())),
                        escape(defaultString(finding.severity())),
                        finding.ignored() ? "Yes" : "No"))
                .toList();
        renderTable(
                html,
                List.of("File", "Line", "Snippet", "Type", "Severity", "Ignored"),
                rows);
        html.append("<p class=\"muted\">Showing first 50 findings. Download the CSV/PDF exports for the full list.</p>");
        html.append("</section>");
    }

    private void appendGherkinSection(StringBuilder html, List<GherkinFeatureSummary> features) {
        html.append("<section><h1>7. Gherkin Features</h1>");
        if (features == null || features.isEmpty()) {
            html.append("<p class=\"muted\">No .feature files were detected.</p></section>");
            return;
        }
        features.forEach(feature -> {
            html.append("<h2>")
                    .append(escape(defaultString(feature.featureTitle(), feature.featureFile())))
                    .append("</h2>")
                    .append("<p class=\"muted\">")
                    .append(escape(defaultString(feature.featureFile())))
                    .append("</p>");
            if (feature.scenarios() == null || feature.scenarios().isEmpty()) {
                html.append("<p class=\"muted\">No scenarios listed.</p>");
                return;
            }
            feature.scenarios().forEach(scenario -> {
                html.append("<h3>")
                        .append(escape(defaultString(scenario.name(), scenario.scenarioType())))
                        .append("</h3>");
                if (scenario.steps() == null || scenario.steps().isEmpty()) {
                    html.append("<p class=\"muted\">No steps recorded.</p>");
                } else {
                    html.append("<ul>");
                    scenario.steps().forEach(step -> html.append("<li>")
                            .append(escape(step))
                            .append("</li>"));
                    html.append("</ul>");
                }
            });
        });
        html.append("</section>");
    }

    private void appendTechStackSection(StringBuilder html, ParsedDataResponse snapshot) {
        html.append("<section><h1>8. Tech Stack & Assets</h1>");
        html.append("<ul>");
        if (snapshot.buildInfo() != null) {
            html.append("<li>Maven: ")
                    .append(escape(defaultString(snapshot.buildInfo().groupId())))
                    .append(":")
                    .append(escape(defaultString(snapshot.buildInfo().artifactId())))
                    .append(":")
                    .append(escape(defaultString(snapshot.buildInfo().version())))
                    .append(" (Java ")
                    .append(escape(defaultString(snapshot.buildInfo().javaVersion())))
                    .append(")</li>");
        }
        html.append("<li>Class metadata entries: ")
                .append(snapshot.classes() != null ? snapshot.classes().size() : 0)
                .append("</li>");
        if (snapshot.assets() != null && snapshot.assets().images() != null) {
            html.append("<li>Image assets detected: ").append(snapshot.assets().images().size()).append("</li>");
        }
        MetadataDump metadataDump = snapshot.metadataDump();
        if (metadataDump != null) {
            html.append("<li>Metadata artifacts: ")
                    .append(count(metadataDump.openApiSpecs()))
                    .append(" OpenAPI, ")
                    .append(count(metadataDump.wsdlDocuments()))
                    .append(" WSDL, ")
                    .append(count(metadataDump.xsdDocuments()))
                    .append(" XSD.</li>");
        }
        html.append("</ul>");
        html.append("</section>");
    }

    private void appendFooter(StringBuilder html, ParsedDataResponse snapshot) {
        html.append("<section><h1>9. Footer</h1>")
                .append("<p>Generated by <strong>CodeDocGen</strong> for ")
                .append(escape(snapshot.repoUrl(), "repository"))
                .append(" at ")
                .append(formatTimestamp(snapshot.analyzedAt()))
                .append(".</p></section>");
    }

    private void renderTable(StringBuilder html, List<String> headers, List<List<String>> rows) {
        if (rows == null || rows.isEmpty()) {
            html.append("<p class=\"muted\">No data available for this section.</p>");
            return;
        }
        html.append("<table><thead><tr>");
        headers.forEach(header -> html.append("<th>").append(escape(header)).append("</th>"));
        html.append("</tr></thead><tbody>");
        rows.forEach(row -> {
            html.append("<tr>");
            for (int i = 0; i < headers.size(); i++) {
                String cell = i < row.size() ? row.get(i) : "";
                html.append("<td>").append(cell).append("</td>");
            }
            html.append("</tr>");
        });
        html.append("</tbody></table>");
    }

    private String formatClassesForEntity(Map<String, List<String>> classesByEntity, String entityName) {
        if (classesByEntity == null || classesByEntity.isEmpty()) {
            return "—";
        }
        List<String> classes = classesByEntity.get(entityName);
        if (classes == null || classes.isEmpty()) {
            return "—";
        }
        return String.join(", ", classes);
    }

    private String formatTimestamp(OffsetDateTime timestamp) {
        if (timestamp == null) {
            return "n/a";
        }
        return TIMESTAMP_FORMATTER.format(timestamp);
    }

    private int count(List<?> items) {
        return items == null ? 0 : items.size();
    }

    private String escape(String value) {
        return escape(value, "—");
    }

    private String escape(String value, String defaultValue) {
        String resolved = (value == null || value.isBlank()) ? defaultValue : value;
        return HtmlUtils.htmlEscape(resolved == null ? "" : resolved);
    }

    private String defaultString(String value) {
        return defaultString(value, "—");
    }

    private String defaultString(String value, String defaultValue) {
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}
