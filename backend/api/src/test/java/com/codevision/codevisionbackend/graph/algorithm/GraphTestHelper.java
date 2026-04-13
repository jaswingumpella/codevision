package com.codevision.codevisionbackend.graph.algorithm;

import com.codevision.codevisionbackend.graph.*;

import java.util.Map;
import java.util.UUID;

/**
 * Utility for building test KnowledgeGraph instances.
 */
final class GraphTestHelper {

    private GraphTestHelper() {}

    static KgNode node(String id, String name, KgNodeType type) {
        return new KgNode(id, type, name, "test." + name,
                new NodeMetadata(null, null, null, null, null, null, null, null, 0, 0, 0, null, null, 0, 0, null),
                null, "SOURCE", new Provenance("test", "test.java", 0, ConfidenceLevel.EXTRACTED));
    }

    static KgNode classNode(String id, String name) {
        return node(id, name, KgNodeType.CLASS);
    }

    static KgNode methodNode(String id, String name) {
        return node(id, name, KgNodeType.METHOD);
    }

    static KgNode endpointNode(String id, String name) {
        return node(id, name, KgNodeType.ENDPOINT);
    }

    static KgNode interfaceNode(String id, String name) {
        return node(id, name, KgNodeType.INTERFACE);
    }

    static KgNode fieldNode(String id, String name) {
        return node(id, name, KgNodeType.FIELD);
    }

    static KgEdge edge(String sourceId, String targetId, KgEdgeType type) {
        return new KgEdge(UUID.randomUUID().toString(), type, sourceId, targetId,
                null, ConfidenceLevel.EXTRACTED, new Provenance("test", "test.java", 0, ConfidenceLevel.EXTRACTED),
                Map.of());
    }

    static KgEdge callsEdge(String sourceId, String targetId) {
        return edge(sourceId, targetId, KgEdgeType.CALLS);
    }

    static KgEdge extendsEdge(String sourceId, String targetId) {
        return edge(sourceId, targetId, KgEdgeType.EXTENDS);
    }

    static KgEdge implementsEdge(String sourceId, String targetId) {
        return edge(sourceId, targetId, KgEdgeType.IMPLEMENTS);
    }

    static KgEdge containsEdge(String sourceId, String targetId) {
        return edge(sourceId, targetId, KgEdgeType.CONTAINS);
    }

    static KgEdge dependsOnEdge(String sourceId, String targetId) {
        return edge(sourceId, targetId, KgEdgeType.DEPENDS_ON);
    }

    static KgEdge injectsEdge(String sourceId, String targetId) {
        return edge(sourceId, targetId, KgEdgeType.INJECTS);
    }

    /**
     * Creates a linear chain: A -> B -> C -> ... with CALLS edges.
     */
    static KnowledgeGraph linearChain(int size) {
        var graph = new KnowledgeGraph();
        for (int i = 0; i < size; i++) {
            graph.addNode(classNode("n" + i, "Node" + i));
        }
        for (int i = 0; i < size - 1; i++) {
            graph.addEdge(callsEdge("n" + i, "n" + (i + 1)));
        }
        return graph;
    }

    /**
     * Creates a star graph: center -> spoke1, center -> spoke2, ...
     */
    static KnowledgeGraph starGraph(int spokeCount) {
        var graph = new KnowledgeGraph();
        graph.addNode(classNode("center", "Center"));
        for (int i = 0; i < spokeCount; i++) {
            graph.addNode(classNode("spoke" + i, "Spoke" + i));
            graph.addEdge(callsEdge("center", "spoke" + i));
        }
        return graph;
    }

    /**
     * Creates two disconnected cliques of the given size, connected internally with CALLS edges.
     */
    static KnowledgeGraph twoCliques(int cliqueSize) {
        var graph = new KnowledgeGraph();
        for (int c = 0; c < 2; c++) {
            for (int i = 0; i < cliqueSize; i++) {
                graph.addNode(classNode("c" + c + "n" + i, "Clique" + c + "Node" + i));
            }
            for (int i = 0; i < cliqueSize; i++) {
                for (int j = 0; j < cliqueSize; j++) {
                    if (i != j) {
                        graph.addEdge(callsEdge("c" + c + "n" + i, "c" + c + "n" + j));
                    }
                }
            }
        }
        return graph;
    }
}
