package com.codevision.codevisionbackend.graph.algorithm;

import static org.junit.jupiter.api.Assertions.*;

import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class GodNodeDetectorTest {

    private final GodNodeDetector detector = new GodNodeDetector();

    @Nested
    class Given_GraphWithHighDegreeNode {

        @Nested
        class When_Detecting {

            @Test
            void Then_HighDegreeNodeIsGod() {
                // Center connects to 20 spokes
                var graph = GraphTestHelper.starGraph(20);
                var result = detector.execute(graph);
                assertTrue(result.contains("center"),
                        "Center of 20-spoke star should be detected as god node");
            }
        }
    }

    @Nested
    class Given_GraphWithNormalDegrees {

        @Nested
        class When_Detecting {

            @Test
            void Then_NoGodNodesFound() {
                var graph = GraphTestHelper.linearChain(5);
                var result = detector.execute(graph);
                assertTrue(result.isEmpty(),
                        "Linear chain should have no god nodes");
            }
        }
    }

    @Nested
    class Given_EmptyGraph {

        @Nested
        class When_Detecting {

            @Test
            void Then_ReturnsEmpty() {
                var graph = new KnowledgeGraph();
                var result = detector.execute(graph);
                assertTrue(result.isEmpty());
            }
        }
    }

    @Nested
    class Given_AlgorithmMetadata {

        @Nested
        class When_QueryingName {

            @Test
            void Then_ReturnsGodNodeDetector() {
                assertEquals("god-node-detector", detector.name());
            }
        }
    }
}
