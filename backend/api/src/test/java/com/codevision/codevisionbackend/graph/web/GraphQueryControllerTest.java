package com.codevision.codevisionbackend.graph.web;

import com.codevision.codevisionbackend.graph.*;
import com.codevision.codevisionbackend.graph.algorithm.CommunityDetectionAlgorithm;
import com.codevision.codevisionbackend.graph.algorithm.GraphAlgorithmOrchestrator;
import com.codevision.codevisionbackend.graph.algorithm.ImpactAnalyzer;
import com.codevision.codevisionbackend.graph.algorithm.PageRankAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;

class GraphQueryControllerTest {

    private GraphQueryController controller;
    private GraphAlgorithmOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new GraphAlgorithmOrchestrator(List.of(
                new PageRankAlgorithm(),
                new CommunityDetectionAlgorithm(),
                new ImpactAnalyzer()
        ));
        controller = new GraphQueryController(orchestrator);
    }

    private KnowledgeGraph buildSampleGraph() {
        var graph = new KnowledgeGraph();
        var metadata = new NodeMetadata(
                "public", Set.of(), emptyList(), emptyList(),
                "void", emptyList(), emptyList(), null,
                0, 0, 0, null, null, 0, 0, null
        );
        var provenance = new Provenance("test", "Test.java", 1, ConfidenceLevel.EXTRACTED);

        graph.addNode(new KgNode("ep1", KgNodeType.ENDPOINT, "getUsers", "com.app.UserController.getUsers",
                metadata, null, "SOURCE", provenance));
        graph.addNode(new KgNode("cls1", KgNodeType.CLASS, "UserService", "com.app.UserService",
                metadata, null, "SOURCE", provenance));
        graph.addNode(new KgNode("cls2", KgNodeType.CLASS, "UserRepo", "com.app.UserRepo",
                metadata, null, "SOURCE", provenance));
        graph.addNode(new KgNode("db1", KgNodeType.DATABASE_ENTITY, "users", "users",
                metadata, null, "SOURCE", provenance));

        graph.addEdge(new KgEdge("e1", KgEdgeType.CALLS, "ep1", "cls1", "calls", ConfidenceLevel.EXTRACTED, provenance, Map.of()));
        graph.addEdge(new KgEdge("e2", KgEdgeType.CALLS, "cls1", "cls2", "calls", ConfidenceLevel.EXTRACTED, provenance, Map.of()));
        graph.addEdge(new KgEdge("e3", KgEdgeType.MAPS_TO_TABLE, "cls2", "db1", "accesses", ConfidenceLevel.EXTRACTED, provenance, Map.of()));

        return graph;
    }

    private KnowledgeGraph buildCyclicGraph() {
        var graph = new KnowledgeGraph();
        var metadata = new NodeMetadata(
                "public", Set.of(), emptyList(), emptyList(),
                "void", emptyList(), emptyList(), null,
                0, 0, 0, null, null, 0, 0, null
        );
        var provenance = new Provenance("test", "Test.java", 1, ConfidenceLevel.EXTRACTED);

        graph.addNode(new KgNode("a", KgNodeType.CLASS, "A", "A", metadata, null, "SOURCE", provenance));
        graph.addNode(new KgNode("b", KgNodeType.CLASS, "B", "B", metadata, null, "SOURCE", provenance));
        graph.addNode(new KgNode("c", KgNodeType.CLASS, "C", "C", metadata, null, "SOURCE", provenance));

        graph.addEdge(new KgEdge("e1", KgEdgeType.CALLS, "a", "b", "calls", ConfidenceLevel.EXTRACTED, provenance, Map.of()));
        graph.addEdge(new KgEdge("e2", KgEdgeType.CALLS, "b", "c", "calls", ConfidenceLevel.EXTRACTED, provenance, Map.of()));
        graph.addEdge(new KgEdge("e3", KgEdgeType.CALLS, "c", "a", "calls", ConfidenceLevel.EXTRACTED, provenance, Map.of()));

        return graph;
    }

    @Nested
    class Given_ValidGraph {

        @Nested
        class When_GetFullGraph {

            @Test
            void Then_ReturnsGraphologyFormat() {
                var graph = buildSampleGraph();
                var response = controller.getFullGraph(graph);

                assertEquals(HttpStatus.OK, response.getStatusCode());
                var body = response.getBody();
                assertNotNull(body);

                @SuppressWarnings("unchecked")
                var nodes = (List<Map<String, Object>>) body.get("nodes");
                @SuppressWarnings("unchecked")
                var edges = (List<Map<String, Object>>) body.get("edges");

                assertEquals(4, nodes.size());
                assertEquals(3, edges.size());

                // Verify Graphology node shape
                var firstNode = nodes.stream()
                        .filter(n -> "ep1".equals(n.get("key")))
                        .findFirst().orElseThrow();
                @SuppressWarnings("unchecked")
                var attrs = (Map<String, Object>) firstNode.get("attributes");
                assertEquals("getUsers", attrs.get("label"));
                assertEquals("ENDPOINT", attrs.get("type"));
            }
        }

        @Nested
        class When_GetSubgraph {

            @Test
            void Then_ReturnsExpandedNodes() {
                var graph = buildSampleGraph();
                var request = new GraphQueryController.SubgraphRequest(graph, Set.of("cls1"));
                var response = controller.getSubgraph(request);

                assertEquals(HttpStatus.OK, response.getStatusCode());
                var body = response.getBody();
                assertNotNull(body);

                @SuppressWarnings("unchecked")
                var nodes = (List<Map<String, Object>>) body.get("nodes");
                // BFS from cls1 expands to ep1 (incoming), cls2 (outgoing), then db1
                assertEquals(4, nodes.size());
            }
        }

        @Nested
        class When_GetCommunities {

            @Test
            void Then_ReturnsCommunityAssignments() {
                var graph = buildSampleGraph();
                var response = controller.getCommunities(graph);

                assertEquals(HttpStatus.OK, response.getStatusCode());
                var body = response.getBody();
                assertNotNull(body);
                assertTrue(body.containsKey("communities"));
            }
        }

        @Nested
        class When_GetMetrics {

            @Test
            void Then_ReturnsAllAlgorithmResults() {
                var graph = buildSampleGraph();
                var response = controller.getMetrics(graph);

                assertEquals(HttpStatus.OK, response.getStatusCode());
                var body = response.getBody();
                assertNotNull(body);
                assertTrue(body.containsKey("nodeCount"));
                assertTrue(body.containsKey("edgeCount"));
                assertTrue(body.containsKey("algorithms"));
            }
        }

        @Nested
        class When_GetShortestPath {

            @Test
            void Then_ReturnsPathBetweenNodes() {
                var graph = buildSampleGraph();
                var response = controller.getShortestPath(graph, "ep1", "db1");

                assertEquals(HttpStatus.OK, response.getStatusCode());
                var body = response.getBody();
                assertNotNull(body);

                @SuppressWarnings("unchecked")
                var path = (List<String>) body.get("path");
                assertNotNull(path);
                assertEquals(4, path.size());
                assertEquals("ep1", path.get(0));
                assertEquals("db1", path.get(path.size() - 1));
            }

            @Test
            void Then_ReturnsEmptyPathForUnreachableNodes() {
                var graph = buildSampleGraph();
                var response = controller.getShortestPath(graph, "db1", "ep1");

                assertEquals(HttpStatus.OK, response.getStatusCode());
                var body = response.getBody();
                assertNotNull(body);

                @SuppressWarnings("unchecked")
                var path = (List<String>) body.get("path");
                assertTrue(path.isEmpty());
            }

            @Test
            void Then_ReturnsSingleNodePathWhenFromEqualsTo() {
                var graph = buildSampleGraph();
                var response = controller.getShortestPath(graph, "ep1", "ep1");

                @SuppressWarnings("unchecked")
                var path = (List<String>) response.getBody().get("path");
                assertEquals(1, path.size());
                assertEquals("ep1", path.get(0));
            }
        }

        @Nested
        class When_GetTransitiveDeps {

            @Test
            void Then_ReturnsAllReachableNodes() {
                var graph = buildSampleGraph();
                var response = controller.getTransitiveDeps(graph, "ep1");

                assertEquals(HttpStatus.OK, response.getStatusCode());
                var body = response.getBody();
                assertNotNull(body);

                @SuppressWarnings("unchecked")
                var deps = (List<String>) body.get("dependencies");
                assertEquals(3, deps.size());
            }

            @Test
            void Then_ReturnsEmptyForIsolatedNode() {
                var graph = buildSampleGraph();
                var response = controller.getTransitiveDeps(graph, "db1");

                @SuppressWarnings("unchecked")
                var deps = (List<String>) response.getBody().get("dependencies");
                assertTrue(deps.isEmpty());
            }

            @Test
            void Then_ReturnsEmptyForMissingNode() {
                var graph = buildSampleGraph();
                var response = controller.getTransitiveDeps(graph, "nonexistent");

                @SuppressWarnings("unchecked")
                var deps = (List<String>) response.getBody().get("dependencies");
                assertTrue(deps.isEmpty());
            }
        }
    }

    @Nested
    class Given_CyclicGraph {

        @Nested
        class When_GetTransitiveDeps {

            @Test
            void Then_TerminatesAndReturnsAllNodes() {
                var graph = buildCyclicGraph();
                var response = controller.getTransitiveDeps(graph, "a");

                @SuppressWarnings("unchecked")
                var deps = (List<String>) response.getBody().get("dependencies");
                assertEquals(2, deps.size()); // b and c, not a itself
            }
        }

        @Nested
        class When_GetShortestPath {

            @Test
            void Then_TerminatesAndFindsPath() {
                var graph = buildCyclicGraph();
                var response = controller.getShortestPath(graph, "a", "c");

                @SuppressWarnings("unchecked")
                var path = (List<String>) response.getBody().get("path");
                assertEquals(3, path.size());
                assertEquals("a", path.get(0));
                assertEquals("c", path.get(2));
            }
        }

        @Nested
        class When_GetSubgraph {

            @Test
            void Then_TerminatesAndReturnsAllReachableNodes() {
                var graph = buildCyclicGraph();
                var request = new GraphQueryController.SubgraphRequest(graph, Set.of("a"));
                var response = controller.getSubgraph(request);

                @SuppressWarnings("unchecked")
                var nodes = (List<Map<String, Object>>) response.getBody().get("nodes");
                assertEquals(3, nodes.size());
            }
        }
    }

    @Nested
    class Given_EmptyGraph {

        @Nested
        class When_GetFullGraph {

            @Test
            void Then_ReturnsEmptyGraphology() {
                var graph = new KnowledgeGraph();
                var response = controller.getFullGraph(graph);

                assertEquals(HttpStatus.OK, response.getStatusCode());
                var body = response.getBody();
                assertNotNull(body);

                @SuppressWarnings("unchecked")
                var nodes = (List<Map<String, Object>>) body.get("nodes");
                assertTrue(nodes.isEmpty());
            }
        }

        @Nested
        class When_GetMetrics {

            @Test
            void Then_ReturnsZeroCounts() {
                var graph = new KnowledgeGraph();
                var response = controller.getMetrics(graph);

                assertEquals(HttpStatus.OK, response.getStatusCode());
                var body = response.getBody();
                assertNotNull(body);
                assertEquals(0, body.get("nodeCount"));
                assertEquals(0, body.get("edgeCount"));
            }
        }
    }

    @Nested
    class Given_OrchestratorWithoutCommunityAlgorithm {

        @Nested
        class When_GetCommunities {

            @Test
            void Then_ReturnsEmptyCommunities() {
                var emptyOrchestrator = new GraphAlgorithmOrchestrator(List.of());
                var ctrl = new GraphQueryController(emptyOrchestrator);
                var graph = buildSampleGraph();

                var response = ctrl.getCommunities(graph);
                @SuppressWarnings("unchecked")
                var communities = response.getBody().get("communities");
                assertNotNull(communities);
            }
        }
    }
}
