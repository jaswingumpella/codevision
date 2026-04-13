package com.codevision.codevisionbackend.graph.export;

import com.codevision.codevisionbackend.graph.KgNodeType;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * Exports a {@link KnowledgeGraph} as a simple SVG document with nodes rendered
 * as circles or rectangles and edges as lines. Node positions are derived from
 * a hash-based layout to provide deterministic placement.
 */
@Component
public class SvgExporter implements GraphExporter {

    private final SvgExportProperties properties;

    public SvgExporter(SvgExportProperties properties) {
        this.properties = properties;
    }

    @Override
    public String formatName() {
        return "svg";
    }

    @Override
    public String fileExtension() {
        return ".svg";
    }

    @Override
    public String contentType() {
        return "image/svg+xml";
    }

    @Override
    public byte[] export(KnowledgeGraph graph) {
        var sb = new StringBuilder();
        var positions = new HashMap<String, int[]>();

        var canvasWidth = properties.canvasWidth();
        var canvasHeight = properties.canvasHeight();
        var nodeRadius = properties.nodeRadius();
        var rectWidth = properties.rectWidth();
        var rectHeight = properties.rectHeight();

        // Calculate positions using hashCode for deterministic layout
        var margin = 60;
        var usableWidth = canvasWidth - 2 * margin;
        var usableHeight = canvasHeight - 2 * margin;

        for (var entry : graph.getNodes().entrySet()) {
            var id = entry.getKey();
            var hash = id.hashCode();
            var x = margin + Math.abs(hash % usableWidth);
            var y = margin + Math.abs((hash >>> 16) % usableHeight);
            positions.put(id, new int[]{x, y});
        }

        sb.append("""
                <?xml version="1.0" encoding="UTF-8"?>
                <svg xmlns="http://www.w3.org/2000/svg" width="%d" height="%d" viewBox="0 0 %d %d">
                  <style>
                    .node-label { font-family: sans-serif; font-size: 10px; text-anchor: middle; }
                    .edge-line { stroke: #999; stroke-width: 1; marker-end: url(#arrow); }
                    .node-rect { fill: #4a90d9; stroke: #2c5f8a; stroke-width: 1.5; }
                    .node-circle { fill: #67b86a; stroke: #3d7a3f; stroke-width: 1.5; }
                    .node-diamond { fill: #e6a23c; stroke: #b87d2a; stroke-width: 1.5; }
                  </style>
                  <defs>
                    <marker id="arrow" viewBox="0 0 10 10" refX="10" refY="5"
                            markerWidth="6" markerHeight="6" orient="auto-start-reverse">
                      <path d="M 0 0 L 10 5 L 0 10 z" fill="#999"/>
                    </marker>
                  </defs>
                """.formatted(canvasWidth, canvasHeight, canvasWidth, canvasHeight));

        // Draw edges
        for (var edge : graph.getEdges()) {
            var src = positions.get(edge.sourceNodeId());
            var tgt = positions.get(edge.targetNodeId());
            if (src != null && tgt != null) {
                sb.append("  <line class=\"edge-line\" x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\"/>\n"
                        .formatted(src[0], src[1], tgt[0], tgt[1]));
            }
        }

        // Draw nodes
        for (var entry : graph.getNodes().entrySet()) {
            var node = entry.getValue();
            var pos = positions.get(node.id());
            if (pos == null) continue;

            var label = xmlEscape(node.name() != null ? node.name() : node.id());
            if (isRectangleType(node.type())) {
                sb.append("  <rect class=\"node-rect\" x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" rx=\"4\"/>\n"
                        .formatted(pos[0] - rectWidth / 2, pos[1] - rectHeight / 2, rectWidth, rectHeight));
            } else if (isDiamondType(node.type())) {
                sb.append("  <polygon class=\"node-diamond\" points=\"%d,%d %d,%d %d,%d %d,%d\"/>\n"
                        .formatted(pos[0], pos[1] - nodeRadius,
                                   pos[0] + nodeRadius, pos[1],
                                   pos[0], pos[1] + nodeRadius,
                                   pos[0] - nodeRadius, pos[1]));
            } else {
                sb.append("  <circle class=\"node-circle\" cx=\"%d\" cy=\"%d\" r=\"%d\"/>\n"
                        .formatted(pos[0], pos[1], nodeRadius));
            }
            sb.append("  <text class=\"node-label\" x=\"%d\" y=\"%d\">%s</text>\n"
                    .formatted(pos[0], pos[1] + nodeRadius + 14, label));
        }

        sb.append("</svg>\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private boolean isRectangleType(KgNodeType type) {
        return type == KgNodeType.CLASS || type == KgNodeType.RECORD
                || type == KgNodeType.STRUCT || type == KgNodeType.ENUM;
    }

    private boolean isDiamondType(KgNodeType type) {
        return type == KgNodeType.INTERFACE || type == KgNodeType.TRAIT
                || type == KgNodeType.PROTOCOL;
    }

    private String xmlEscape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
