package com.codevision.codevisionbackend.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CycleBreakerServiceTest {

    private final CycleBreakerService service = new CycleBreakerService();

    @Nested
    class Given_GraphWithCycle {

        @Nested
        class When_AnalyzingCycles {

            @Test
            void Then_FindsCyclesWithSuggestions() {
                var graph = new KnowledgeGraph();
                graph.addNode(UseCaseTestHelper.classNode("a", "ClassA"));
                graph.addNode(UseCaseTestHelper.classNode("b", "ClassB"));
                graph.addNode(UseCaseTestHelper.classNode("c", "ClassC"));
                graph.addEdge(UseCaseTestHelper.callsEdge("a", "b"));
                graph.addEdge(UseCaseTestHelper.callsEdge("b", "c"));
                graph.addEdge(UseCaseTestHelper.callsEdge("c", "a"));

                var result = service.analyze(graph);
                assertFalse(result.cycles().isEmpty());
                assertEquals(3, result.cycles().get(0).nodeIds().size());
            }
        }
    }

    @Nested
    class Given_AcyclicGraph {

        @Nested
        class When_AnalyzingCycles {

            @Test
            void Then_NoCyclesFound() {
                var graph = UseCaseTestHelper.layeredApp();
                var result = service.analyze(graph);
                assertTrue(result.cycles().isEmpty());
            }
        }
    }
}
