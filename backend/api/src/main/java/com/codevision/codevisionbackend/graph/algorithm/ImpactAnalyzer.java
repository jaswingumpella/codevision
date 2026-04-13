package com.codevision.codevisionbackend.graph.algorithm;

import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Analyzes the impact of changing a given node by computing its forward
 * transitive closure (all nodes reachable from it via outgoing edges).
 * Answers: "If I change X, what else might break?"
 */
@Component
public class ImpactAnalyzer implements GraphAlgorithm<Map<String, Set<String>>> {

    private static final long DEFAULT_MAX_RUNTIME_SECONDS = 120;

    @Override
    public String name() {
        return "impact-analyzer";
    }

    /**
     * Returns a map of each node to its forward transitive closure.
     * Uses a time-based deadline to prevent runaway computation on large graphs.
     * For targeted use, prefer {@link #analyzeImpact(KnowledgeGraph, String)}.
     */
    @Override
    public Map<String, Set<String>> execute(KnowledgeGraph graph) {
        var result = new HashMap<String, Set<String>>();
        var deadline = Instant.now().plusSeconds(DEFAULT_MAX_RUNTIME_SECONDS);
        for (var nodeId : graph.getNodes().keySet()) {
            if (Instant.now().isAfter(deadline)) {
                break;
            }
            result.put(nodeId, analyzeImpact(graph, nodeId));
        }
        return result;
    }

    /**
     * Computes the forward transitive closure from a single node.
     * Uses BFS with cycle detection.
     */
    public Set<String> analyzeImpact(KnowledgeGraph graph, String nodeId) {
        if (graph.getNode(nodeId) == null) {
            return Set.of();
        }

        Set<String> impacted = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(nodeId);
        impacted.add(nodeId);

        while (!queue.isEmpty()) {
            var current = queue.poll();
            for (var edge : graph.getNeighbors(current)) {
                var target = edge.targetNodeId();
                if (target != null && impacted.add(target)) {
                    queue.add(target);
                }
            }
        }

        return impacted;
    }
}
