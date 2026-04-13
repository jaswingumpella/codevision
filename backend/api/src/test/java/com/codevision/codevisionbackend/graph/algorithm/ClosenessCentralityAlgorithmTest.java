package com.codevision.codevisionbackend.graph.algorithm;

import static org.junit.jupiter.api.Assertions.*;

import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ClosenessCentralityAlgorithmTest {

    private final ClosenessCentralityAlgorithm algorithm = new ClosenessCentralityAlgorithm();

    @Nested
    class Given_StarGraph {

        @Nested
        class When_Computing {

            @Test
            void Then_CenterHasHighestCloseness() {
                var graph = GraphTestHelper.starGraph(4);
                var result = algorithm.execute(graph);

                assertEquals(5, result.size());
                double centerCloseness = result.get("center");
                for (int i = 0; i < 4; i++) {
                    double spokeCloseness = result.get("spoke" + i);
                    assertTrue(centerCloseness > spokeCloseness,
                            "Center closeness (%f) should be higher than spoke%d closeness (%f)"
                                    .formatted(centerCloseness, i, spokeCloseness));
                }
            }
        }
    }

    @Nested
    class Given_LinearChain {

        @Nested
        class When_Computing {

            @Test
            void Then_MiddleNodesHaveHigherCloseness() {
                // n0 -> n1 -> n2 -> n3 -> n4
                var graph = GraphTestHelper.linearChain(5);
                var result = algorithm.execute(graph);

                assertEquals(5, result.size());
                double endCloseness = result.get("n0");
                double middleCloseness = result.get("n2");
                assertTrue(middleCloseness > endCloseness,
                        "Middle node closeness (%f) should be higher than endpoint closeness (%f)"
                                .formatted(middleCloseness, endCloseness));

                double otherEndCloseness = result.get("n4");
                assertTrue(middleCloseness > otherEndCloseness,
                        "Middle node closeness (%f) should be higher than other endpoint closeness (%f)"
                                .formatted(middleCloseness, otherEndCloseness));
            }
        }
    }

    @Nested
    class Given_DisconnectedGraph {

        @Nested
        class When_Computing {

            @Test
            void Then_IsolatedNodeHasZeroCloseness() {
                var graph = GraphTestHelper.linearChain(3);
                // Add an isolated node with no edges
                graph.addNode(GraphTestHelper.classNode("isolated", "Isolated"));

                var result = algorithm.execute(graph);

                assertEquals(4, result.size());
                assertEquals(0.0, result.get("isolated"),
                        "Isolated node should have zero closeness centrality");
            }
        }
    }

    @Nested
    class Given_SingleNode {

        @Nested
        class When_Computing {

            @Test
            void Then_ReturnsZero() {
                var graph = new KnowledgeGraph();
                graph.addNode(GraphTestHelper.classNode("only", "Only"));

                var result = algorithm.execute(graph);

                assertEquals(1, result.size());
                assertEquals(0.0, result.get("only"),
                        "Single node should have zero closeness centrality");
            }
        }
    }

    @Nested
    class Given_EmptyGraph {

        @Nested
        class When_Computing {

            @Test
            void Then_ReturnsEmpty() {
                var graph = new KnowledgeGraph();
                var result = algorithm.execute(graph);
                assertTrue(result.isEmpty());
            }
        }
    }

    @Nested
    class Given_ShortDeadline {

        @Nested
        class When_Computing {

            @Test
            void Then_StillReturnsResults() {
                var shortDeadlineAlgorithm = new ClosenessCentralityAlgorithm(0);
                var graph = GraphTestHelper.starGraph(4);

                var result = shortDeadlineAlgorithm.execute(graph);

                assertNotNull(result, "Result should not be null even with expired deadline");
                // With 0-second deadline, the loop may time out immediately,
                // filling remaining nodes with 0.0
                assertEquals(5, result.size(),
                        "All nodes should be present in result (timeout fills remaining with 0.0)");
            }
        }
    }

    @Nested
    class Given_Metadata {

        @Nested
        class When_QueryingName {

            @Test
            void Then_ReturnsClosenessCentrality() {
                assertEquals("closeness-centrality", algorithm.name());
            }
        }
    }
}
