package com.codevision.codevisionbackend.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ImpactAnalysisServiceTest {

    private final ImpactAnalysisService service = new ImpactAnalysisService();

    @Nested
    class Given_LayeredApp {

        @Nested
        class When_AnalyzingImpactOfService {

            @Test
            void Then_DownstreamNodesAreImpacted() {
                var graph = UseCaseTestHelper.layeredApp();
                var result = service.analyzeImpact(graph, "svc");
                assertTrue(result.impactedNodeIds().contains("repo"));
                assertTrue(result.impactedNodeIds().contains("svc"));
            }

            @Test
            void Then_UpstreamNodesAreNotInForwardImpact() {
                var graph = UseCaseTestHelper.layeredApp();
                var result = service.analyzeImpact(graph, "repo");
                assertFalse(result.impactedNodeIds().contains("ep1"));
            }
        }
    }

    @Nested
    class Given_UnknownNode {

        @Nested
        class When_AnalyzingImpact {

            @Test
            void Then_ReturnsEmptyImpact() {
                var graph = UseCaseTestHelper.layeredApp();
                var result = service.analyzeImpact(graph, "nonexistent");
                assertTrue(result.impactedNodeIds().isEmpty());
            }
        }
    }
}
