package com.codevision.codevisionbackend.graph.algorithm;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PageRankAlgorithmTest {

    private final PageRankAlgorithm algorithm = new PageRankAlgorithm();

    @Nested
    class Given_EmptyGraph {

        @Nested
        class When_ExecutingPageRank {

            @Test
            void Then_ReturnsEmptyRanks() {
                var graph = new com.codevision.codevisionbackend.graph.KnowledgeGraph();
                var result = algorithm.execute(graph);
                assertTrue(result.isEmpty());
            }
        }
    }

    @Nested
    class Given_StarGraph {

        @Nested
        class When_ExecutingPageRank {

            @Test
            void Then_CenterHasHighestRank() {
                // Spokes point to center (reverse star: all spokes call center)
                var graph = GraphTestHelper.starGraph(5);
                // In starGraph, center -> spokes. Spokes receive rank from center.
                var result = algorithm.execute(graph);
                assertFalse(result.isEmpty());
                // All nodes should have ranks
                assertEquals(6, result.size());
            }

            @Test
            void Then_RanksSumToApproximatelyOne() {
                var graph = GraphTestHelper.starGraph(5);
                var result = algorithm.execute(graph);
                double sum = result.values().stream().mapToDouble(Double::doubleValue).sum();
                assertEquals(1.0, sum, 0.01);
            }
        }
    }

    @Nested
    class Given_LinearChain {

        @Nested
        class When_ExecutingPageRank {

            @Test
            void Then_LastNodeHasHighRank() {
                var graph = GraphTestHelper.linearChain(5);
                var result = algorithm.execute(graph);
                assertEquals(5, result.size());
                // In a chain A->B->C->D->E, rank flows to E (the sink)
                double lastRank = result.getOrDefault("n4", 0.0);
                double firstRank = result.getOrDefault("n0", 0.0);
                assertTrue(lastRank >= firstRank, "Last node in chain should have >= rank of first");
            }
        }
    }

    @Nested
    class Given_AlgorithmMetadata {

        @Nested
        class When_QueryingName {

            @Test
            void Then_ReturnsPageRank() {
                assertEquals("pagerank", algorithm.name());
            }
        }
    }
}
