package com.codevision.codevisionbackend.graph.algorithm;

import com.codevision.codevisionbackend.graph.KgNodeType;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Detects dead code by identifying nodes that are not reachable from any
 * entry point (endpoints, main methods, test suites). Uses BFS from all
 * entry points; any node not visited is considered dead.
 */
@Component
public class DeadCodeDetector implements GraphAlgorithm<Set<String>> {

    private static final Set<KgNodeType> ENTRY_POINT_TYPES = Set.of(
            KgNodeType.ENDPOINT,
            KgNodeType.TEST_CASE,
            KgNodeType.TEST_SUITE
    );

    @Override
    public String name() {
        return "dead-code-detector";
    }

    @Override
    public Set<String> execute(KnowledgeGraph graph) {
        var allNodeIds = graph.getNodes().keySet();
        if (allNodeIds.isEmpty()) {
            return Set.of();
        }

        // Find entry points
        Set<String> entryPoints = new HashSet<>();
        for (var type : ENTRY_POINT_TYPES) {
            entryPoints.addAll(graph.nodesOfType(type));
        }

        // Also consider nodes with no incoming edges but with outgoing edges as roots
        Set<String> nodesWithIncoming = new HashSet<>();
        Set<String> nodesWithOutgoing = new HashSet<>();
        for (var edge : graph.getEdges()) {
            if (edge.targetNodeId() != null) {
                nodesWithIncoming.add(edge.targetNodeId());
            }
            if (edge.sourceNodeId() != null) {
                nodesWithOutgoing.add(edge.sourceNodeId());
            }
        }
        for (var id : allNodeIds) {
            if (!nodesWithIncoming.contains(id) && nodesWithOutgoing.contains(id)) {
                entryPoints.add(id);
            }
        }

        // BFS from all entry points
        Set<String> reachable = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>(entryPoints);
        reachable.addAll(entryPoints);

        while (!queue.isEmpty()) {
            var current = queue.poll();
            for (var edge : graph.getNeighbors(current)) {
                var target = edge.targetNodeId();
                if (target != null && reachable.add(target)) {
                    queue.add(target);
                }
            }
        }

        // Dead code = all nodes not reachable
        return allNodeIds.stream()
                .filter(id -> !reachable.contains(id))
                .collect(Collectors.toSet());
    }
}
