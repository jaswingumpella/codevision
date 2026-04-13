package com.codevision.codevisionbackend.usecase;

import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import com.codevision.codevisionbackend.graph.algorithm.CouplingAnalyzer;
import com.codevision.codevisionbackend.graph.algorithm.GodNodeDetector;
import com.codevision.codevisionbackend.graph.algorithm.TarjanSccAlgorithm;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Use case: "How healthy is my architecture? 0-100 score."
 * Combines coupling, cycles, and god-node analysis into a single health score.
 */
@Service
public class ArchitectureHealthService {

    private final CouplingAnalyzer couplingAnalyzer = new CouplingAnalyzer();
    private final TarjanSccAlgorithm sccAlgorithm = new TarjanSccAlgorithm();
    private final GodNodeDetector godNodeDetector = new GodNodeDetector();

    public record HealthReport(int score, List<HealthComponent> components) {}
    public record HealthComponent(String name, int score, String detail) {}

    public HealthReport score(KnowledgeGraph graph) {
        if (graph.nodeCount() == 0) {
            return new HealthReport(0, List.of());
        }

        var components = new ArrayList<HealthComponent>();

        // 1. Coupling score (0-30): penalize high instability
        var couplingMetrics = couplingAnalyzer.execute(graph);
        double avgInstability = couplingMetrics.values().stream()
                .mapToDouble(CouplingAnalyzer.CouplingMetrics::instability)
                .average().orElse(0.5);
        int couplingScore = (int) ((1.0 - avgInstability) * 30);
        components.add(new HealthComponent("coupling", couplingScore,
                String.format("Average instability: %.2f", avgInstability)));

        // 2. Cycle score (0-30): penalize circular dependencies
        var cycles = sccAlgorithm.execute(graph);
        int cycleScore = cycles.isEmpty() ? 30 : Math.max(0, 30 - cycles.size() * 5);
        components.add(new HealthComponent("cycles", cycleScore,
                cycles.size() + " circular dependency groups"));

        // 3. God node score (0-20): penalize over-coupled entities
        var godNodes = godNodeDetector.execute(graph);
        int godScore = godNodes.isEmpty() ? 20 : Math.max(0, 20 - godNodes.size() * 5);
        components.add(new HealthComponent("god-nodes", godScore,
                godNodes.size() + " god nodes detected"));

        // 4. Structure score (0-20): reward having endpoints + services + data
        int structureScore = 20; // Base score; deducted for missing layers
        boolean hasEndpoints = !graph.nodesOfType(com.codevision.codevisionbackend.graph.KgNodeType.ENDPOINT).isEmpty();
        boolean hasData = !graph.nodesOfType(com.codevision.codevisionbackend.graph.KgNodeType.DATABASE_ENTITY).isEmpty();
        if (!hasEndpoints) structureScore -= 10;
        if (!hasData) structureScore -= 5;
        components.add(new HealthComponent("structure", Math.max(0, structureScore),
                "endpoints=" + hasEndpoints + ", data=" + hasData));

        int total = components.stream().mapToInt(HealthComponent::score).sum();
        return new HealthReport(Math.min(100, total), components);
    }
}
