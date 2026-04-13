package com.codevision.codevisionbackend.graph.export;

import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Exports a {@link KnowledgeGraph} in the standard GraphML XML format,
 * compliant with the graphml.graphdrawing.org namespace.
 */
@Component
public class GraphMLExporter implements GraphExporter {

    @Override
    public String formatName() {
        return "graphml";
    }

    @Override
    public String fileExtension() {
        return ".graphml";
    }

    @Override
    public String contentType() {
        return "application/xml";
    }

    @Override
    public byte[] export(KnowledgeGraph graph) {
        var sb = new StringBuilder();

        sb.append("""
                <?xml version="1.0" encoding="UTF-8"?>
                <graphml xmlns="http://graphml.graphdrawing.org/xmlns"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://graphml.graphdrawing.org/xmlns http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd">
                  <key id="nodeType" for="node" attr.name="type" attr.type="string"/>
                  <key id="nodeName" for="node" attr.name="name" attr.type="string"/>
                  <key id="nodeQualifiedName" for="node" attr.name="qualifiedName" attr.type="string"/>
                  <key id="edgeType" for="edge" attr.name="type" attr.type="string"/>
                  <key id="edgeLabel" for="edge" attr.name="label" attr.type="string"/>
                  <graph id="G" edgedefault="directed">
                """);

        for (var entry : graph.getNodes().entrySet()) {
            var node = entry.getValue();
            sb.append("    <node id=\"").append(xmlEscape(node.id())).append("\">\n");
            sb.append("      <data key=\"nodeType\">").append(xmlEscape(node.type() != null ? node.type().name() : "")).append("</data>\n");
            sb.append("      <data key=\"nodeName\">").append(xmlEscape(node.name())).append("</data>\n");
            sb.append("      <data key=\"nodeQualifiedName\">").append(xmlEscape(node.qualifiedName())).append("</data>\n");
            sb.append("    </node>\n");
        }

        for (var edge : graph.getEdges()) {
            sb.append("    <edge id=\"").append(xmlEscape(edge.id()))
              .append("\" source=\"").append(xmlEscape(edge.sourceNodeId()))
              .append("\" target=\"").append(xmlEscape(edge.targetNodeId()))
              .append("\">\n");
            sb.append("      <data key=\"edgeType\">").append(xmlEscape(edge.type() != null ? edge.type().name() : "")).append("</data>\n");
            sb.append("      <data key=\"edgeLabel\">").append(xmlEscape(edge.label())).append("</data>\n");
            sb.append("    </edge>\n");
        }

        sb.append("  </graph>\n");
        sb.append("</graphml>\n");

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String xmlEscape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
