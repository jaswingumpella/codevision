package com.codevision.codevisionbackend.usecase;

import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import com.codevision.codevisionbackend.graph.algorithm.DeadCodeDetector;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Use case: "Show me dead code I can safely delete."
 * Combines DeadCodeDetector results with node metadata to produce an actionable report.
 */
@Service
public class DeadCodeReportService {

    private final DeadCodeDetector detector = new DeadCodeDetector();

    public record DeadCodeReport(
            List<String> deadNodes,
            int totalNodes,
            double deadPercentage
    ) {}

    public DeadCodeReport generate(KnowledgeGraph graph) {
        if (graph.nodeCount() == 0) {
            return new DeadCodeReport(List.of(), 0, 0.0);
        }

        var deadNodeIds = detector.execute(graph);
        var deadNames = deadNodeIds.stream()
                .map(id -> {
                    var node = graph.getNode(id);
                    return node != null ? node.name() + " (" + node.type() + ")" : id;
                })
                .sorted()
                .toList();

        double percentage = (double) deadNodeIds.size() / graph.nodeCount() * 100;

        return new DeadCodeReport(deadNames, graph.nodeCount(), percentage);
    }
}
