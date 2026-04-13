package com.codevision.codevisionbackend.usecase;

import com.codevision.codevisionbackend.graph.KgEdgeType;
import com.codevision.codevisionbackend.graph.KgNodeType;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Use case: "Which high-risk code has no tests?"
 * Identifies classes that are not covered by any test case.
 */
@Service
public class TestGapAnalysisService {

    public record TestGapReport(List<String> untestedClasses, int totalClasses, double coveragePercentage) {}

    public TestGapReport analyze(KnowledgeGraph graph) {
        var classIds = graph.nodesOfType(KgNodeType.CLASS);
        if (classIds.isEmpty()) {
            return new TestGapReport(List.of(), 0, 100.0);
        }

        // Find all classes that have at least one test pointing to them
        Set<String> testedClassIds = new HashSet<>();
        for (var edge : graph.edgesOfType(KgEdgeType.TESTS)) {
            if (edge.targetNodeId() != null) {
                testedClassIds.add(edge.targetNodeId());
            }
        }

        var untestedNames = classIds.stream()
                .filter(id -> !testedClassIds.contains(id))
                .map(id -> {
                    var node = graph.getNode(id);
                    return node != null ? node.name() : id;
                })
                .sorted()
                .toList();

        double coverage = classIds.isEmpty() ? 100.0
                : (double) testedClassIds.size() / classIds.size() * 100;

        return new TestGapReport(untestedNames, classIds.size(), coverage);
    }
}
