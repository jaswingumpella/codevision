package com.codevision.codevisionbackend.graph.algorithm;

import static org.junit.jupiter.api.Assertions.*;

import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CouplingAnalyzerTest {

    private final CouplingAnalyzer analyzer = new CouplingAnalyzer();

    @Nested
    class Given_StarGraph {

        @Nested
        class When_AnalyzingCoupling {

            @Test
            void Then_CenterHasHighEfferentCoupling() {
                var graph = GraphTestHelper.starGraph(5);
                var result = analyzer.execute(graph);
                var centerCoupling = result.get("center");
                assertNotNull(centerCoupling);
                assertEquals(5, centerCoupling.efferent(), "Center should have efferent coupling of 5");
                assertEquals(0, centerCoupling.afferent(), "Center should have 0 afferent coupling");
            }

            @Test
            void Then_SpokesHaveHighAfferentCoupling() {
                var graph = GraphTestHelper.starGraph(5);
                var result = analyzer.execute(graph);
                var spokeCoupling = result.get("spoke0");
                assertNotNull(spokeCoupling);
                assertEquals(1, spokeCoupling.afferent());
                assertEquals(0, spokeCoupling.efferent());
            }
        }
    }

    @Nested
    class Given_EmptyGraph {

        @Nested
        class When_AnalyzingCoupling {

            @Test
            void Then_ReturnsEmpty() {
                var graph = new KnowledgeGraph();
                var result = analyzer.execute(graph);
                assertTrue(result.isEmpty());
            }
        }
    }

    @Nested
    class Given_AlgorithmMetadata {

        @Nested
        class When_QueryingName {

            @Test
            void Then_ReturnsCouplingAnalyzer() {
                assertEquals("coupling-analyzer", analyzer.name());
            }
        }
    }
}
