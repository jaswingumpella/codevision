package com.codevision.codevisionbackend.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OnboardingGuideServiceTest {

    private final OnboardingGuideService service = new OnboardingGuideService();

    @Nested
    class Given_LayeredApp {

        @Nested
        class When_GeneratingGuide {

            @Test
            void Then_StartsWithEndpoints() {
                var graph = UseCaseTestHelper.layeredApp();
                var guide = service.generate(graph);
                assertFalse(guide.steps().isEmpty());
                assertEquals("ENDPOINT", guide.steps().get(0).type());
            }

            @Test
            void Then_IncludesAllNodeTypes() {
                var graph = UseCaseTestHelper.layeredApp();
                var guide = service.generate(graph);
                assertTrue(guide.totalSteps() > 0);
            }
        }
    }

    @Nested
    class Given_EmptyGraph {

        @Nested
        class When_GeneratingGuide {

            @Test
            void Then_ReturnsEmptyGuide() {
                var graph = new KnowledgeGraph();
                var guide = service.generate(graph);
                assertTrue(guide.steps().isEmpty());
                assertEquals(0, guide.totalSteps());
            }
        }
    }
}
