package com.codevision.codevisionbackend.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DuplicationFinderServiceTest {

    private final DuplicationFinderService service = new DuplicationFinderService();

    @Nested
    class Given_ClassesWithSimilarStructure {

        @Nested
        class When_FindingDuplicates {

            @Test
            void Then_GroupsSimilarClasses() {
                var graph = new KnowledgeGraph();
                graph.addNode(UseCaseTestHelper.classNode("a", "ServiceA"));
                graph.addNode(UseCaseTestHelper.classNode("b", "ServiceB"));
                graph.addNode(UseCaseTestHelper.classNode("target", "Repository"));
                // Both call the same target with same edge type
                graph.addEdge(UseCaseTestHelper.callsEdge("a", "target"));
                graph.addEdge(UseCaseTestHelper.callsEdge("b", "target"));

                var report = service.find(graph);
                assertFalse(report.groups().isEmpty());
            }
        }
    }

    @Nested
    class Given_EmptyGraph {

        @Nested
        class When_FindingDuplicates {

            @Test
            void Then_ReturnsEmptyReport() {
                var graph = new KnowledgeGraph();
                var report = service.find(graph);
                assertTrue(report.groups().isEmpty());
                assertEquals(0, report.totalDuplicates());
            }
        }
    }
}
