package com.codevision.codevisionbackend.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("KnowledgeGraphBuilder")
class KnowledgeGraphBuilderTest {

    // ── Fixture helpers ─────────────────────────────────────────────────

    private static KgNode node(String id, KgNodeType type) {
        return new KgNode(id, type, id, "qual." + id, null, null, "SOURCE", null);
    }

    private static KgEdge edge(String id, KgEdgeType type, String source, String target) {
        return new KgEdge(id, type, source, target, null, ConfidenceLevel.EXTRACTED, null, Map.of());
    }

    // ── Tests ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Given an empty builder")
    class Given_EmptyBuilder {

        @Nested
        @DisplayName("When building")
        class When_Building {

            @Test
            @DisplayName("Then returns an empty graph")
            void Then_ReturnsEmptyGraph() {
                KnowledgeGraph graph = new KnowledgeGraphBuilder().build();

                assertNotNull(graph);
                assertEquals(0, graph.nodeCount());
                assertEquals(0, graph.edgeCount());
            }
        }
    }

    @Nested
    @DisplayName("Given a builder with nodes")
    class Given_BuilderWithNodes {

        @Nested
        @DisplayName("When building")
        class When_Building {

            @Test
            @DisplayName("Then all nodes are present")
            void Then_AllNodesPresent() {
                KnowledgeGraph graph = new KnowledgeGraphBuilder()
                        .withNode(node("A", KgNodeType.CLASS))
                        .withNode(node("B", KgNodeType.INTERFACE))
                        .withNode(node("C", KgNodeType.ENUM))
                        .build();

                assertEquals(3, graph.nodeCount());
                assertNotNull(graph.getNode("A"));
                assertNotNull(graph.getNode("B"));
                assertNotNull(graph.getNode("C"));
            }
        }

        @Nested
        @DisplayName("When adding a null node")
        class When_AddingNullNode {

            @Test
            @DisplayName("Then null is silently ignored")
            void Then_NullIsIgnored() {
                KnowledgeGraph graph = new KnowledgeGraphBuilder()
                        .withNode(node("A", KgNodeType.CLASS))
                        .withNode(null)
                        .build();

                assertEquals(1, graph.nodeCount());
            }
        }
    }

    @Nested
    @DisplayName("Given a builder with edges")
    class Given_BuilderWithEdges {

        @Nested
        @DisplayName("When building")
        class When_Building {

            @Test
            @DisplayName("Then all edges are present")
            void Then_AllEdgesPresent() {
                KnowledgeGraph graph = new KnowledgeGraphBuilder()
                        .withNode(node("A", KgNodeType.CLASS))
                        .withNode(node("B", KgNodeType.INTERFACE))
                        .withEdge(edge("e1", KgEdgeType.IMPLEMENTS, "A", "B"))
                        .withEdge(edge("e2", KgEdgeType.CALLS, "A", "B"))
                        .build();

                assertEquals(2, graph.edgeCount());
            }

            @Test
            @DisplayName("Then adjacency indexes are populated")
            void Then_AdjacencyIndexesPopulated() {
                KnowledgeGraph graph = new KnowledgeGraphBuilder()
                        .withNode(node("A", KgNodeType.CLASS))
                        .withNode(node("B", KgNodeType.INTERFACE))
                        .withEdge(edge("e1", KgEdgeType.IMPLEMENTS, "A", "B"))
                        .build();

                assertEquals(1, graph.getNeighbors("A").size());
                assertEquals(1, graph.getIncoming("B").size());
            }
        }

        @Nested
        @DisplayName("When adding a null edge")
        class When_AddingNullEdge {

            @Test
            @DisplayName("Then null is silently ignored")
            void Then_NullIsIgnored() {
                KnowledgeGraph graph = new KnowledgeGraphBuilder()
                        .withNode(node("A", KgNodeType.CLASS))
                        .withEdge(null)
                        .build();

                assertEquals(0, graph.edgeCount());
            }
        }
    }

    @Nested
    @DisplayName("Given a builder used with fluent chaining")
    class Given_FluentChaining {

        @Test
        @DisplayName("Then each withNode returns the same builder instance")
        void Then_ReturnsBuilderForChaining() {
            KnowledgeGraphBuilder builder = new KnowledgeGraphBuilder();
            KnowledgeGraphBuilder returned = builder.withNode(node("A", KgNodeType.CLASS));
            assertEquals(builder, returned);
        }

        @Test
        @DisplayName("Then each withEdge returns the same builder instance")
        void Then_WithEdgeReturnsSameBuilder() {
            KnowledgeGraphBuilder builder = new KnowledgeGraphBuilder();
            KnowledgeGraphBuilder returned = builder.withEdge(edge("e1", KgEdgeType.CALLS, "A", "B"));
            assertEquals(builder, returned);
        }
    }
}
