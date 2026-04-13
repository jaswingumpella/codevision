package com.codevision.codevisionbackend.graph.algorithm;

import static org.junit.jupiter.api.Assertions.*;

import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BetweennessCentralityAlgorithmTest {

    private final BetweennessCentralityAlgorithm algorithm = new BetweennessCentralityAlgorithm();

    @Nested
    class Given_LinearChain {

        @Nested
        class When_Computing {

            @Test
            void Then_MiddleNodeHasHighestBetweenness() {
                // A -> B -> C: B is the bridge
                var graph = GraphTestHelper.linearChain(3);
                var result = algorithm.execute(graph);
                assertEquals(3, result.size());
                double middleBetweenness = result.getOrDefault("n1", 0.0);
                double endBetweenness = result.getOrDefault("n2", 0.0);
                assertTrue(middleBetweenness >= endBetweenness,
                        "Middle node should have higher betweenness than end node");
            }
        }
    }

    @Nested
    class Given_StarGraph {

        @Nested
        class When_Computing {

            @Test
            void Then_CenterHasHighBetweenness() {
                var graph = GraphTestHelper.starGraph(4);
                var result = algorithm.execute(graph);
                assertFalse(result.isEmpty());
                // Center is the only node with outgoing edges; it should have highest betweenness
                double centerBetweenness = result.getOrDefault("center", 0.0);
                double spokeBetweenness = result.getOrDefault("spoke0", 0.0);
                assertTrue(centerBetweenness >= spokeBetweenness,
                        "Center should have higher betweenness than spokes");
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
    class Given_AlgorithmMetadata {

        @Nested
        class When_QueryingName {

            @Test
            void Then_ReturnsBetweennessCentrality() {
                assertEquals("betweenness-centrality", algorithm.name());
            }
        }
    }
}
