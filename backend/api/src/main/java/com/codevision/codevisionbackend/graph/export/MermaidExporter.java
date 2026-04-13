package com.codevision.codevisionbackend.graph.export;

import com.codevision.codevisionbackend.graph.KgNodeType;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Exports a {@link KnowledgeGraph} in Mermaid flowchart format (flowchart TD).
 * Nodes are rendered with type-based shapes.
 */
@Component
public class MermaidExporter implements GraphExporter {

    @Override
    public String formatName() {
        return "mermaid";
    }

    @Override
    public String fileExtension() {
        return ".mmd";
    }

    @Override
    public String contentType() {
        return "text/plain";
    }

    @Override
    public byte[] export(KnowledgeGraph graph) {
        var sb = new StringBuilder();
        sb.append("flowchart TD\n");

        for (var entry : graph.getNodes().entrySet()) {
            var node = entry.getValue();
            var id = sanitizeId(node.id());
            var label = mermaidEscape(node.name() != null ? node.name() : node.id());
            var shape = shapeFor(node.type(), id, label);
            sb.append("  ").append(shape).append('\n');
        }

        for (var edge : graph.getEdges()) {
            var sourceId = sanitizeId(edge.sourceNodeId());
            var targetId = sanitizeId(edge.targetNodeId());
            var label = edge.type() != null ? edge.type().name() : "";
            sb.append("  ").append(sourceId)
              .append(" -->|").append(mermaidEscape(label)).append("| ")
              .append(targetId).append('\n');
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String shapeFor(KgNodeType type, String id, String label) {
        if (type == null) {
            return id + "[" + label + "]";
        }
        return switch (type) {
            case CLASS, RECORD, STRUCT, ENUM -> id + "[" + label + "]";
            case INTERFACE, TRAIT, PROTOCOL -> id + "{" + label + "}";
            case METHOD, CONSTRUCTOR, FUNCTION, LAMBDA, CLOSURE -> id + "(" + label + ")";
            case PACKAGE, MODULE, NAMESPACE -> id + "[[" + label + "]]";
            default -> id + "[" + label + "]";
        };
    }

    private String sanitizeId(String id) {
        if (id == null) {
            return "null";
        }
        return id.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    private String mermaidEscape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\"", "#quot;");
    }
}
