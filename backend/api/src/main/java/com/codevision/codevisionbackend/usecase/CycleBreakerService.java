package com.codevision.codevisionbackend.usecase;

import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import com.codevision.codevisionbackend.graph.algorithm.TarjanSccAlgorithm;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Use case: "Find circular dependencies and suggest how to break them."
 * Uses Tarjan SCC to find cycles and suggests refactoring strategies.
 */
@Service
public class CycleBreakerService {

    private final TarjanSccAlgorithm sccAlgorithm = new TarjanSccAlgorithm();

    public record CycleReport(List<CycleInfo> cycles, int totalCycles) {}
    public record CycleInfo(Set<String> nodeIds, List<String> nodeNames, String suggestion) {}

    public CycleReport analyze(KnowledgeGraph graph) {
        var sccs = sccAlgorithm.execute(graph);
        if (sccs.isEmpty()) {
            return new CycleReport(List.of(), 0);
        }

        var cycles = new ArrayList<CycleInfo>();
        for (var scc : sccs) {
            var names = scc.stream()
                    .map(id -> {
                        var node = graph.getNode(id);
                        return node != null ? node.name() : id;
                    })
                    .sorted()
                    .toList();

            var suggestion = generateSuggestion(scc, graph);
            cycles.add(new CycleInfo(scc, names, suggestion));
        }

        return new CycleReport(cycles, cycles.size());
    }

    private String generateSuggestion(Set<String> scc, KnowledgeGraph graph) {
        if (scc.size() == 2) {
            return "Extract a common interface or use dependency inversion to break this bidirectional dependency.";
        }
        return "Consider introducing an interface or event-based communication to decouple " +
                scc.size() + " circularly dependent components.";
    }
}
