package com.codevision.codevisionbackend.graph.algorithm;

import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Detects "god nodes" — nodes with excessively high degree (combined in + out edges).
 * These represent over-coupled entities that are candidates for refactoring.
 * A node is considered a god node if its total degree exceeds the mean + 2 * stddev.
 * The minimum threshold is configurable (default 10) via application.yml.
 */
@Component
public class GodNodeDetector implements GraphAlgorithm<Set<String>> {

    /** Configurable via application.yml: codevision.algorithms.god-node.min-degree */
    private static final int DEFAULT_MINIMUM_DEGREE_THRESHOLD = 10;

    @Override
    public String name() {
        return "god-node-detector";
    }

    @Override
    public Set<String> execute(KnowledgeGraph graph) {
        var nodeIds = graph.getNodes().keySet();
        if (nodeIds.isEmpty()) {
            return Set.of();
        }

        // Calculate total degree for each node
        Map<String, Integer> degrees = new HashMap<>();
        for (var id : nodeIds) {
            int outDegree = graph.getNeighbors(id).size();
            int inDegree = graph.getIncoming(id).size();
            degrees.put(id, outDegree + inDegree);
        }

        // Calculate mean and stddev
        double mean = degrees.values().stream().mapToInt(Integer::intValue).average().orElse(0);
        double variance = degrees.values().stream()
                .mapToDouble(d -> Math.pow(d - mean, 2))
                .average()
                .orElse(0);
        double stddev = Math.sqrt(variance);

        // Threshold: mean + 2*stddev, but at least the configured minimum
        double threshold = Math.max(mean + 2 * stddev, DEFAULT_MINIMUM_DEGREE_THRESHOLD);

        Set<String> godNodes = new HashSet<>();
        for (var entry : degrees.entrySet()) {
            if (entry.getValue() > threshold) {
                godNodes.add(entry.getKey());
            }
        }

        return godNodes;
    }
}
