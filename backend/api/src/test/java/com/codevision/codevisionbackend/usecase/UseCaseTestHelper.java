package com.codevision.codevisionbackend.usecase;

import com.codevision.codevisionbackend.graph.*;

import java.util.Map;
import java.util.UUID;

/**
 * Utility for building test KnowledgeGraph instances for use case tests.
 */
final class UseCaseTestHelper {

    private UseCaseTestHelper() {}

    static KgNode node(String id, String name, KgNodeType type) {
        return new KgNode(id, type, name, "com.example." + name,
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

    static KgNode dbEntityNode(String id, String name) {
        return node(id, name, KgNodeType.DATABASE_ENTITY);
    }

    static KgNode testNode(String id, String name) {
        return node(id, name, KgNodeType.TEST_CASE);
    }

    static KgEdge edge(String sourceId, String targetId, KgEdgeType type) {
        return new KgEdge(UUID.randomUUID().toString(), type, sourceId, targetId,
                null, ConfidenceLevel.EXTRACTED, new Provenance("test", "test.java", 0, ConfidenceLevel.EXTRACTED),
                Map.of());
    }

    static KgEdge callsEdge(String src, String tgt) { return edge(src, tgt, KgEdgeType.CALLS); }
    static KgEdge containsEdge(String src, String tgt) { return edge(src, tgt, KgEdgeType.CONTAINS); }
    static KgEdge testsEdge(String src, String tgt) { return edge(src, tgt, KgEdgeType.TESTS); }
    static KgEdge mapsToTableEdge(String src, String tgt) { return edge(src, tgt, KgEdgeType.MAPS_TO_TABLE); }
    static KgEdge dependsOnEdge(String src, String tgt) { return edge(src, tgt, KgEdgeType.DEPENDS_ON); }

    /**
     * Creates a typical layered app: endpoint -> service -> repository -> entity.
     */
    static KnowledgeGraph layeredApp() {
        var graph = new KnowledgeGraph();
        graph.addNode(endpointNode("ep1", "GET /api/users"));
        graph.addNode(classNode("svc", "UserService"));
        graph.addNode(classNode("repo", "UserRepository"));
        graph.addNode(dbEntityNode("entity", "users"));
        graph.addNode(methodNode("m1", "getUsers"));
        graph.addNode(methodNode("m2", "findAll"));
        graph.addEdge(callsEdge("ep1", "svc"));
        graph.addEdge(callsEdge("svc", "repo"));
        graph.addEdge(edge("repo", "entity", KgEdgeType.QUERIES));
        graph.addEdge(containsEdge("svc", "m1"));
        graph.addEdge(containsEdge("repo", "m2"));
        return graph;
    }
}
