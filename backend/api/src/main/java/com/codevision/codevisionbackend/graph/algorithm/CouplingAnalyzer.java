package com.codevision.codevisionbackend.graph.algorithm;

import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Analyzes afferent (incoming) and efferent (outgoing) coupling for each node.
 * High efferent = depends on many others (fragile).
 * High afferent = many others depend on it (hard to change).
 */
@Component
public class CouplingAnalyzer implements GraphAlgorithm<Map<String, CouplingAnalyzer.CouplingMetrics>> {

    /**
     * Coupling metrics for a single node.
     */
    public record CouplingMetrics(int afferent, int efferent, double instability) {}

    @Override
    public String name() {
        return "coupling-analyzer";
    }

    @Override
    public Map<String, CouplingMetrics> execute(KnowledgeGraph graph) {
        var nodeIds = graph.getNodes().keySet();
        if (nodeIds.isEmpty()) {
            return Map.of();
        }

        Map<String, Set<String>> outgoing = new HashMap<>();
        Map<String, Set<String>> incoming = new HashMap<>();

        for (var id : nodeIds) {
            outgoing.put(id, new HashSet<>());
            incoming.put(id, new HashSet<>());
        }

        for (var edge : graph.getEdges()) {
            var src = edge.sourceNodeId();
            var tgt = edge.targetNodeId();
            if (src != null && tgt != null && nodeIds.contains(src) && nodeIds.contains(tgt)) {
                outgoing.computeIfAbsent(src, k -> new HashSet<>()).add(tgt);
                incoming.computeIfAbsent(tgt, k -> new HashSet<>()).add(src);
            }
        }

        Map<String, CouplingMetrics> result = new HashMap<>();
        for (var id : nodeIds) {
            int aff = incoming.getOrDefault(id, Set.of()).size();
            int eff = outgoing.getOrDefault(id, Set.of()).size();
            double instability = (aff + eff) > 0 ? (double) eff / (aff + eff) : 0.0;
            result.put(id, new CouplingMetrics(aff, eff, instability));
        }

        return result;
    }
}
