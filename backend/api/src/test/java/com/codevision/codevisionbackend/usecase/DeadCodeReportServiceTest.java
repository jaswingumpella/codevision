package com.codevision.codevisionbackend.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DeadCodeReportServiceTest {

    private final DeadCodeReportService service = new DeadCodeReportService();

    @Nested
    class Given_GraphWithOrphanCode {

        @Nested
        class When_GeneratingReport {

            @Test
            void Then_ReportContainsOrphanNodes() {
                var graph = UseCaseTestHelper.layeredApp();
                graph.addNode(UseCaseTestHelper.classNode("orphan", "UnusedHelper"));
                var report = service.generate(graph);
                assertFalse(report.deadNodes().isEmpty());
                assertTrue(report.deadNodes().stream().anyMatch(n -> n.contains("UnusedHelper")));
            }

            @Test
            void Then_ReportIncludesSummary() {
                var graph = UseCaseTestHelper.layeredApp();
                graph.addNode(UseCaseTestHelper.classNode("orphan", "UnusedHelper"));
                var report = service.generate(graph);
                assertTrue(report.totalNodes() > 0);
                assertTrue(report.deadPercentage() > 0);
            }
        }
    }

    @Nested
    class Given_FullyConnectedGraph {

        @Nested
        class When_GeneratingReport {

            @Test
            void Then_NoDeadCode() {
                var graph = UseCaseTestHelper.layeredApp();
                var report = service.generate(graph);
                assertTrue(report.deadNodes().isEmpty());
            }
        }
    }

    @Nested
    class Given_EmptyGraph {

        @Nested
        class When_GeneratingReport {

            @Test
            void Then_ReturnsEmptyReport() {
                var graph = new KnowledgeGraph();
                var report = service.generate(graph);
                assertNotNull(report);
                assertEquals(0, report.totalNodes());
            }
        }
    }
}
