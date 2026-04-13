package com.codevision.codevisionbackend.graph.algorithm;

import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Computes betweenness centrality for all nodes using Brandes' algorithm.
 * Betweenness measures how often a node lies on shortest paths between
 * other nodes — high betweenness indicates bridge/bottleneck nodes.
 */
@Component
public class BetweennessCentralityAlgorithm implements GraphAlgorithm<Map<String, Double>> {

    @Override
    public String name() {
        return "betweenness-centrality";
    }

    @Override
    public Map<String, Double> execute(KnowledgeGraph graph) {
        var nodeIds = new ArrayList<>(graph.getNodes().keySet());
        if (nodeIds.isEmpty()) {
            return Map.of();
        }

        Map<String, Double> betweenness = new HashMap<>();
        for (var id : nodeIds) {
            betweenness.put(id, 0.0);
        }

        // Build adjacency map
        Map<String, Set<String>> adjacency = graph.buildAdjacencyMap();

        // Brandes' algorithm
        for (var source : nodeIds) {
            Deque<String> stack = new ArrayDeque<>();
            Map<String, List<String>> predecessors = new HashMap<>();
            Map<String, Integer> sigma = new HashMap<>();
            Map<String, Integer> dist = new HashMap<>();

            for (var id : nodeIds) {
                predecessors.put(id, new ArrayList<>());
                sigma.put(id, 0);
                dist.put(id, -1);
            }

            sigma.put(source, 1);
            dist.put(source, 0);
            Deque<String> queue = new ArrayDeque<>();
            queue.add(source);

            // BFS
            while (!queue.isEmpty()) {
                var v = queue.poll();
                stack.push(v);
                int dv = dist.get(v);
                for (var w : adjacency.getOrDefault(v, Set.of())) {
                    if (dist.get(w) == null || dist.get(w) < 0) {
                        dist.put(w, dv + 1);
                        queue.add(w);
                    }
                    if (dist.get(w) != null && dist.get(w) == dv + 1) {
                        sigma.merge(w, sigma.get(v), Integer::sum);
                        predecessors.get(w).add(v);
                    }
                }
            }

            // Back-propagation
            Map<String, Double> delta = new HashMap<>();
            for (var id : nodeIds) {
                delta.put(id, 0.0);
            }

            while (!stack.isEmpty()) {
                var w = stack.pop();
                for (var v : predecessors.get(w)) {
                    int sigmaW = sigma.getOrDefault(w, 1);
                    if (sigmaW > 0) {
                        double d = (double) sigma.get(v) / sigmaW * (1.0 + delta.get(w));
                        delta.merge(v, d, Double::sum);
                    }
                }
                if (!w.equals(source)) {
                    betweenness.merge(w, delta.get(w), Double::sum);
                }
            }
        }

        return betweenness;
    }
}
