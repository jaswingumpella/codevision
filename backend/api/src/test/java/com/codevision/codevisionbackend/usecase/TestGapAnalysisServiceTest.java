package com.codevision.codevisionbackend.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.codevision.codevisionbackend.graph.KgEdgeType;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TestGapAnalysisServiceTest {

    private final TestGapAnalysisService service = new TestGapAnalysisService();

    @Nested
    class Given_ClassesWithAndWithoutTests {

        @Nested
        class When_AnalyzingTestGaps {

            @Test
            void Then_FindsUntestedClasses() {
                var graph = new KnowledgeGraph();
                graph.addNode(UseCaseTestHelper.classNode("svc", "UserService"));
                graph.addNode(UseCaseTestHelper.classNode("repo", "UserRepository"));
                graph.addNode(UseCaseTestHelper.testNode("test1", "UserServiceTest"));
                graph.addEdge(UseCaseTestHelper.testsEdge("test1", "svc"));
                // repo has no test

                var result = service.analyze(graph);
                assertFalse(result.untestedClasses().isEmpty());
                assertTrue(result.untestedClasses().stream()
                        .anyMatch(c -> c.contains("UserRepository")));
            }

            @Test
            void Then_TestedClassesNotInGaps() {
                var graph = new KnowledgeGraph();
                graph.addNode(UseCaseTestHelper.classNode("svc", "UserService"));
                graph.addNode(UseCaseTestHelper.testNode("test1", "UserServiceTest"));
                graph.addEdge(UseCaseTestHelper.testsEdge("test1", "svc"));

                var result = service.analyze(graph);
                assertTrue(result.untestedClasses().stream()
                        .noneMatch(c -> c.contains("UserService")));
            }
        }
    }

    @Nested
    class Given_EmptyGraph {

        @Nested
        class When_AnalyzingTestGaps {

            @Test
            void Then_ReturnsEmptyReport() {
                var graph = new KnowledgeGraph();
                var result = service.analyze(graph);
                assertTrue(result.untestedClasses().isEmpty());
            }
        }
    }
}
