package com.codevision.codevisionbackend.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ArchitectureHealthServiceTest {

    private final ArchitectureHealthService service = new ArchitectureHealthService();

    @Nested
    class Given_HealthyArchitecture {

        @Nested
        class When_ScoringHealth {

            @Test
            void Then_ScoreIsBetween0And100() {
                var graph = UseCaseTestHelper.layeredApp();
                var result = service.score(graph);
                assertTrue(result.score() >= 0 && result.score() <= 100);
            }

            @Test
            void Then_IncludesComponentBreakdown() {
                var graph = UseCaseTestHelper.layeredApp();
                var result = service.score(graph);
                assertFalse(result.components().isEmpty());
            }
        }
    }

    @Nested
    class Given_EmptyGraph {

        @Nested
        class When_ScoringHealth {

            @Test
            void Then_ReturnsZeroScore() {
                var graph = new KnowledgeGraph();
                var result = service.score(graph);
                assertEquals(0, result.score());
            }
        }
    }
}
