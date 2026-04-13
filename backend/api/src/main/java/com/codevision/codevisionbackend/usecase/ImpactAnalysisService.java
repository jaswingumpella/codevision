package com.codevision.codevisionbackend.usecase;

import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import com.codevision.codevisionbackend.graph.algorithm.ImpactAnalyzer;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Use case: "If I change X, what might break?"
 * Wraps ImpactAnalyzer to provide actionable impact analysis results.
 */
@Service
public class ImpactAnalysisService {

    private final ImpactAnalyzer analyzer = new ImpactAnalyzer();

    public record ImpactResult(
            String sourceNodeId,
            Set<String> impactedNodeIds,
            int impactCount,
            List<String> impactedNames
    ) {}

    public ImpactResult analyzeImpact(KnowledgeGraph graph, String nodeId) {
        var impacted = analyzer.analyzeImpact(graph, nodeId);
        var names = impacted.stream()
                .map(id -> {
                    var node = graph.getNode(id);
                    return node != null ? node.name() : id;
                })
                .sorted()
                .toList();

        return new ImpactResult(nodeId, impacted, impacted.size(), names);
    }
}
