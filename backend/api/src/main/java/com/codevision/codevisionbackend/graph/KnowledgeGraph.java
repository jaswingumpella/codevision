package com.codevision.codevisionbackend.graph;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe, mutable container that accumulates {@link KgNode nodes} and
 * {@link KgEdge edges} during an analysis pass. Maintains several secondary
 * indices for fast lookup by id, type, and adjacency.
 *
 * <p>This is deliberately <em>not</em> a record because it holds mutable state
 * that is built up incrementally as scanners produce results.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KnowledgeGraph {

    /** Primary node store keyed by {@link KgNode#id()}. */
    private final Map<String, KgNode> nodes = new ConcurrentHashMap<>();

    /** Ordered list of all edges in insertion order. */
    private final List<KgEdge> edges = new CopyOnWriteArrayList<>();

    /** Forward adjacency index: source node id -> outgoing edges. */
    private final Map<String, List<KgEdge>> outEdges = new ConcurrentHashMap<>();

    /** Reverse adjacency index: target node id -> incoming edges. */
    private final Map<String, List<KgEdge>> inEdges = new ConcurrentHashMap<>();

    /** Type index for nodes: node type -> set of node ids. */
    private final Map<KgNodeType, Set<String>> nodesByType = new ConcurrentHashMap<>();

    /** Type index for edges: edge type -> edges of that type. */
    private final Map<KgEdgeType, List<KgEdge>> edgesByType = new ConcurrentHashMap<>();

    // ── Accessors (for Jackson serialization) ────────────────────────────

    /**
     * Returns an unmodifiable view of all nodes keyed by id.
     *
     * @return node map
     */
    public Map<String, KgNode> getNodes() {
        return Collections.unmodifiableMap(nodes);
    }

    /**
     * Returns an unmodifiable view of all edges.
     *
     * @return edge list
     */
    public List<KgEdge> getEdges() {
        return Collections.unmodifiableList(edges);
    }

    // ── Jackson deserialization setters ────────────────────────────────

    /**
     * Sets nodes from JSON deserialization, rebuilding all secondary indices.
     * Used by Jackson — prefer {@link #addNode} for programmatic use.
     */
    public void setNodes(Map<String, KgNode> incoming) {
        if (incoming == null) return;
        incoming.values().forEach(this::addNode);
    }

    /**
     * Sets edges from JSON deserialization, rebuilding all secondary indices.
     * Used by Jackson — prefer {@link #addEdge} for programmatic use.
     */
    public void setEdges(List<KgEdge> incoming) {
        if (incoming == null) return;
        incoming.forEach(this::addEdge);
    }

    // ── Mutation ─────────────────────────────────────────────────────────

    /**
     * Adds a node to the graph. If a node with the same id already exists it
     * is silently replaced.
     *
     * @param node the node to add; must not be {@code null}
     */
    public void addNode(KgNode node) {
        if (node == null || node.id() == null) {
            return;
        }
        nodes.put(node.id(), node);
        nodesByType
                .computeIfAbsent(node.type(), k -> ConcurrentHashMap.newKeySet())
                .add(node.id());
    }

    /**
     * Adds an edge to the graph and updates all secondary indices.
     *
     * @param edge the edge to add; must not be {@code null}
     */
    public void addEdge(KgEdge edge) {
        if (edge == null || edge.id() == null) {
            return;
        }
        edges.add(edge);

        if (edge.sourceNodeId() != null) {
            outEdges
                    .computeIfAbsent(edge.sourceNodeId(), k -> new CopyOnWriteArrayList<>())
                    .add(edge);
        }
        if (edge.targetNodeId() != null) {
            inEdges
                    .computeIfAbsent(edge.targetNodeId(), k -> new CopyOnWriteArrayList<>())
                    .add(edge);
        }
        edgesByType
                .computeIfAbsent(edge.type(), k -> new CopyOnWriteArrayList<>())
                .add(edge);
    }

    // ── Query ────────────────────────────────────────────────────────────

    /**
     * Retrieves a node by its unique identifier.
     *
     * @param id node id
     * @return the node, or {@code null} if not present
     */
    public KgNode getNode(String id) {
        return nodes.get(id);
    }

    /**
     * Returns all outgoing edges from the given node.
     *
     * @param id source node id
     * @return outgoing edges (never {@code null})
     */
    public List<KgEdge> getNeighbors(String id) {
        return outEdges.getOrDefault(id, List.of());
    }

    /**
     * Returns all incoming edges to the given node.
     *
     * @param id target node id
     * @return incoming edges (never {@code null})
     */
    public List<KgEdge> getIncoming(String id) {
        return inEdges.getOrDefault(id, List.of());
    }

    /**
     * Returns the total number of nodes in the graph.
     *
     * @return node count
     */
    public int nodeCount() {
        return nodes.size();
    }

    /**
     * Returns the total number of edges in the graph.
     *
     * @return edge count
     */
    public int edgeCount() {
        return edges.size();
    }

    /**
     * Returns the set of node ids that have the given type.
     *
     * @param type node type to query
     * @return set of matching node ids (never {@code null})
     */
    public Set<String> nodesOfType(KgNodeType type) {
        return nodesByType.getOrDefault(type, Set.of());
    }

    /**
     * Returns all edges of the given type.
     *
     * @param type edge type to query
     * @return matching edges (never {@code null})
     */
    public List<KgEdge> edgesOfType(KgEdgeType type) {
        return edgesByType.getOrDefault(type, List.of());
    }

    /**
     * Builds a simple forward adjacency map (source node id to set of target
     * node ids) from all edges currently in the graph.
     *
     * @return adjacency map
     */
    public Map<String, Set<String>> buildAdjacencyMap() {
        Map<String, Set<String>> adjacency = new ConcurrentHashMap<>();
        for (KgEdge edge : edges) {
            if (edge.sourceNodeId() == null || edge.targetNodeId() == null) {
                continue;
            }
            adjacency
                    .computeIfAbsent(edge.sourceNodeId(), k -> ConcurrentHashMap.newKeySet())
                    .add(edge.targetNodeId());
        }
        return adjacency;
    }
}
