package com.codevision.codevisionbackend.graph.algorithm;

import static org.junit.jupiter.api.Assertions.*;

import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TarjanSccAlgorithmTest {

    private final TarjanSccAlgorithm algorithm = new TarjanSccAlgorithm();

    @Nested
    class Given_GraphWithCycle {

        @Nested
        class When_FindingSCCs {

            @Test
            void Then_DetectsCyclicComponent() {
                var graph = new KnowledgeGraph();
                graph.addNode(GraphTestHelper.classNode("a", "A"));
                graph.addNode(GraphTestHelper.classNode("b", "B"));
                graph.addNode(GraphTestHelper.classNode("c", "C"));
                graph.addEdge(GraphTestHelper.callsEdge("a", "b"));
                graph.addEdge(GraphTestHelper.callsEdge("b", "c"));
                graph.addEdge(GraphTestHelper.callsEdge("c", "a"));

                var result = algorithm.execute(graph);
                assertEquals(1, result.size(), "Should find one SCC for the cycle");
                assertEquals(3, result.get(0).size(), "SCC should contain all 3 nodes");
            }
        }
    }

    @Nested
    class Given_AcyclicGraph {

        @Nested
        class When_FindingSCCs {

            @Test
            void Then_NoNonTrivialSCCs() {
                var graph = GraphTestHelper.linearChain(5);
                var result = algorithm.execute(graph);
                assertTrue(result.isEmpty(), "DAG should have no non-trivial SCCs");
            }
        }
    }

    @Nested
    class Given_MultipleCycles {

        @Nested
        class When_FindingSCCs {

            @Test
            void Then_DetectsAllCycles() {
                var graph = new KnowledgeGraph();
                // Cycle 1: a <-> b
                graph.addNode(GraphTestHelper.classNode("a", "A"));
                graph.addNode(GraphTestHelper.classNode("b", "B"));
                graph.addEdge(GraphTestHelper.callsEdge("a", "b"));
                graph.addEdge(GraphTestHelper.callsEdge("b", "a"));
                // Cycle 2: c <-> d
                graph.addNode(GraphTestHelper.classNode("c", "C"));
                graph.addNode(GraphTestHelper.classNode("d", "D"));
                graph.addEdge(GraphTestHelper.callsEdge("c", "d"));
                graph.addEdge(GraphTestHelper.callsEdge("d", "c"));
                // Bridge (no cycle)
                graph.addEdge(GraphTestHelper.callsEdge("b", "c"));

                var result = algorithm.execute(graph);
                assertEquals(2, result.size(), "Should find 2 non-trivial SCCs");
            }
        }
    }

    @Nested
    class Given_EmptyGraph {

        @Nested
        class When_FindingSCCs {

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
            void Then_ReturnsTarjanScc() {
                assertEquals("tarjan-scc", algorithm.name());
            }
        }
    }
}
