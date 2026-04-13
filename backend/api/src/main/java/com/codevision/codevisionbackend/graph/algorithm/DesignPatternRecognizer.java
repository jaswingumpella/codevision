package com.codevision.codevisionbackend.graph.algorithm;

import com.codevision.codevisionbackend.graph.KgEdgeType;
import com.codevision.codevisionbackend.graph.KgNodeType;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
     * Singleton: a class that has a self-referencing field access (READS_FIELD/WRITES_FIELD)
     * OR constructs itself (CONSTRUCTS self-edge), indicating lazy initialization.
     */
    private List<String> detectSingleton(KnowledgeGraph graph) {
        List<String> singletons = new ArrayList<>();
        for (var classId : graph.nodesOfType(KgNodeType.CLASS)) {
            var neighbors = graph.getNeighbors(classId);
            boolean selfFieldRef = neighbors.stream()
                    .anyMatch(e -> classId.equals(e.targetNodeId())
                            && (e.type() == KgEdgeType.READS_FIELD || e.type() == KgEdgeType.WRITES_FIELD));
            boolean selfConstruct = neighbors.stream()
                    .anyMatch(e -> classId.equals(e.targetNodeId())
                            && e.type() == KgEdgeType.CONSTRUCTS);
            if (selfFieldRef || selfConstruct) {
                var node = graph.getNode(classId);
                singletons.add(node != null ? node.name() : classId);
            }
        }
        return singletons;
    }

    private static final Set<String> OBSERVER_METHOD_PREFIXES = Set.of(
            "add", "remove", "notify", "fire");

    /**
     * Observer: a class that PUBLISHES events, OR a class that contains 3+ methods
     * whose names suggest listener management (add*, remove*, notify*, on*, fire*).
     */
    private List<String> detectObserver(KnowledgeGraph graph) {
        Set<String> observerNames = new LinkedHashSet<>();

        // Detect via PUBLISHES edges
        for (var edge : graph.edgesOfType(KgEdgeType.PUBLISHES)) {
            var node = graph.getNode(edge.sourceNodeId());
            if (node != null) {
                observerNames.add(node.name());
            }
        }

        // Detect via structural method analysis: class contains 3+ listener-management methods
        for (var classId : graph.nodesOfType(KgNodeType.CLASS)) {
            var containedMethods = graph.getNeighbors(classId).stream()
                    .filter(e -> e.type() == KgEdgeType.CONTAINS)
                    .map(e -> graph.getNode(e.targetNodeId()))
                    .filter(n -> n != null && n.type() == KgNodeType.METHOD)
                    .toList();

            long listenerMethodCount = containedMethods.stream()
                    .filter(m -> {
                        var methodName = m.name() != null ? m.name().toLowerCase() : "";
                        return OBSERVER_METHOD_PREFIXES.stream().anyMatch(methodName::startsWith);
                    })
                    .count();

            if (listenerMethodCount >= 3) {
                var node = graph.getNode(classId);
                if (node != null) {
                    observerNames.add(node.name());
                }
            }
        }

        return new ArrayList<>(observerNames);
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
     * Factory: a class that has CONSTRUCTS or INSTANTIATES edges to 2+ different target classes.
     * Purely structural - no name-based heuristics.
     */
    private List<String> detectFactory(KnowledgeGraph graph) {
        List<String> factories = new ArrayList<>();
        for (var entry : graph.getNodes().entrySet()) {
            var nodeId = entry.getKey();
            var node = entry.getValue();
            if (node.type() != KgNodeType.CLASS && node.type() != KgNodeType.METHOD) {
                continue;
            }
            long distinctTargets = graph.getNeighbors(nodeId).stream()
                    .filter(e -> e.type() == KgEdgeType.CONSTRUCTS || e.type() == KgEdgeType.INSTANTIATES)
                    .filter(e -> !nodeId.equals(e.targetNodeId())) // exclude self-construction (singleton)
                    .map(e -> e.targetNodeId())
                    .distinct()
                    .count();
            if (distinctTargets >= 2) {
                factories.add(node.name());
            }
        }
        return factories;
    }
}
