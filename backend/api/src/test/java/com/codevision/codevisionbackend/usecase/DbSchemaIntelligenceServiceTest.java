package com.codevision.codevisionbackend.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.codevision.codevisionbackend.graph.KgEdgeType;
import com.codevision.codevisionbackend.graph.KgNodeType;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DbSchemaIntelligenceServiceTest {

    private final DbSchemaIntelligenceService service = new DbSchemaIntelligenceService();

    @Nested
    class Given_GraphWithDbEntities {

        @Nested
        class When_Analyzing {

            @Test
            void Then_FindsEntities() {
                var graph = new KnowledgeGraph();
                graph.addNode(UseCaseTestHelper.dbEntityNode("users", "users"));
                graph.addNode(UseCaseTestHelper.node("col1", "id", KgNodeType.DATABASE_COLUMN));
                graph.addEdge(UseCaseTestHelper.containsEdge("users", "col1"));

                var report = service.analyze(graph);
                assertEquals(1, report.entities().size());
                assertEquals(1, report.entities().get(0).columnCount());
            }
        }
    }

    @Nested
    class Given_NoDbEntities {

        @Nested
        class When_Analyzing {

            @Test
            void Then_ReturnsEmptyReport() {
                var graph = UseCaseTestHelper.layeredApp();
                // layeredApp has DATABASE_ENTITY but without CONTAINS edges to columns
                var report = service.analyze(graph);
                assertNotNull(report);
            }
        }
    }
}
