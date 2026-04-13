package com.codevision.codevisionbackend.graph.algorithm;

import static org.junit.jupiter.api.Assertions.*;

import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ImpactAnalyzerTest {

    private final ImpactAnalyzer analyzer = new ImpactAnalyzer();

    @Nested
    class Given_LinearChain {

        @Nested
        class When_AnalyzingImpactOfLeaf {

            @Test
            void Then_OnlyLeafIsImpacted() {
                var graph = GraphTestHelper.linearChain(4);
                var result = analyzer.analyzeImpact(graph, "n3");
                // n3 is the leaf; nothing depends on it
                assertEquals(1, result.size());
                assertTrue(result.contains("n3"));
            }
        }

        @Nested
        class When_AnalyzingImpactOfRoot {

            @Test
            void Then_AllNodesAreImpacted() {
                var graph = GraphTestHelper.linearChain(4);
                // Reverse closure: who calls n0? Nobody. But n0 calls n1, n1 calls n2, etc.
                // Impact = forward transitive closure (what does changing n0 break?)
                var result = analyzer.analyzeImpact(graph, "n0");
                assertEquals(4, result.size());
            }
        }
    }

    @Nested
    class Given_StarGraph {

        @Nested
        class When_AnalyzingImpactOfCenter {

            @Test
            void Then_AllSpokesAreImpacted() {
                var graph = GraphTestHelper.starGraph(3);
                var result = analyzer.analyzeImpact(graph, "center");
                // center -> spoke0, spoke1, spoke2; changing center impacts all
                assertEquals(4, result.size());
            }
        }
    }

    @Nested
    class Given_EmptyGraph {

        @Nested
        class When_AnalyzingImpact {

            @Test
            void Then_ReturnsEmptyForUnknownNode() {
                var graph = new KnowledgeGraph();
                var result = analyzer.analyzeImpact(graph, "nonexistent");
                assertTrue(result.isEmpty());
            }
        }
    }

    @Nested
    class Given_AlgorithmMetadata {

        @Nested
        class When_QueryingName {

            @Test
            void Then_ReturnsImpactAnalyzer() {
                assertEquals("impact-analyzer", analyzer.name());
            }
        }
    }
}
