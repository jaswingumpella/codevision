package com.codevision.codevisionbackend.usecase;

import com.codevision.codevisionbackend.graph.KgNodeType;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import com.codevision.codevisionbackend.graph.algorithm.ImpactAnalyzer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Use case: "Plan a library migration — what breaks if I swap library X?"
 * Uses impact analysis to show what depends on a given dependency artifact.
 */
@Service
public class MigrationPlannerService {

    private final ImpactAnalyzer impactAnalyzer = new ImpactAnalyzer();

    public record MigrationPlan(String targetArtifact, int impactedCount, List<String> impactedNames) {}

    public MigrationPlan plan(KnowledgeGraph graph, String artifactNodeId) {
        var impact = impactAnalyzer.analyzeImpact(graph, artifactNodeId);
        var names = impact.stream()
                .filter(id -> !id.equals(artifactNodeId))
                .map(id -> {
                    var node = graph.getNode(id);
                    return node != null ? node.name() : id;
                })
                .sorted()
                .toList();

        return new MigrationPlan(artifactNodeId, names.size(), names);
    }
}
