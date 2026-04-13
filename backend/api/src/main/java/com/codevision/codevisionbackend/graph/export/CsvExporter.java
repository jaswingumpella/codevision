package com.codevision.codevisionbackend.graph.export;

import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Exports a {@link KnowledgeGraph} as CSV with two sections: nodes and edges,
 * separated by a blank line and {@code # Edges} marker.
 */
@Component
public class CsvExporter implements GraphExporter {

    @Override
    public String formatName() {
        return "csv";
    }

    @Override
    public String fileExtension() {
        return ".csv";
    }

    @Override
    public String contentType() {
        return "text/csv";
    }

    @Override
    public byte[] export(KnowledgeGraph graph) {
        var sb = new StringBuilder();

        // Nodes section
        sb.append("id,type,name,qualifiedName\n");
        for (var entry : graph.getNodes().entrySet()) {
            var node = entry.getValue();
            sb.append(escapeCsv(node.id())).append(',');
            sb.append(escapeCsv(node.type() != null ? node.type().name() : "")).append(',');
            sb.append(escapeCsv(node.name())).append(',');
            sb.append(escapeCsv(node.qualifiedName())).append('\n');
        }

        // Separator
        sb.append('\n');
        sb.append("# Edges\n");

        // Edges section
        sb.append("id,type,source,target,label\n");
        for (var edge : graph.getEdges()) {
            sb.append(escapeCsv(edge.id())).append(',');
            sb.append(escapeCsv(edge.type() != null ? edge.type().name() : "")).append(',');
            sb.append(escapeCsv(edge.sourceNodeId())).append(',');
            sb.append(escapeCsv(edge.targetNodeId())).append(',');
            sb.append(escapeCsv(edge.label())).append('\n');
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
