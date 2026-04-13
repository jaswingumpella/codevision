package com.codevision.codevisionbackend.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.codevision.codevisionbackend.graph.KgNodeType;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DependencyAuditServiceTest {

    private final DependencyAuditService service = new DependencyAuditService();

    @Nested
    class Given_GraphWithDependencies {

        @Nested
        class When_Auditing {

            @Test
            void Then_FindsDependencyArtifacts() {
                var graph = new KnowledgeGraph();
                graph.addNode(UseCaseTestHelper.node("dep1", "spring-core", KgNodeType.DEPENDENCY_ARTIFACT));
                graph.addNode(UseCaseTestHelper.classNode("svc", "UserService"));
                graph.addEdge(UseCaseTestHelper.dependsOnEdge("svc", "dep1"));

                var report = service.audit(graph);
                assertEquals(1, report.totalDependencies());
                assertFalse(report.risks().isEmpty());
            }
        }
    }

    @Nested
    class Given_NoDependencies {

        @Nested
        class When_Auditing {

            @Test
            void Then_ReturnsEmptyReport() {
                var graph = UseCaseTestHelper.layeredApp();
                var report = service.audit(graph);
                assertEquals(0, report.totalDependencies());
            }
        }
    }
}
