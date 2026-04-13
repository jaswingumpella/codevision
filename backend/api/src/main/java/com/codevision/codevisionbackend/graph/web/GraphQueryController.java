package com.codevision.codevisionbackend.graph.web;

import com.codevision.codevisionbackend.graph.KgEdge;
import com.codevision.codevisionbackend.graph.KgNode;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import com.codevision.codevisionbackend.graph.algorithm.GraphAlgorithmOrchestrator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

/**
 * REST controller exposing graph query endpoints for the interactive dashboard.
 * Returns data in Graphology-compatible JSON format for Sigma.js rendering.
 */
@RestController
@RequestMapping("/api/v1/graph")
public class GraphQueryController {

    private static final long DEFAULT_MAX_TRAVERSAL_SECONDS = 30;

    private final GraphAlgorithmOrchestrator orchestrator;

    public GraphQueryController(GraphAlgorithmOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    /**
     * Returns the full graph in Graphology JSON format.
     */
    @PostMapping("/full")
    public ResponseEntity<Map<String, Object>> getFullGraph(@RequestBody KnowledgeGraph graph) {
        return ResponseEntity.ok(toGraphologyFormat(graph, graph.getNodes().keySet()));
    }

    /**
     * Returns a subgraph centered on the given seed nodes, expanding via BFS
     * with cycle detection and time-based deadline (no depth limit).
     */
    @PostMapping("/subgraph")
    public ResponseEntity<Map<String, Object>> getSubgraph(@RequestBody SubgraphRequest request) {
        var graph = request.graph();
        var nodeIds = expandSubgraph(graph, request.seedNodes());
        return ResponseEntity.ok(toGraphologyFormat(graph, nodeIds));
    }

    /**
     * Returns community detection results.
     */
    @PostMapping("/communities")
    public ResponseEntity<Map<String, Object>> getCommunities(@RequestBody KnowledgeGraph graph) {
        var communityAlgo = orchestrator.findByName("community-detection");
        Map<String, Object> result = new LinkedHashMap<>();
        if (communityAlgo.isPresent()) {
            result.put("communities", communityAlgo.get().execute(graph));
        } else {
            result.put("communities", Map.of());
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Returns graph metrics including node/edge counts and algorithm results.
     */
    @PostMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics(@RequestBody KnowledgeGraph graph) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("nodeCount", graph.nodeCount());
        metrics.put("edgeCount", graph.edgeCount());
        metrics.put("algorithms", orchestrator.runAll(graph));
        return ResponseEntity.ok(metrics);
    }

    /**
     * Returns the shortest path between two nodes using BFS.
     */
    @PostMapping("/shortest-path")
    public ResponseEntity<Map<String, Object>> getShortestPath(
            @RequestBody KnowledgeGraph graph,
            @RequestParam String from,
            @RequestParam String to) {
        var path = bfsShortestPath(graph, from, to);
        return ResponseEntity.ok(Map.of("path", path, "length", path.size()));
    }

    /**
     * Returns all transitive dependencies (forward reachable nodes) from a given node.
     */
    @PostMapping("/transitive-deps")
    public ResponseEntity<Map<String, Object>> getTransitiveDeps(
            @RequestBody KnowledgeGraph graph,
            @RequestParam String nodeId) {
        var deps = bfsForwardReachable(graph, nodeId);
        return ResponseEntity.ok(Map.of("sourceNode", nodeId, "dependencies", new ArrayList<>(deps)));
    }

    // ── Graphology format conversion ────────────────────────────────────

    private Map<String, Object> toGraphologyFormat(KnowledgeGraph graph, Set<String> nodeIds) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        for (var nodeId : nodeIds) {
            var node = graph.getNode(nodeId);
            if (node == null) continue;
            nodes.add(nodeToGraphology(node));
        }

        for (var edge : graph.getEdges()) {
            if (nodeIds.contains(edge.sourceNodeId()) && nodeIds.contains(edge.targetNodeId())) {
                edges.add(edgeToGraphology(edge));
            }
        }

        return Map.of("nodes", nodes, "edges", edges);
    }

    private Map<String, Object> nodeToGraphology(KgNode node) {
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("key", node.id());
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("label", node.name());
        attributes.put("type", node.type() != null ? node.type().name() : "UNKNOWN");
        attributes.put("qualifiedName", node.qualifiedName());
        attributes.put("origin", node.origin());
        if (node.metadata() != null) {
            attributes.put("modifiers", node.metadata().modifiers());
            attributes.put("returnType", node.metadata().returnType());
        }
        attrs.put("attributes", attributes);
        return attrs;
    }

    private Map<String, Object> edgeToGraphology(KgEdge edge) {
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("key", edge.id());
        attrs.put("source", edge.sourceNodeId());
        attrs.put("target", edge.targetNodeId());
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("type", edge.type() != null ? edge.type().name() : "UNKNOWN");
        attributes.put("label", edge.label());
        if (edge.confidence() != null) {
            attributes.put("confidence", edge.confidence().name());
        }
        attrs.put("attributes", attributes);
        return attrs;
    }

    // ── Graph traversal utilities ───────────────────────────────────────

    /**
     * Expands from seed nodes via BFS with cycle detection (visited-set)
     * and time-based deadline. No depth limit.
     */
    private Set<String> expandSubgraph(KnowledgeGraph graph, Set<String> seeds) {
        Set<String> result = new HashSet<>(seeds);
        Queue<String> frontier = new ArrayDeque<>(seeds);
        var deadline = Instant.now().plusSeconds(DEFAULT_MAX_TRAVERSAL_SECONDS);

        while (!frontier.isEmpty() && Instant.now().isBefore(deadline)) {
            var nodeId = frontier.poll();
            for (var edge : graph.getNeighbors(nodeId)) {
                if (result.add(edge.targetNodeId())) {
                    frontier.add(edge.targetNodeId());
                }
            }
            for (var edge : graph.getIncoming(nodeId)) {
                if (result.add(edge.sourceNodeId())) {
                    frontier.add(edge.sourceNodeId());
                }
            }
        }
        return result;
    }

    private List<String> bfsShortestPath(KnowledgeGraph graph, String from, String to) {
        if (from.equals(to)) return List.of(from);

        Map<String, String> parent = new HashMap<>();
        Queue<String> queue = new ArrayDeque<>();
        queue.add(from);
        parent.put(from, null);
        var deadline = Instant.now().plusSeconds(DEFAULT_MAX_TRAVERSAL_SECONDS);

        while (!queue.isEmpty() && Instant.now().isBefore(deadline)) {
            var current = queue.poll();
            for (var edge : graph.getNeighbors(current)) {
                var next = edge.targetNodeId();
                if (!parent.containsKey(next)) {
                    parent.put(next, current);
                    if (next.equals(to)) {
                        return reconstructPath(parent, to);
                    }
                    queue.add(next);
                }
            }
        }
        return List.of();
    }

    private List<String> reconstructPath(Map<String, String> parent, String to) {
        var path = new ArrayList<String>();
        var current = to;
        while (current != null) {
            path.add(current);
            current = parent.get(current);
        }
        Collections.reverse(path);
        return path;
    }

    private Set<String> bfsForwardReachable(KnowledgeGraph graph, String startNode) {
        Set<String> visited = new LinkedHashSet<>();
        Queue<String> queue = new ArrayDeque<>();
        queue.add(startNode);
        Set<String> seen = new HashSet<>();
        seen.add(startNode);
        var deadline = Instant.now().plusSeconds(DEFAULT_MAX_TRAVERSAL_SECONDS);

        while (!queue.isEmpty() && Instant.now().isBefore(deadline)) {
            var current = queue.poll();
            for (var edge : graph.getNeighbors(current)) {
                var next = edge.targetNodeId();
                if (seen.add(next)) {
                    visited.add(next);
                    queue.add(next);
                }
            }
        }
        return visited;
    }

    /**
     * Combined request body for subgraph queries.
     */
    public record SubgraphRequest(KnowledgeGraph graph, Set<String> seedNodes) {}
}
