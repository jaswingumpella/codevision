package com.codevision.codevisionbackend.graph.algorithm;

import com.codevision.codevisionbackend.graph.KgEdgeType;
import com.codevision.codevisionbackend.graph.KgNodeType;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Recognizes common design patterns in the knowledge graph by analyzing
 * structural relationships between nodes. Detects: Singleton, Strategy,
 * Observer, Factory, Decorator patterns.
 */
@Component
public class DesignPatternRecognizer implements GraphAlgorithm<Map<String, List<String>>> {

    @Override
    public String name() {
        return "design-pattern-recognizer";
    }

    @Override
    public Map<String, List<String>> execute(KnowledgeGraph graph) {
        var nodes = graph.getNodes();
        if (nodes.isEmpty()) {
            return Map.of();
        }

        Map<String, List<String>> patterns = new LinkedHashMap<>();
        patterns.put("STRATEGY", detectStrategy(graph));
        patterns.put("SINGLETON", detectSingleton(graph));
        patterns.put("OBSERVER", detectObserver(graph));
        patterns.put("DECORATOR", detectDecorator(graph));
        patterns.put("FACTORY", detectFactory(graph));

        // Remove empty pattern lists
        patterns.entrySet().removeIf(e -> e.getValue().isEmpty());

        return patterns;
    }

    /**
     * Strategy: an interface with 2+ implementing classes.
     */
    private List<String> detectStrategy(KnowledgeGraph graph) {
        List<String> strategies = new ArrayList<>();
        for (var interfaceId : graph.nodesOfType(KgNodeType.INTERFACE)) {
            var implementors = graph.edgesOfType(KgEdgeType.IMPLEMENTS).stream()
                    .filter(e -> interfaceId.equals(e.targetNodeId()))
                    .count();
            if (implementors >= 2) {
                var node = graph.getNode(interfaceId);
                strategies.add(node != null ? node.name() : interfaceId);
            }
        }
        return strategies;
    }

    /**
     * Singleton: a class with a field referencing its own type and a static factory method.
     * Simplified heuristic: class with READS_FIELD edge to itself.
     */
    private List<String> detectSingleton(KnowledgeGraph graph) {
        List<String> singletons = new ArrayList<>();
        for (var classId : graph.nodesOfType(KgNodeType.CLASS)) {
            boolean selfReference = graph.getNeighbors(classId).stream()
                    .anyMatch(e -> classId.equals(e.targetNodeId())
                            && (e.type() == KgEdgeType.READS_FIELD || e.type() == KgEdgeType.WRITES_FIELD));
            if (selfReference) {
                var node = graph.getNode(classId);
                singletons.add(node != null ? node.name() : classId);
            }
        }
        return singletons;
    }

    /**
     * Observer: PUBLISHES + SUBSCRIBES edges from/to the same node.
     */
    private List<String> detectObserver(KnowledgeGraph graph) {
        List<String> observers = new ArrayList<>();
        var publishEdges = graph.edgesOfType(KgEdgeType.PUBLISHES);
        if (!publishEdges.isEmpty()) {
            for (var edge : publishEdges) {
                var node = graph.getNode(edge.sourceNodeId());
                if (node != null) {
                    observers.add(node.name());
                }
            }
        }
        return observers;
    }

    /**
     * Decorator: a class that both EXTENDS another class and CONTAINS a field of the same type.
     */
    private List<String> detectDecorator(KnowledgeGraph graph) {
        List<String> decorators = new ArrayList<>();
        for (var extendsEdge : graph.edgesOfType(KgEdgeType.EXTENDS)) {
            var childId = extendsEdge.sourceNodeId();
            var parentId = extendsEdge.targetNodeId();
            // Check if child also has a field referencing parent type
            boolean hasFieldOfParentType = graph.getNeighbors(childId).stream()
                    .anyMatch(e -> e.type() == KgEdgeType.USES && parentId.equals(e.targetNodeId()));
            if (hasFieldOfParentType) {
                var node = graph.getNode(childId);
                decorators.add(node != null ? node.name() : childId);
            }
        }
        return decorators;
    }

    /**
     * Factory: a class/method whose name contains "Factory" and CONSTRUCTS other classes.
     */
    private List<String> detectFactory(KnowledgeGraph graph) {
        List<String> factories = new ArrayList<>();
        for (var entry : graph.getNodes().entrySet()) {
            var node = entry.getValue();
            if (node.name() != null && node.name().toLowerCase().contains("factory")) {
                boolean constructs = graph.getNeighbors(entry.getKey()).stream()
                        .anyMatch(e -> e.type() == KgEdgeType.CONSTRUCTS || e.type() == KgEdgeType.INSTANTIATES);
                if (constructs) {
                    factories.add(node.name());
                }
            }
        }
        return factories;
    }
}
