package com.codevision.codevisionbackend.graph.export;

import com.codevision.codevisionbackend.graph.KgEdge;
import com.codevision.codevisionbackend.graph.KgNode;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Exports a {@link KnowledgeGraph} as a JSON document containing two arrays:
 * {@code nodes} and {@code edges}.
 */
@Component
public class JsonExporter implements GraphExporter {

    private final ObjectMapper objectMapper;

    public JsonExporter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String formatName() {
        return "json";
    }

    @Override
    public String fileExtension() {
        return ".json";
    }

    @Override
    public String contentType() {
        return "application/json";
    }

    @Override
    public byte[] export(KnowledgeGraph graph) {
        var result = new LinkedHashMap<String, Object>();
        result.put("nodes", buildNodes(graph));
        result.put("edges", buildEdges(graph));
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(result);
        } catch (JsonProcessingException e) {
            throw new ExportException("Failed to export graph as JSON", e);
        }
    }

    private List<Map<String, Object>> buildNodes(KnowledgeGraph graph) {
        var nodes = new ArrayList<Map<String, Object>>();
        for (var entry : graph.getNodes().entrySet()) {
            var node = entry.getValue();
            var map = new LinkedHashMap<String, Object>();
            map.put("id", node.id());
            map.put("type", node.type() != null ? node.type().name() : null);
            map.put("name", node.name());
            map.put("qualifiedName", node.qualifiedName());
            nodes.add(map);
        }
        return nodes;
    }

    private List<Map<String, Object>> buildEdges(KnowledgeGraph graph) {
        var edges = new ArrayList<Map<String, Object>>();
        for (var edge : graph.getEdges()) {
            var map = new LinkedHashMap<String, Object>();
            map.put("id", edge.id());
            map.put("type", edge.type() != null ? edge.type().name() : null);
            map.put("source", edge.sourceNodeId());
            map.put("target", edge.targetNodeId());
            map.put("label", edge.label());
            edges.add(map);
        }
        return edges;
    }
}
