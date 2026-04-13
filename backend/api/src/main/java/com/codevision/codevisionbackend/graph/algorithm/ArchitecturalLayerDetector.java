package com.codevision.codevisionbackend.graph.algorithm;

import com.codevision.codevisionbackend.graph.KgNodeType;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Detects architectural layers (PRESENTATION, BUSINESS, DATA, DOMAIN)
 * by analyzing node types and their position in the dependency graph.
 * Endpoints are PRESENTATION, database entities are DATA, and the rest
 * is inferred from their connections.
 */
@Component
public class ArchitecturalLayerDetector implements GraphAlgorithm<Map<String, String>> {

    private static final Set<KgNodeType> PRESENTATION_TYPES = Set.of(
            KgNodeType.ENDPOINT
    );

    private static final Set<KgNodeType> DATA_TYPES = Set.of(
            KgNodeType.DATABASE_ENTITY,
            KgNodeType.DATABASE_COLUMN
    );

    @Override
    public String name() {
        return "architectural-layer-detector";
    }

    @Override
    public Map<String, String> execute(KnowledgeGraph graph) {
        var nodes = graph.getNodes();
        if (nodes.isEmpty()) {
            return Map.of();
        }

        Map<String, String> layers = new HashMap<>();

        // Phase 1: Assign by type
        for (var entry : nodes.entrySet()) {
            var nodeId = entry.getKey();
            var node = entry.getValue();

            if (PRESENTATION_TYPES.contains(node.type())) {
                layers.put(nodeId, "PRESENTATION");
            } else if (DATA_TYPES.contains(node.type())) {
                layers.put(nodeId, "DATA");
            }
        }

        // Phase 2: Infer remaining layers by position in graph
        // Nodes directly called by PRESENTATION that call DATA are BUSINESS
        // Nodes that only interact with other BUSINESS/DOMAIN nodes are DOMAIN
        for (var entry : nodes.entrySet()) {
            var nodeId = entry.getKey();
            if (layers.containsKey(nodeId)) {
                continue;
            }

            boolean calledByPresentation = false;
            boolean callsData = false;

            // Check incoming edges
            for (var edge : graph.getIncoming(nodeId)) {
                var sourceLayer = layers.get(edge.sourceNodeId());
                if ("PRESENTATION".equals(sourceLayer)) {
                    calledByPresentation = true;
                }
            }

            // Check outgoing edges
            for (var edge : graph.getNeighbors(nodeId)) {
                var targetLayer = layers.get(edge.targetNodeId());
                if ("DATA".equals(targetLayer)) {
                    callsData = true;
                }
            }

            if (calledByPresentation || callsData) {
                layers.put(nodeId, "BUSINESS");
            } else {
                layers.put(nodeId, "DOMAIN");
            }
        }

        return layers;
    }
}
