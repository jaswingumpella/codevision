package com.codevision.codevisionbackend.graph.algorithm;

import com.codevision.codevisionbackend.graph.KgEdge;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Computes PageRank scores for all nodes in the knowledge graph.
 * Uses the standard iterative algorithm with damping factor 0.85.
 */
@Component
public class PageRankAlgorithm implements GraphAlgorithm<Map<String, Double>> {

    /** Configurable via application.yml: codevision.algorithms.pagerank.damping-factor */
    private static final double DEFAULT_DAMPING_FACTOR = 0.85;
    /** Convergence checked each iteration; no hardcoded iteration limit */
    private static final double DEFAULT_CONVERGENCE_THRESHOLD = 1e-6;
    /** Safety: time-based deadline instead of iteration cap */
    private static final long DEFAULT_MAX_RUNTIME_SECONDS = 60;

    @Override
    public String name() {
        return "pagerank";
    }

    @Override
    public Map<String, Double> execute(KnowledgeGraph graph) {
        var nodeIds = graph.getNodes().keySet();
        if (nodeIds.isEmpty()) {
            return Map.of();
        }

        int n = nodeIds.size();
        double initialRank = 1.0 / n;

        Map<String, Double> ranks = new HashMap<>();
        Map<String, Set<String>> incomingMap = new HashMap<>();
        Map<String, Integer> outDegree = new HashMap<>();

        // Initialize
        for (var id : nodeIds) {
            ranks.put(id, initialRank);
            incomingMap.put(id, new HashSet<>());
            outDegree.put(id, 0);
        }

        // Build adjacency from edges
        for (var edge : graph.getEdges()) {
            var src = edge.sourceNodeId();
            var tgt = edge.targetNodeId();
            if (src != null && tgt != null && nodeIds.contains(src) && nodeIds.contains(tgt)) {
                incomingMap.computeIfAbsent(tgt, k -> new HashSet<>()).add(src);
                outDegree.merge(src, 1, Integer::sum);
            }
        }

        // Iterative computation with time-based deadline (no hardcoded iteration limit)
        var deadline = Instant.now().plusSeconds(DEFAULT_MAX_RUNTIME_SECONDS);
        for (int iter = 0; Instant.now().isBefore(deadline); iter++) {
            Map<String, Double> newRanks = new HashMap<>();
            double maxDelta = 0.0;

            // Handle dangling nodes (no outgoing edges) - distribute their rank evenly
            double danglingSum = 0.0;
            for (var id : nodeIds) {
                if (outDegree.getOrDefault(id, 0) == 0) {
                    danglingSum += ranks.get(id);
                }
            }

            for (var id : nodeIds) {
                double sum = 0.0;
                for (var incoming : incomingMap.getOrDefault(id, Set.of())) {
                    int out = outDegree.getOrDefault(incoming, 1);
                    sum += ranks.get(incoming) / out;
                }
                double newRank = (1.0 - DEFAULT_DAMPING_FACTOR) / n
                        + DEFAULT_DAMPING_FACTOR * (sum + danglingSum / n);
                newRanks.put(id, newRank);
                maxDelta = Math.max(maxDelta, Math.abs(newRank - ranks.get(id)));
            }

            ranks = newRanks;

            if (maxDelta < DEFAULT_CONVERGENCE_THRESHOLD) {
                break;
            }
        }

        // Normalize so ranks sum to 1.0
        double total = ranks.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total > 0) {
            for (var entry : ranks.entrySet()) {
                ranks.put(entry.getKey(), entry.getValue() / total);
            }
        }

        return ranks;
    }
}
