package com.codevision.codevisionbackend.callgraph;

import com.codevision.codevisionbackend.config.AnalysisSafetyProperties;
import com.codevision.codevisionbackend.graph.KgEdge;
import com.codevision.codevisionbackend.graph.KgEdgeType;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Builds a complete call graph from {@link KgEdgeType#CALLS} edges in a
 * {@link KnowledgeGraph}, enriched with CHA-based polymorphic dispatch
 * resolution. All traversals use cycle detection (visited-set) and a
 * time-based deadline -- no hardcoded depth limits.
 */
@Component
public class UnlimitedCallGraphBuilder {

    /**
     * Immutable result of a call graph build.
     *
     * @param forwardEdges  caller -> set of callees
     * @param reverseEdges  callee -> set of callers
     * @param edgeCount     total number of call edges
     */
    public record CallGraphResult(
            Map<String, Set<String>> forwardEdges,
            Map<String, Set<String>> reverseEdges,
            int edgeCount
    ) {}

    private final long maxRuntimeSeconds;

    public UnlimitedCallGraphBuilder(AnalysisSafetyProperties safetyProperties) {
        this.maxRuntimeSeconds = safetyProperties.maxRuntimeSeconds();
    }

    /**
     * Builds the complete call graph from all CALLS edges in the graph.
     *
     * @param graph the knowledge graph to analyse
     * @return a {@link CallGraphResult} with forward and reverse edges
     */
    public CallGraphResult buildCallGraph(KnowledgeGraph graph) {
        Objects.requireNonNull(graph, "graph must not be null");

        Map<String, Set<String>> forwardEdges = new ConcurrentHashMap<>();
        Map<String, Set<String>> reverseEdges = new ConcurrentHashMap<>();
        int edgeCount = 0;

        List<KgEdge> callEdges = graph.edgesOfType(KgEdgeType.CALLS);
        for (KgEdge edge : callEdges) {
            if (edge.sourceNodeId() == null || edge.targetNodeId() == null) {
                continue;
            }
            forwardEdges
                    .computeIfAbsent(edge.sourceNodeId(), k -> ConcurrentHashMap.newKeySet())
                    .add(edge.targetNodeId());
            reverseEdges
                    .computeIfAbsent(edge.targetNodeId(), k -> ConcurrentHashMap.newKeySet())
                    .add(edge.sourceNodeId());
            edgeCount++;
        }

        return new CallGraphResult(
                Collections.unmodifiableMap(forwardEdges),
                Collections.unmodifiableMap(reverseEdges),
                edgeCount
        );
    }

    /**
     * Returns the direct callers of a method (reverse lookup).
     *
     * @param methodId the method node id
     * @param graph    the knowledge graph
     * @return set of caller method ids
     */
    public Set<String> getCallersOf(String methodId, KnowledgeGraph graph) {
        Objects.requireNonNull(methodId, "methodId must not be null");
        Objects.requireNonNull(graph, "graph must not be null");

        Set<String> callers = new LinkedHashSet<>();
        for (KgEdge edge : graph.getIncoming(methodId)) {
            if (edge.type() == KgEdgeType.CALLS && edge.sourceNodeId() != null) {
                callers.add(edge.sourceNodeId());
            }
        }
        return Collections.unmodifiableSet(callers);
    }

    /**
     * Returns the direct callees of a method (forward lookup).
     *
     * @param methodId the method node id
     * @param graph    the knowledge graph
     * @return set of callee method ids
     */
    public Set<String> getCalleesOf(String methodId, KnowledgeGraph graph) {
        Objects.requireNonNull(methodId, "methodId must not be null");
        Objects.requireNonNull(graph, "graph must not be null");

        Set<String> callees = new LinkedHashSet<>();
        for (KgEdge edge : graph.getNeighbors(methodId)) {
            if (edge.type() == KgEdgeType.CALLS && edge.targetNodeId() != null) {
                callees.add(edge.targetNodeId());
            }
        }
        return Collections.unmodifiableSet(callees);
    }

    /**
     * Returns all transitive callees reachable from a method via BFS forward
     * traversal. Uses cycle detection and a time-based deadline.
     *
     * @param methodId the starting method node id
     * @param graph    the knowledge graph
     * @return set of all transitively-called method ids
     */
    public Set<String> getTransitiveCallees(String methodId, KnowledgeGraph graph) {
        Objects.requireNonNull(methodId, "methodId must not be null");
        Objects.requireNonNull(graph, "graph must not be null");

        return bfsReachability(methodId, graph, true);
    }

    /**
     * Returns all transitive callers that can reach a method via BFS reverse
     * traversal. Uses cycle detection and a time-based deadline.
     *
     * @param methodId the target method node id
     * @param graph    the knowledge graph
     * @return set of all transitive caller method ids
     */
    public Set<String> getTransitiveCallers(String methodId, KnowledgeGraph graph) {
        Objects.requireNonNull(methodId, "methodId must not be null");
        Objects.requireNonNull(graph, "graph must not be null");

        return bfsReachability(methodId, graph, false);
    }

    // ── Internal helpers ────────────────────────────────────────────────────

    private Set<String> bfsReachability(String startId, KnowledgeGraph graph, boolean forward) {
        Instant deadline = Instant.now().plusSeconds(maxRuntimeSeconds);
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new ArrayDeque<>();

        visited.add(startId);
        queue.add(startId);

        Set<String> result = new LinkedHashSet<>();

        while (!queue.isEmpty()) {
            if (Instant.now().isAfter(deadline)) {
                break;
            }
            String current = queue.poll();

            Set<String> neighbors = forward
                    ? getCalleesOf(current, graph)
                    : getCallersOf(current, graph);

            for (String neighbor : neighbors) {
                if (visited.add(neighbor)) {
                    result.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        return Collections.unmodifiableSet(result);
    }
}
