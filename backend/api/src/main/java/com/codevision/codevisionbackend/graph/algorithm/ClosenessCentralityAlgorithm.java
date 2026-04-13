package com.codevision.codevisionbackend.graph.algorithm;

import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

/**
 * Computes closeness centrality for all nodes in the knowledge graph.
 *
 * <p>Closeness centrality measures how close a node is to all other reachable
 * nodes. Nodes with high closeness can reach every other node more quickly
 * and are therefore more central.</p>
 *
 * <p>Uses Wasserman-Faust normalization to handle disconnected graphs:
 * <pre>
 *   C(u) = ((reachable - 1) / (N - 1)) * ((reachable - 1) / sum_of_distances)
 * </pre>
 * where {@code reachable} is the number of nodes reachable from u (including u),
 * and N is the total number of nodes in the graph.</p>
 *
 * <p>The graph is treated as undirected: each directed edge creates adjacency
 * in both directions.</p>
 */
@Component
public class ClosenessCentralityAlgorithm implements GraphAlgorithm<Map<String, Double>> {

    private final long maxRuntimeSeconds;

    public ClosenessCentralityAlgorithm(
            @Value("${codevision.algorithms.closeness-centrality.maxRuntimeSeconds:60}") long maxRuntimeSeconds) {
        this.maxRuntimeSeconds = maxRuntimeSeconds;
    }

    /** No-arg constructor with defaults for test convenience. */
    public ClosenessCentralityAlgorithm() {
        this(60);
    }

    @Override
    public String name() {
        return "closeness-centrality";
    }

    @Override
    public Map<String, Double> execute(KnowledgeGraph graph) {
        var nodeIds = new ArrayList<>(graph.getNodes().keySet());
        if (nodeIds.isEmpty()) {
            return Map.of();
        }

        int n = nodeIds.size();

        // Build undirected adjacency from all edges
        var adjacency = new HashMap<String, Set<String>>();
        for (var edge : graph.getEdges()) {
            var src = edge.sourceNodeId();
            var tgt = edge.targetNodeId();
            if (src != null && tgt != null && !src.equals(tgt)) {
                adjacency.computeIfAbsent(src, k -> new HashSet<>()).add(tgt);
                adjacency.computeIfAbsent(tgt, k -> new HashSet<>()).add(src);
            }
        }

        var deadline = Instant.now().plusSeconds(maxRuntimeSeconds);
        var result = new HashMap<String, Double>();

        for (var nodeId : nodeIds) {
            if (Instant.now().isAfter(deadline)) {
                // Fill remaining nodes with 0.0 on timeout
                for (var remaining : nodeIds) {
                    result.putIfAbsent(remaining, 0.0);
                }
                break;
            }

            if (n == 1) {
                result.put(nodeId, 0.0);
                continue;
            }

            var neighbors = adjacency.getOrDefault(nodeId, Set.of());
            if (neighbors.isEmpty()) {
                result.put(nodeId, 0.0);
                continue;
            }

            // BFS from this node
            var distances = bfs(nodeId, adjacency);

            long sumDistances = 0;
            int reachable = 0;
            for (var dist : distances.values()) {
                if (dist > 0) {
                    sumDistances += dist;
                    reachable++;
                }
            }

            if (reachable == 0 || sumDistances == 0) {
                result.put(nodeId, 0.0);
            } else {
                // Wasserman-Faust normalization
                double closeness = ((double) reachable / (n - 1))
                        * ((double) reachable / sumDistances);
                result.put(nodeId, closeness);
            }
        }

        return result;
    }

    /**
     * Performs BFS from the source node and returns distances to all reachable nodes.
     */
    private Map<String, Integer> bfs(String source, Map<String, Set<String>> adjacency) {
        var distances = new HashMap<String, Integer>();
        var visited = new HashSet<String>();
        var queue = new ArrayDeque<String>();

        distances.put(source, 0);
        visited.add(source);
        queue.add(source);

        while (!queue.isEmpty()) {
            var current = queue.poll();
            int currentDist = distances.get(current);

            for (var neighbor : adjacency.getOrDefault(current, Set.of())) {
                if (visited.add(neighbor)) {
                    distances.put(neighbor, currentDist + 1);
                    queue.add(neighbor);
                }
            }
        }

        return distances;
    }
}
