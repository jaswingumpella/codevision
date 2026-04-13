package com.codevision.codevisionbackend.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.codevision.codevisionbackend.graph.KgNodeType;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MigrationPlannerServiceTest {

    private final MigrationPlannerService service = new MigrationPlannerService();

    @Nested
    class Given_DependencyWithDependents {

        @Nested
        class When_PlanningMigration {

            @Test
            void Then_ShowsImpactedCode() {
                var graph = new KnowledgeGraph();
                graph.addNode(UseCaseTestHelper.node("lib", "old-lib", KgNodeType.DEPENDENCY_ARTIFACT));
                graph.addNode(UseCaseTestHelper.classNode("svc", "UserService"));
                graph.addEdge(UseCaseTestHelper.callsEdge("lib", "svc"));

                var plan = service.plan(graph, "lib");
                assertTrue(plan.impactedCount() > 0);
            }
        }
    }

    @Nested
    class Given_UnknownArtifact {

        @Nested
        class When_PlanningMigration {

            @Test
            void Then_ReturnsEmptyPlan() {
                var graph = new KnowledgeGraph();
                var plan = service.plan(graph, "nonexistent");
                assertEquals(0, plan.impactedCount());
            }
        }
    }
}
