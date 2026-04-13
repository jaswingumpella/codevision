package com.codevision.codevisionbackend.usecase;

import com.codevision.codevisionbackend.graph.KgEdgeType;
import com.codevision.codevisionbackend.graph.KgNodeType;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Use case: "Find security risks: endpoints without auth, SQL injection vectors, etc."
 * Analyzes the graph for security-related patterns.
 */
@Service
public class SecurityScanService {

    private static final Set<String> SENSITIVE_PATTERNS = Set.of(
            "password", "secret", "token", "key", "credential", "auth"
    );

    public record SecurityReport(List<SecurityFinding> findings, int totalFindings) {}
    public record SecurityFinding(String nodeId, String nodeName, String category, String description) {}

    public SecurityReport scan(KnowledgeGraph graph) {
        var findings = new ArrayList<SecurityFinding>();

        // Check for exposed sensitive fields
        for (var fieldId : graph.nodesOfType(KgNodeType.FIELD)) {
            var node = graph.getNode(fieldId);
            if (node != null && node.name() != null) {
                var nameLower = node.name().toLowerCase();
                for (var pattern : SENSITIVE_PATTERNS) {
                    if (nameLower.contains(pattern)) {
                        findings.add(new SecurityFinding(fieldId, node.name(),
                                "SENSITIVE_DATA", "Field name suggests sensitive data: " + node.name()));
                        break;
                    }
                }
            }
        }

        // Check for endpoints with direct DB access (missing service layer)
        for (var epId : graph.nodesOfType(KgNodeType.ENDPOINT)) {
            for (var edge : graph.getNeighbors(epId)) {
                var target = graph.getNode(edge.targetNodeId());
                if (target != null && target.type() == KgNodeType.DATABASE_ENTITY) {
                    findings.add(new SecurityFinding(epId,
                            graph.getNode(epId) != null ? graph.getNode(epId).name() : epId,
                            "DIRECT_DB_ACCESS",
                            "Endpoint directly accesses database without service layer"));
                }
            }
        }

        return new SecurityReport(findings, findings.size());
    }
}
