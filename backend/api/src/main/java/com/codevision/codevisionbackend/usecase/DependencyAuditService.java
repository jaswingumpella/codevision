package com.codevision.codevisionbackend.usecase;

import com.codevision.codevisionbackend.graph.KgEdgeType;
import com.codevision.codevisionbackend.graph.KgNodeType;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Use case: "Audit my transitive dependencies for risk."
 * Identifies dependency artifacts in the graph and scores their risk.
 */
@Service
public class DependencyAuditService {

    public record AuditReport(List<DependencyRisk> risks, int totalDependencies) {}
    public record DependencyRisk(String artifactName, int dependentCount, String riskLevel) {}

    public AuditReport audit(KnowledgeGraph graph) {
        var depIds = graph.nodesOfType(KgNodeType.DEPENDENCY_ARTIFACT);
        if (depIds.isEmpty()) {
            return new AuditReport(List.of(), 0);
        }

        var risks = new ArrayList<DependencyRisk>();
        for (var depId : depIds) {
            var node = graph.getNode(depId);
            int dependentCount = graph.getIncoming(depId).size();
            String riskLevel = dependentCount > 10 ? "HIGH" : dependentCount > 3 ? "MEDIUM" : "LOW";
            risks.add(new DependencyRisk(
                    node != null ? node.name() : depId,
                    dependentCount, riskLevel));
        }

        risks.sort((a, b) -> Integer.compare(b.dependentCount(), a.dependentCount()));
        return new AuditReport(risks, depIds.size());
    }
}
