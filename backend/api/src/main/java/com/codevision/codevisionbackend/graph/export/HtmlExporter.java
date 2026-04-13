package com.codevision.codevisionbackend.graph.export;

import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Exports a {@link KnowledgeGraph} as a standalone HTML page with embedded
 * CSS styling and inline JavaScript for search and filter functionality.
 */
@Component
public class HtmlExporter implements GraphExporter {

    @Override
    public String formatName() {
        return "html";
    }

    @Override
    public String fileExtension() {
        return ".html";
    }

    @Override
    public String contentType() {
        return "text/html";
    }

    @Override
    public byte[] export(KnowledgeGraph graph) {
        var sb = new StringBuilder();

        sb.append("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>CodeVision Knowledge Graph Export</title>
                  <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 20px; background: #f5f5f5; }
                    h1 { color: #333; }
                    .search-bar { margin: 10px 0; padding: 8px 12px; width: 300px; border: 1px solid #ccc; border-radius: 4px; font-size: 14px; }
                    .filter-select { margin: 10px 5px; padding: 8px; border: 1px solid #ccc; border-radius: 4px; }
                    table { border-collapse: collapse; width: 100%%; margin-top: 10px; background: white; box-shadow: 0 1px 3px rgba(0,0,0,0.12); }
                    th { background: #4a90d9; color: white; padding: 10px 12px; text-align: left; }
                    td { padding: 8px 12px; border-bottom: 1px solid #eee; }
                    tr:hover { background: #f0f7ff; }
                    .section { margin-top: 30px; }
                    .stats { background: white; padding: 15px; border-radius: 4px; box-shadow: 0 1px 3px rgba(0,0,0,0.12); margin-bottom: 20px; }
                  </style>
                </head>
                <body>
                  <h1>CodeVision Knowledge Graph</h1>
                  <div class="stats">
                    <strong>Nodes:</strong> %d &nbsp; | &nbsp; <strong>Edges:</strong> %d
                  </div>

                  <div class="section">
                    <h2>Nodes</h2>
                    <input type="text" class="search-bar" id="nodeSearch" placeholder="Search nodes..." oninput="filterTable('nodeTable', this.value)">
                    <table id="nodeTable">
                      <thead><tr><th>ID</th><th>Type</th><th>Name</th><th>Qualified Name</th></tr></thead>
                      <tbody>
                """.formatted(graph.nodeCount(), graph.edgeCount()));

        for (var entry : graph.getNodes().entrySet()) {
            var node = entry.getValue();
            sb.append("        <tr>");
            sb.append("<td>").append(htmlEscape(node.id())).append("</td>");
            sb.append("<td>").append(htmlEscape(node.type() != null ? node.type().name() : "")).append("</td>");
            sb.append("<td>").append(htmlEscape(node.name())).append("</td>");
            sb.append("<td>").append(htmlEscape(node.qualifiedName())).append("</td>");
            sb.append("</tr>\n");
        }

        sb.append("""
                      </tbody>
                    </table>
                  </div>

                  <div class="section">
                    <h2>Edges</h2>
                    <input type="text" class="search-bar" id="edgeSearch" placeholder="Search edges..." oninput="filterTable('edgeTable', this.value)">
                    <table id="edgeTable">
                      <thead><tr><th>ID</th><th>Type</th><th>Source</th><th>Target</th><th>Label</th></tr></thead>
                      <tbody>
                """);

        for (var edge : graph.getEdges()) {
            sb.append("        <tr>");
            sb.append("<td>").append(htmlEscape(edge.id())).append("</td>");
            sb.append("<td>").append(htmlEscape(edge.type() != null ? edge.type().name() : "")).append("</td>");
            sb.append("<td>").append(htmlEscape(edge.sourceNodeId())).append("</td>");
            sb.append("<td>").append(htmlEscape(edge.targetNodeId())).append("</td>");
            sb.append("<td>").append(htmlEscape(edge.label())).append("</td>");
            sb.append("</tr>\n");
        }

        sb.append("""
                      </tbody>
                    </table>
                  </div>

                  <script>
                    function filterTable(tableId, query) {
                      var table = document.getElementById(tableId);
                      var rows = table.getElementsByTagName('tbody')[0].getElementsByTagName('tr');
                      var lowerQuery = query.toLowerCase();
                      for (var i = 0; i < rows.length; i++) {
                        var text = rows[i].textContent.toLowerCase();
                        rows[i].style.display = text.indexOf(lowerQuery) !== -1 ? '' : 'none';
                      }
                    }
                  </script>
                </body>
                </html>
                """);

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String htmlEscape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
