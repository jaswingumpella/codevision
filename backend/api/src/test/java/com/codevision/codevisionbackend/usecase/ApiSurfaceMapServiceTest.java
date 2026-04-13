package com.codevision.codevisionbackend.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApiSurfaceMapServiceTest {

    private final ApiSurfaceMapService service = new ApiSurfaceMapService();

    @Nested
    class Given_AppWithEndpoints {

        @Nested
        class When_MappingApiSurface {

            @Test
            void Then_FindsEndpointChains() {
                var graph = UseCaseTestHelper.layeredApp();
                var result = service.map(graph);
                assertFalse(result.chains().isEmpty());
                // Should find: ep1 -> svc -> repo -> entity
                var chain = result.chains().get(0);
                assertTrue(chain.endpointId().equals("ep1"));
            }
        }
    }

    @Nested
    class Given_AppWithNoEndpoints {

        @Nested
        class When_MappingApiSurface {

            @Test
            void Then_ReturnsEmptyChains() {
                var graph = new KnowledgeGraph();
                graph.addNode(UseCaseTestHelper.classNode("svc", "Service"));
                var result = service.map(graph);
                assertTrue(result.chains().isEmpty());
            }
        }
    }
}
