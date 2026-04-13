package com.codevision.codevisionbackend.usecase;

import com.codevision.codevisionbackend.graph.KgNodeType;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Use case: "I'm new — give me a guided tour of this codebase."
 * Produces a dependency-ordered BFS tour starting from entry points.
 */
@Service
public class OnboardingGuideService {

    public record OnboardingGuide(List<TourStep> steps, int totalSteps) {}
    public record TourStep(int order, String nodeId, String name, String type, String description) {}

    public OnboardingGuide generate(KnowledgeGraph graph) {
        if (graph.nodeCount() == 0) {
            return new OnboardingGuide(List.of(), 0);
        }

        // Start from endpoints, then services, then data
        var steps = new ArrayList<TourStep>();
        Set<String> visited = new HashSet<>();
        int order = 1;

        // Priority order: ENDPOINT -> CLASS -> DATABASE_ENTITY
        for (var type : List.of(KgNodeType.ENDPOINT, KgNodeType.CLASS, KgNodeType.DATABASE_ENTITY)) {
            for (var nodeId : graph.nodesOfType(type)) {
                if (visited.add(nodeId)) {
                    var node = graph.getNode(nodeId);
                    steps.add(new TourStep(order++, nodeId,
                            node != null ? node.name() : nodeId,
                            type.name(),
                            describeNode(type)));
                }
            }
        }

        return new OnboardingGuide(steps, steps.size());
    }

    private String describeNode(KgNodeType type) {
        return switch (type) {
            case ENDPOINT -> "API entry point — start here to understand the system's external interface";
            case CLASS -> "Core business logic";
            case DATABASE_ENTITY -> "Data model — understand what data the system persists";
            default -> "Code element";
        };
    }
}
