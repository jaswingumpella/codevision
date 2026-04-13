package com.codevision.codevisionbackend.graph.algorithm;

import static org.junit.jupiter.api.Assertions.*;

import com.codevision.codevisionbackend.graph.KgEdgeType;
import com.codevision.codevisionbackend.graph.KgNodeType;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DeadCodeDetectorTest {

    private final DeadCodeDetector detector = new DeadCodeDetector();

    @Nested
    class Given_GraphWithOrphanNodes {

        @Nested
        class When_DetectingDeadCode {

            @Test
            void Then_OrphansAreDetected() {
                var graph = new KnowledgeGraph();
                graph.addNode(GraphTestHelper.endpointNode("ep1", "GET /api"));
                graph.addNode(GraphTestHelper.classNode("svc", "ServiceClass"));
                graph.addNode(GraphTestHelper.classNode("orphan", "OrphanClass"));
                graph.addEdge(GraphTestHelper.callsEdge("ep1", "svc"));
                // orphan has no edges - it's dead code

                var result = detector.execute(graph);
                assertTrue(result.contains("orphan"), "Orphan should be detected as dead code");
                assertFalse(result.contains("ep1"), "Endpoint should not be dead code");
                assertFalse(result.contains("svc"), "Reachable service should not be dead code");
            }
        }
    }

    @Nested
    class Given_FullyConnectedGraph {

        @Nested
        class When_DetectingDeadCode {

            @Test
            void Then_NoDeadCodeFound() {
                var graph = new KnowledgeGraph();
                graph.addNode(GraphTestHelper.endpointNode("ep1", "GET /api"));
                graph.addNode(GraphTestHelper.classNode("a", "ClassA"));
                graph.addNode(GraphTestHelper.classNode("b", "ClassB"));
                graph.addEdge(GraphTestHelper.callsEdge("ep1", "a"));
                graph.addEdge(GraphTestHelper.callsEdge("a", "b"));

                var result = detector.execute(graph);
                assertTrue(result.isEmpty(), "All nodes are reachable from entry points");
            }
        }
    }

    @Nested
    class Given_EmptyGraph {

        @Nested
        class When_DetectingDeadCode {

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
            void Then_ReturnsDeadCodeDetector() {
                assertEquals("dead-code-detector", detector.name());
            }
        }
    }
}
