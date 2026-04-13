package com.codevision.codevisionbackend.graph.algorithm;

import com.codevision.codevisionbackend.graph.KgEdge;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

/**
 * Scores each node in the knowledge graph by dependency risk.
 * <p>
 * Risk = 0.3 * normalizedFanIn + 0.3 * normalizedFanOut
 *      + 0.2 * cycleParticipation + 0.2 * normalizedTransitiveDepth
 * <p>
 * All component scores are normalized to [0, 1], producing a final risk in [0, 1].
 */
@Component
public class DependencyRiskScorer implements GraphAlgorithm<Map<String, Double>> {

    private final TarjanSccAlgorithm tarjanScc = new TarjanSccAlgorithm();
    private final long maxRuntimeSeconds;

    public DependencyRiskScorer(
            @Value("${codevision.algorithms.dependency-risk.maxRuntimeSeconds:60}") long maxRuntimeSeconds) {
        this.maxRuntimeSeconds = maxRuntimeSeconds;
    }

    /** No-arg constructor with default timeout for test convenience. */
    public DependencyRiskScorer() {
        this(60);
    }

    @Override
    public String name() {
        return "dependency-risk";
    }

    @Override
    public Map<String, Double> execute(KnowledgeGraph graph) {
        var nodeIds = graph.getNodes().keySet();
        if (nodeIds.isEmpty()) {
            return Map.of();
        }

        // Compute fan-in and fan-out per node
        Map<String, Integer> fanIn = new HashMap<>();
        Map<String, Integer> fanOut = new HashMap<>();
        for (var id : nodeIds) {
            fanIn.put(id, 0);
            fanOut.put(id, 0);
        }
        for (KgEdge edge : graph.getEdges()) {
            var src = edge.sourceNodeId();
            var tgt = edge.targetNodeId();
            if (src != null && fanOut.containsKey(src)) {
                fanOut.merge(src, 1, Integer::sum);
            }
            if (tgt != null && fanIn.containsKey(tgt)) {
                fanIn.merge(tgt, 1, Integer::sum);
            }
        }

        int maxFanIn = fanIn.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        int maxFanOut = fanOut.values().stream().mapToInt(Integer::intValue).max().orElse(0);

        // Compute cycle participation via Tarjan SCC
        List<Set<String>> sccs = tarjanScc.execute(graph);
        Set<String> cycleNodes = new HashSet<>();
        for (var scc : sccs) {
            cycleNodes.addAll(scc);
        }

        // Compute transitive depth via BFS from each node
        Map<String, Set<String>> adjacency = graph.buildAdjacencyMap();
        Map<String, Integer> transitiveDepth = new HashMap<>();
        for (var id : nodeIds) {
            transitiveDepth.put(id, bfsDepth(id, adjacency));
        }
        int maxDepth = transitiveDepth.values().stream().mapToInt(Integer::intValue).max().orElse(0);

        // Compute risk scores
        Instant deadline = Instant.now().plusSeconds(maxRuntimeSeconds);
        Map<String, Double> result = new HashMap<>();
        for (var id : nodeIds) {
            if (Instant.now().isAfter(deadline)) {
                break;
            }
            double normFanIn = maxFanIn > 0 ? (double) fanIn.get(id) / maxFanIn : 0.0;
            double normFanOut = maxFanOut > 0 ? (double) fanOut.get(id) / maxFanOut : 0.0;
            double cyclePart = cycleNodes.contains(id) ? 1.0 : 0.0;
            double normDepth = maxDepth > 0 ? (double) transitiveDepth.get(id) / maxDepth : 0.0;

            double risk = 0.3 * normFanIn + 0.3 * normFanOut + 0.2 * cyclePart + 0.2 * normDepth;
            result.put(id, risk);
        }

        return result;
    }

    /**
     * BFS from a start node, returning the maximum depth reached.
     * Uses a visited set for cycle detection.
     */
    private int bfsDepth(String start, Map<String, Set<String>> adjacency) {
        Set<String> visited = new HashSet<>();
        Deque<String> currentLevel = new ArrayDeque<>();
        currentLevel.add(start);
        visited.add(start);
        int depth = 0;

        while (!currentLevel.isEmpty()) {
            Deque<String> nextLevel = new ArrayDeque<>();
            for (var node : currentLevel) {
                for (var neighbor : adjacency.getOrDefault(node, Set.of())) {
                    if (visited.add(neighbor)) {
                        nextLevel.add(neighbor);
                    }
                }
            }
            if (!nextLevel.isEmpty()) {
                depth++;
            }
            currentLevel = nextLevel;
        }

        return depth;
    }
}
