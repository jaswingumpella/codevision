package com.codevision.codevisionbackend.graph.algorithm;

import static org.junit.jupiter.api.Assertions.*;

import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

class DependencyRiskScorerTest {

    private final DependencyRiskScorer scorer = new DependencyRiskScorer();

    @Nested
    class Given_HighFanInNode {

        @Nested
        class When_Scoring {

            @Test
            void Then_HasHigherRisk() {
                // A node that many others depend on (high fan-in) should have higher risk
                // Also give target some fan-out so it dominates on both dimensions
                var graph = new KnowledgeGraph();
                graph.addNode(GraphTestHelper.classNode("target", "Target"));
                graph.addNode(GraphTestHelper.classNode("leaf", "Leaf"));
                for (int i = 0; i < 10; i++) {
                    graph.addNode(GraphTestHelper.classNode("src" + i, "Source" + i));
                    graph.addEdge(GraphTestHelper.dependsOnEdge("src" + i, "target"));
                }
                // target also calls leaf, giving it fan-out
                graph.addEdge(GraphTestHelper.callsEdge("target", "leaf"));

                Map<String, Double> result = scorer.execute(graph);

                double targetRisk = result.get("target");
                // target has max fan-in and also fan-out, so it should score highest
                for (int i = 0; i < 10; i++) {
                    assertTrue(targetRisk > result.get("src" + i),
                            "High fan-in node should have higher risk than sources");
                }
            }
        }
    }

    @Nested
    class Given_HighFanOutNode {

        @Nested
        class When_Scoring {

            @Test
            void Then_HasHigherRisk() {
                // A star graph center has high fan-out
                var graph = GraphTestHelper.starGraph(5);

                Map<String, Double> result = scorer.execute(graph);

                double centerRisk = result.get("center");
                for (int i = 0; i < 5; i++) {
                    assertTrue(centerRisk > result.get("spoke" + i),
                            "High fan-out center should have higher risk than spokes");
                }
            }
        }
    }

    @Nested
    class Given_NodeInCycle {

        @Nested
        class When_Scoring {

            @Test
            void Then_HasHigherRisk() {
                // Create a graph with a cycle (A -> B -> C -> A) and an isolated node D
                var graph = new KnowledgeGraph();
                graph.addNode(GraphTestHelper.classNode("a", "A"));
                graph.addNode(GraphTestHelper.classNode("b", "B"));
                graph.addNode(GraphTestHelper.classNode("c", "C"));
                graph.addNode(GraphTestHelper.classNode("d", "D"));
                graph.addEdge(GraphTestHelper.callsEdge("a", "b"));
                graph.addEdge(GraphTestHelper.callsEdge("b", "c"));
                graph.addEdge(GraphTestHelper.callsEdge("c", "a"));
                // D has one outgoing edge but is not in a cycle
                graph.addEdge(GraphTestHelper.callsEdge("d", "a"));

                Map<String, Double> result = scorer.execute(graph);

                // Cycle participants (a, b, c) should score higher than d
                double dRisk = result.get("d");
                assertTrue(result.get("a") > dRisk,
                        "Cycle participant 'a' should have higher risk than non-cycle node 'd'");
                assertTrue(result.get("b") > dRisk,
                        "Cycle participant 'b' should have higher risk than non-cycle node 'd'");
                assertTrue(result.get("c") > dRisk,
                        "Cycle participant 'c' should have higher risk than non-cycle node 'd'");
            }
        }
    }

    @Nested
    class Given_IsolatedNode {

        @Nested
        class When_Scoring {

            @Test
            void Then_HasLowRisk() {
                var graph = new KnowledgeGraph();
                graph.addNode(GraphTestHelper.classNode("isolated", "Isolated"));

                Map<String, Double> result = scorer.execute(graph);

                assertEquals(0.0, result.get("isolated"),
                        "Isolated node with no edges should have zero risk");
            }
        }
    }

    @Nested
    class Given_EmptyGraph {

        @Nested
        class When_Scoring {

            @Test
            void Then_ReturnsEmpty() {
                var graph = new KnowledgeGraph();

                Map<String, Double> result = scorer.execute(graph);

                assertTrue(result.isEmpty(), "Empty graph should produce empty result");
            }
        }
    }

    @Nested
    class Given_Metadata {

        @Nested
        class When_QueryingName {

            @Test
            void Then_ReturnsDependencyRisk() {
                assertEquals("dependency-risk", scorer.name());
            }
        }
    }
}
