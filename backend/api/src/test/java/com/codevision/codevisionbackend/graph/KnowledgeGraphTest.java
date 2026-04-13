package com.codevision.codevisionbackend.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("KnowledgeGraph")
class KnowledgeGraphTest {

    // ── Fixture helpers ─────────────────────────────────────────────────

    private static KgNode node(String id, KgNodeType type) {
        return new KgNode(id, type, id, "qual." + id, null, null, "SOURCE", null);
    }

    private static KgEdge edge(String id, KgEdgeType type, String source, String target) {
        return new KgEdge(id, type, source, target, null, ConfidenceLevel.EXTRACTED, null, Map.of());
    }

    // ── Tests ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Given an empty graph")
    class Given_EmptyGraph {

        private final KnowledgeGraph graph = new KnowledgeGraph();

        @Nested
        @DisplayName("When querying node count")
        class When_QueryingNodeCount {

            @Test
            @DisplayName("Then returns zero")
            void Then_ReturnsZero() {
                assertEquals(0, graph.nodeCount());
            }
        }

        @Nested
        @DisplayName("When querying edge count")
        class When_QueryingEdgeCount {

            @Test
            @DisplayName("Then returns zero")
            void Then_ReturnsZero() {
                assertEquals(0, graph.edgeCount());
            }
        }

        @Nested
        @DisplayName("When adding a node")
        class When_AddingNode {

            private final KgNode classNode = node("com.example.Foo", KgNodeType.CLASS);

            @Test
            @DisplayName("Then node is retrievable by id")
            void Then_NodeIsRetrievableById() {
                graph.addNode(classNode);
                assertNotNull(graph.getNode("com.example.Foo"));
            }

            @Test
            @DisplayName("Then node appears in the type index")
            void Then_NodeAppearsInTypeIndex() {
                graph.addNode(classNode);
                assertTrue(graph.nodesOfType(KgNodeType.CLASS).contains("com.example.Foo"));
            }

            @Test
            @DisplayName("Then node count becomes one")
            void Then_NodeCountBecomesOne() {
                graph.addNode(classNode);
                assertEquals(1, graph.nodeCount());
            }
        }

        @Nested
        @DisplayName("When adding a null node")
        class When_AddingNullNode {

            @Test
            @DisplayName("Then graph remains empty")
            void Then_GraphRemainsEmpty() {
                graph.addNode(null);
                assertEquals(0, graph.nodeCount());
            }
        }

        @Nested
        @DisplayName("When adding a node with null id")
        class When_AddingNodeWithNullId {

            @Test
            @DisplayName("Then graph remains empty")
            void Then_GraphRemainsEmpty() {
                graph.addNode(new KgNode(null, KgNodeType.CLASS, "X", "X", null, null, null, null));
                assertEquals(0, graph.nodeCount());
            }
        }

        @Nested
        @DisplayName("When querying a non-existent node")
        class When_QueryingNonExistentNode {

            @Test
            @DisplayName("Then returns null")
            void Then_ReturnsNull() {
                assertNull(graph.getNode("does.not.exist"));
            }
        }

        @Nested
        @DisplayName("When querying nodes of an unused type")
        class When_QueryingNodesOfUnusedType {

            @Test
            @DisplayName("Then returns an empty set")
            void Then_ReturnsEmptySet() {
                assertTrue(graph.nodesOfType(KgNodeType.INTERFACE).isEmpty());
            }
        }

        @Nested
        @DisplayName("When querying neighbors of a missing node")
        class When_QueryingNeighborsOfMissingNode {

            @Test
            @DisplayName("Then returns empty list")
            void Then_ReturnsEmptyList() {
                assertTrue(graph.getNeighbors("missing").isEmpty());
            }
        }

        @Nested
        @DisplayName("When querying incoming edges of a missing node")
        class When_QueryingIncomingOfMissingNode {

            @Test
            @DisplayName("Then returns empty list")
            void Then_ReturnsEmptyList() {
                assertTrue(graph.getIncoming("missing").isEmpty());
            }
        }

        @Nested
        @DisplayName("When querying edges of an unused type")
        class When_QueryingEdgesOfUnusedType {

            @Test
            @DisplayName("Then returns empty list")
            void Then_ReturnsEmptyList() {
                assertTrue(graph.edgesOfType(KgEdgeType.CALLS).isEmpty());
            }
        }

        @Nested
        @DisplayName("When building adjacency map")
        class When_BuildingAdjacencyMap {

            @Test
            @DisplayName("Then returns empty map")
            void Then_ReturnsEmptyMap() {
                assertTrue(graph.buildAdjacencyMap().isEmpty());
            }
        }
    }

    @Nested
    @DisplayName("Given a graph with nodes")
    class Given_GraphWithNodes {

        private KnowledgeGraph graphWithTwoNodes() {
            KnowledgeGraph g = new KnowledgeGraph();
            g.addNode(node("A", KgNodeType.CLASS));
            g.addNode(node("B", KgNodeType.INTERFACE));
            return g;
        }

        @Nested
        @DisplayName("When adding an edge")
        class When_AddingEdge {

            @Test
            @DisplayName("Then edge appears in out-adjacency")
            void Then_EdgeAppearsInOutAdjacency() {
                KnowledgeGraph g = graphWithTwoNodes();
                g.addEdge(edge("e1", KgEdgeType.IMPLEMENTS, "A", "B"));

                List<KgEdge> outgoing = g.getNeighbors("A");
                assertEquals(1, outgoing.size());
                assertEquals("B", outgoing.get(0).targetNodeId());
            }

            @Test
            @DisplayName("Then edge appears in in-adjacency")
            void Then_EdgeAppearsInInAdjacency() {
                KnowledgeGraph g = graphWithTwoNodes();
                g.addEdge(edge("e1", KgEdgeType.IMPLEMENTS, "A", "B"));

                List<KgEdge> incoming = g.getIncoming("B");
                assertEquals(1, incoming.size());
                assertEquals("A", incoming.get(0).sourceNodeId());
            }

            @Test
            @DisplayName("Then edge is indexed by type")
            void Then_EdgeIsIndexedByType() {
                KnowledgeGraph g = graphWithTwoNodes();
                g.addEdge(edge("e1", KgEdgeType.IMPLEMENTS, "A", "B"));

                List<KgEdge> byType = g.edgesOfType(KgEdgeType.IMPLEMENTS);
                assertEquals(1, byType.size());
            }

            @Test
            @DisplayName("Then edge count is incremented")
            void Then_EdgeCountIsIncremented() {
                KnowledgeGraph g = graphWithTwoNodes();
                g.addEdge(edge("e1", KgEdgeType.IMPLEMENTS, "A", "B"));

                assertEquals(1, g.edgeCount());
            }
        }

        @Nested
        @DisplayName("When adding a null edge")
        class When_AddingNullEdge {

            @Test
            @DisplayName("Then edge count remains zero")
            void Then_EdgeCountRemainsZero() {
                KnowledgeGraph g = graphWithTwoNodes();
                g.addEdge(null);
                assertEquals(0, g.edgeCount());
            }
        }

        @Nested
        @DisplayName("When adding an edge with null id")
        class When_AddingEdgeWithNullId {

            @Test
            @DisplayName("Then edge count remains zero")
            void Then_EdgeCountRemainsZero() {
                KnowledgeGraph g = graphWithTwoNodes();
                g.addEdge(new KgEdge(null, KgEdgeType.CALLS, "A", "B", null, null, null, null));
                assertEquals(0, g.edgeCount());
            }
        }

        @Nested
        @DisplayName("When querying neighbors")
        class When_QueryingNeighbors {

            @Test
            @DisplayName("Then returns connected nodes")
            void Then_ReturnsConnectedNodes() {
                KnowledgeGraph g = graphWithTwoNodes();
                g.addEdge(edge("e1", KgEdgeType.CALLS, "A", "B"));

                List<KgEdge> neighbors = g.getNeighbors("A");
                Set<String> targets = neighbors.stream()
                        .map(KgEdge::targetNodeId)
                        .collect(Collectors.toSet());
                assertTrue(targets.contains("B"));
            }
        }

        @Nested
        @DisplayName("When querying incoming")
        class When_QueryingIncoming {

            @Test
            @DisplayName("Then returns source nodes")
            void Then_ReturnsSourceNodes() {
                KnowledgeGraph g = graphWithTwoNodes();
                g.addEdge(edge("e1", KgEdgeType.CALLS, "A", "B"));

                List<KgEdge> incoming = g.getIncoming("B");
                Set<String> sources = incoming.stream()
                        .map(KgEdge::sourceNodeId)
                        .collect(Collectors.toSet());
                assertTrue(sources.contains("A"));
            }
        }

        @Nested
        @DisplayName("When replacing a node with same id")
        class When_ReplacingNodeWithSameId {

            @Test
            @DisplayName("Then latest node wins")
            void Then_LatestNodeWins() {
                KnowledgeGraph g = new KnowledgeGraph();
                g.addNode(node("A", KgNodeType.CLASS));
                g.addNode(new KgNode("A", KgNodeType.INTERFACE, "A-v2", "qual.A", null, null, "SOURCE", null));

                assertEquals(KgNodeType.INTERFACE, g.getNode("A").type());
            }
        }
    }

    @Nested
    @DisplayName("Given a graph with cyclic edges")
    class Given_GraphWithCyclicEdges {

        private KnowledgeGraph cyclicGraph() {
            KnowledgeGraph g = new KnowledgeGraph();
            g.addNode(node("A", KgNodeType.CLASS));
            g.addNode(node("B", KgNodeType.CLASS));
            g.addNode(node("C", KgNodeType.CLASS));
            g.addEdge(edge("e1", KgEdgeType.CALLS, "A", "B"));
            g.addEdge(edge("e2", KgEdgeType.CALLS, "B", "C"));
            g.addEdge(edge("e3", KgEdgeType.CALLS, "C", "A"));
            return g;
        }

        @Nested
        @DisplayName("When building adjacency map")
        class When_BuildingAdjacencyMap {

            @Test
            @DisplayName("Then cycles are represented")
            void Then_CyclesRepresented() {
                KnowledgeGraph g = cyclicGraph();
                Map<String, Set<String>> adj = g.buildAdjacencyMap();

                assertTrue(adj.get("A").contains("B"));
                assertTrue(adj.get("B").contains("C"));
                assertTrue(adj.get("C").contains("A"));
            }
        }

        @Nested
        @DisplayName("When querying neighbors along cycle")
        class When_QueryingNeighborsAlongCycle {

            @Test
            @DisplayName("Then each node has outgoing edge")
            void Then_EachNodeHasOutgoingEdge() {
                KnowledgeGraph g = cyclicGraph();

                assertEquals(1, g.getNeighbors("A").size());
                assertEquals(1, g.getNeighbors("B").size());
                assertEquals(1, g.getNeighbors("C").size());
            }
        }
    }

    @Nested
    @DisplayName("Given a graph with a self-referencing edge")
    class Given_GraphWithSelfReferencingEdge {

        @Test
        @DisplayName("Then the self-loop appears in both out and in adjacency")
        void Then_SelfLoopAppearsInBothDirections() {
            KnowledgeGraph g = new KnowledgeGraph();
            g.addNode(node("X", KgNodeType.CLASS));
            g.addEdge(edge("self", KgEdgeType.CALLS, "X", "X"));

            assertEquals(1, g.getNeighbors("X").size());
            assertEquals(1, g.getIncoming("X").size());
        }
    }

    @Nested
    @DisplayName("Given a large graph with 1000 nodes")
    class Given_LargeGraph_1000Nodes {

        private KnowledgeGraph largeGraph() {
            KnowledgeGraph g = new KnowledgeGraph();
            for (int i = 0; i < 500; i++) {
                g.addNode(node("class-" + i, KgNodeType.CLASS));
            }
            for (int i = 500; i < 1000; i++) {
                g.addNode(node("iface-" + i, KgNodeType.INTERFACE));
            }
            return g;
        }

        @Nested
        @DisplayName("When querying by type")
        class When_QueryingByType {

            @Test
            @DisplayName("Then returns correct subset of CLASS nodes")
            void Then_ReturnsCorrectClassSubset() {
                KnowledgeGraph g = largeGraph();
                assertEquals(500, g.nodesOfType(KgNodeType.CLASS).size());
            }

            @Test
            @DisplayName("Then returns correct subset of INTERFACE nodes")
            void Then_ReturnsCorrectInterfaceSubset() {
                KnowledgeGraph g = largeGraph();
                assertEquals(500, g.nodesOfType(KgNodeType.INTERFACE).size());
            }

            @Test
            @DisplayName("Then total node count is 1000")
            void Then_TotalNodeCountIs1000() {
                KnowledgeGraph g = largeGraph();
                assertEquals(1000, g.nodeCount());
            }
        }
    }

    @Nested
    @DisplayName("Given a graph with multiple edge types")
    class Given_GraphWithMultipleEdgeTypes {

        private KnowledgeGraph multiEdgeGraph() {
            KnowledgeGraph g = new KnowledgeGraph();
            g.addNode(node("A", KgNodeType.CLASS));
            g.addNode(node("B", KgNodeType.INTERFACE));
            g.addEdge(edge("e1", KgEdgeType.IMPLEMENTS, "A", "B"));
            g.addEdge(edge("e2", KgEdgeType.CALLS, "A", "B"));
            g.addEdge(edge("e3", KgEdgeType.EXTENDS, "A", "B"));
            return g;
        }

        @Test
        @DisplayName("Then edges of each type are correctly segregated")
        void Then_EdgesOfEachTypeCorrectlySegregated() {
            KnowledgeGraph g = multiEdgeGraph();

            assertEquals(1, g.edgesOfType(KgEdgeType.IMPLEMENTS).size());
            assertEquals(1, g.edgesOfType(KgEdgeType.CALLS).size());
            assertEquals(1, g.edgesOfType(KgEdgeType.EXTENDS).size());
        }

        @Test
        @DisplayName("Then total edge count includes all types")
        void Then_TotalEdgeCountIncludesAllTypes() {
            KnowledgeGraph g = multiEdgeGraph();
            assertEquals(3, g.edgeCount());
        }
    }

    @Nested
    @DisplayName("Given a graph with edges having null source or target")
    class Given_GraphWithEdgesHavingNullSourceOrTarget {

        @Test
        @DisplayName("Then edge with null source does not appear in outEdges")
        void Then_EdgeWithNullSourceNotInOutEdges() {
            KnowledgeGraph g = new KnowledgeGraph();
            g.addNode(node("A", KgNodeType.CLASS));
            g.addEdge(new KgEdge("e1", KgEdgeType.CALLS, null, "A", null, null, null, Map.of()));

            assertTrue(g.getNeighbors("A").isEmpty());
        }

        @Test
        @DisplayName("Then edge with null target does not appear in inEdges")
        void Then_EdgeWithNullTargetNotInInEdges() {
            KnowledgeGraph g = new KnowledgeGraph();
            g.addNode(node("A", KgNodeType.CLASS));
            g.addEdge(new KgEdge("e1", KgEdgeType.CALLS, "A", null, null, null, null, Map.of()));

            assertTrue(g.getIncoming("A").isEmpty());
        }
    }

    @Nested
    @DisplayName("Given a graph returned from getNodes and getEdges")
    class Given_GraphAccessors {

        @Test
        @DisplayName("Then getNodes returns unmodifiable map")
        void Then_GetNodesReturnsUnmodifiableMap() {
            KnowledgeGraph g = new KnowledgeGraph();
            g.addNode(node("A", KgNodeType.CLASS));

            org.junit.jupiter.api.Assertions.assertThrows(
                    UnsupportedOperationException.class,
                    () -> g.getNodes().put("B", node("B", KgNodeType.CLASS))
            );
        }

        @Test
        @DisplayName("Then getEdges returns unmodifiable list")
        void Then_GetEdgesReturnsUnmodifiableList() {
            KnowledgeGraph g = new KnowledgeGraph();
            g.addEdge(edge("e1", KgEdgeType.CALLS, "A", "B"));

            org.junit.jupiter.api.Assertions.assertThrows(
                    UnsupportedOperationException.class,
                    () -> g.getEdges().add(edge("e2", KgEdgeType.CALLS, "C", "D"))
            );
        }
    }
}
