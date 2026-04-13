package com.codevision.codevisionbackend.graph.export;

import com.codevision.codevisionbackend.graph.KgEdgeType;
import com.codevision.codevisionbackend.graph.KgNodeType;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Exports a {@link KnowledgeGraph} in PlantUML class diagram format.
 */
@Component
public class PlantUmlExporter implements GraphExporter {

    @Override
    public String formatName() {
        return "plantuml";
    }

    @Override
    public String fileExtension() {
        return ".puml";
    }

    @Override
    public String contentType() {
        return "text/plain";
    }

    @Override
    public byte[] export(KnowledgeGraph graph) {
        var sb = new StringBuilder();
        sb.append("@startuml\n");
        sb.append("skinparam classAttributeIconSize 0\n\n");

        // Declare nodes
        for (var entry : graph.getNodes().entrySet()) {
            var node = entry.getValue();
            var keyword = keywordFor(node.type());
            var name = pumlEscape(node.name() != null ? node.name() : node.id());
            sb.append(keyword).append(' ').append(name);

            // Add qualifiedName as stereotype
            if (node.qualifiedName() != null && !node.qualifiedName().equals(node.name())) {
                sb.append(" <<").append(pumlEscape(node.type() != null ? node.type().name() : "node")).append(">>");
            }
            sb.append(" {\n");
            sb.append("}\n\n");
        }

        // Declare edges
        for (var edge : graph.getEdges()) {
            var sourceName = nodeNameOrId(graph, edge.sourceNodeId());
            var targetName = nodeNameOrId(graph, edge.targetNodeId());
            var arrow = arrowFor(edge.type());
            var label = edge.type() != null ? edge.type().name() : "";
            sb.append(pumlEscape(sourceName))
              .append(' ').append(arrow).append(' ')
              .append(pumlEscape(targetName))
              .append(" : ").append(label).append('\n');
        }

        sb.append("\n@enduml\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String nodeNameOrId(KnowledgeGraph graph, String nodeId) {
        var node = graph.getNode(nodeId);
        if (node != null && node.name() != null) {
            return node.name();
        }
        return nodeId != null ? nodeId : "unknown";
    }

    private String keywordFor(KgNodeType type) {
        if (type == null) {
            return "class";
        }
        return switch (type) {
            case INTERFACE, TRAIT, PROTOCOL -> "interface";
            case ENUM -> "enum";
            case ANNOTATION_TYPE -> "annotation";
            default -> "class";
        };
    }

    private String arrowFor(KgEdgeType type) {
        if (type == null) {
            return "-->";
        }
        return switch (type) {
            case EXTENDS, INHERITS_FROM -> "--|>";
            case IMPLEMENTS, REALIZES -> "..|>";
            case CONTAINS, DECLARES, MEMBER_OF -> "*--";
            case USES, REFERENCES -> "..>";
            default -> "-->";
        };
    }

    private String pumlEscape(String value) {
        if (value == null) {
            return "unknown";
        }
        // PlantUML identifiers: replace spaces and special chars
        return value.replaceAll("[^a-zA-Z0-9_.]", "_");
    }
}
