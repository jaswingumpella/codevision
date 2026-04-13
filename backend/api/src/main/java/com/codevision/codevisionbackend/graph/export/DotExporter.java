package com.codevision.codevisionbackend.graph.export;

import com.codevision.codevisionbackend.graph.KgNodeType;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Exports a {@link KnowledgeGraph} in Graphviz DOT format with type-based
 * node shapes.
 */
@Component
public class DotExporter implements GraphExporter {

    @Override
    public String formatName() {
        return "dot";
    }

    @Override
    public String fileExtension() {
        return ".dot";
    }

    @Override
    public String contentType() {
        return "text/plain";
    }

    @Override
    public byte[] export(KnowledgeGraph graph) {
        var sb = new StringBuilder();
        sb.append("digraph KnowledgeGraph {\n");
        sb.append("  rankdir=TB;\n");

        for (var entry : graph.getNodes().entrySet()) {
            var node = entry.getValue();
            var shape = shapeFor(node.type());
            var label = node.name() != null ? node.name() : node.id();
            sb.append("  \"").append(dotEscape(node.id()))
              .append("\" [label=\"").append(dotEscape(label))
              .append("\", shape=").append(shape).append("];\n");
        }

        for (var edge : graph.getEdges()) {
            var label = edge.type() != null ? edge.type().name() : "";
            sb.append("  \"").append(dotEscape(edge.sourceNodeId()))
              .append("\" -> \"").append(dotEscape(edge.targetNodeId()))
              .append("\" [label=\"").append(dotEscape(label)).append("\"];\n");
        }

        sb.append("}\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String shapeFor(KgNodeType type) {
        if (type == null) {
            return "ellipse";
        }
        return switch (type) {
            case CLASS, RECORD, STRUCT, ENUM -> "box";
            case INTERFACE, TRAIT, PROTOCOL -> "diamond";
            case METHOD, CONSTRUCTOR, FUNCTION, LAMBDA, CLOSURE -> "ellipse";
            case PACKAGE, MODULE, NAMESPACE -> "folder";
            case FILE -> "note";
            default -> "ellipse";
        };
    }

    private String dotEscape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
