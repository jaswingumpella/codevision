package com.codevision.codevisionbackend.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.codevision.codevisionbackend.graph.KgEdgeType;
import com.codevision.codevisionbackend.graph.KgNodeType;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SecurityScanServiceTest {

    private final SecurityScanService service = new SecurityScanService();

    @Nested
    class Given_SensitiveFields {

        @Nested
        class When_Scanning {

            @Test
            void Then_FindsSensitiveFieldNames() {
                var graph = new KnowledgeGraph();
                graph.addNode(UseCaseTestHelper.node("f1", "passwordHash", KgNodeType.FIELD));
                graph.addNode(UseCaseTestHelper.node("f2", "userName", KgNodeType.FIELD));

                var report = service.scan(graph);
                assertEquals(1, report.totalFindings());
                assertTrue(report.findings().get(0).category().equals("SENSITIVE_DATA"));
            }
        }
    }

    @Nested
    class Given_DirectDbAccess {

        @Nested
        class When_Scanning {

            @Test
            void Then_FindsDirectDbAccessFromEndpoint() {
                var graph = new KnowledgeGraph();
                graph.addNode(UseCaseTestHelper.endpointNode("ep1", "GET /data"));
                graph.addNode(UseCaseTestHelper.dbEntityNode("db1", "users"));
                graph.addEdge(UseCaseTestHelper.callsEdge("ep1", "db1"));

                var report = service.scan(graph);
                assertTrue(report.findings().stream()
                        .anyMatch(f -> f.category().equals("DIRECT_DB_ACCESS")));
            }
        }
    }

    @Nested
    class Given_CleanGraph {

        @Nested
        class When_Scanning {

            @Test
            void Then_NoFindings() {
                var graph = UseCaseTestHelper.layeredApp();
                var report = service.scan(graph);
                assertEquals(0, report.totalFindings());
            }
        }
    }
}
