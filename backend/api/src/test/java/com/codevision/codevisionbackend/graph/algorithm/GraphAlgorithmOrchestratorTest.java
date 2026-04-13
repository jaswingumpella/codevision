package com.codevision.codevisionbackend.graph.algorithm;

import static org.junit.jupiter.api.Assertions.*;

import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class GraphAlgorithmOrchestratorTest {

    @Nested
    class Given_RegisteredAlgorithms {

        @Nested
        class When_RunningAll {

            @Test
            void Then_AllAlgorithmsExecute() {
                var orchestrator = new GraphAlgorithmOrchestrator(List.of(
                        new PageRankAlgorithm(),
                        new DeadCodeDetector()
                ));
                var graph = GraphTestHelper.linearChain(3);
                var results = orchestrator.runAll(graph);
                assertEquals(2, results.size());
                assertTrue(results.containsKey("pagerank"));
                assertTrue(results.containsKey("dead-code-detector"));
            }
        }
    }

    @Nested
    class Given_EmptyAlgorithmList {

        @Nested
        class When_RunningAll {

            @Test
            void Then_ReturnsEmptyResults() {
                var orchestrator = new GraphAlgorithmOrchestrator(List.of());
                var graph = new KnowledgeGraph();
                var results = orchestrator.runAll(graph);
                assertTrue(results.isEmpty());
            }
        }
    }

    @Nested
    class Given_AlgorithmByName {

        @Nested
        class When_QueryingSingle {

            @Test
            void Then_ReturnsCorrectAlgorithm() {
                var orchestrator = new GraphAlgorithmOrchestrator(List.of(
                        new PageRankAlgorithm(),
                        new DeadCodeDetector()
                ));
                var algo = orchestrator.findByName("pagerank");
                assertTrue(algo.isPresent());
                assertEquals("pagerank", algo.get().name());
            }

            @Test
            void Then_ReturnsEmptyForUnknown() {
                var orchestrator = new GraphAlgorithmOrchestrator(List.of());
                var algo = orchestrator.findByName("nonexistent");
                assertTrue(algo.isEmpty());
            }
        }
    }

    @Nested
    class Given_FailingAlgorithm {

        @Nested
        class When_RunningAll {

            @Test
            void Then_IsolatesFailureAndContinues() {
                var failingAlgorithm = new GraphAlgorithm<Object>() {
                    @Override
                    public String name() { return "failing"; }
                    @Override
                    public Object execute(com.codevision.codevisionbackend.graph.KnowledgeGraph graph) {
                        throw new RuntimeException("intentional failure");
                    }
                };
                var orchestrator = new GraphAlgorithmOrchestrator(List.of(
                        failingAlgorithm,
                        new PageRankAlgorithm()
                ));
                var graph = GraphTestHelper.linearChain(3);
                var results = orchestrator.runAll(graph);
                assertEquals(2, results.size());
                assertTrue(results.containsKey("failing"));
                assertTrue(results.containsKey("pagerank"));
            }
        }
    }
}
