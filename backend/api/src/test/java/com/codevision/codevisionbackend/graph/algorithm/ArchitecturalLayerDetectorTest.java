package com.codevision.codevisionbackend.graph.algorithm;

import static org.junit.jupiter.api.Assertions.*;

import com.codevision.codevisionbackend.graph.KgNodeType;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ArchitecturalLayerDetectorTest {

    private final ArchitecturalLayerDetector detector = new ArchitecturalLayerDetector();

    @Nested
    class Given_TypicalLayeredArchitecture {

        @Nested
        class When_DetectingLayers {

            @Test
            void Then_EndpointsArePresentation() {
                var graph = new KnowledgeGraph();
                graph.addNode(GraphTestHelper.endpointNode("ep1", "GET /api/users"));
                graph.addNode(GraphTestHelper.classNode("svc", "UserService"));
                graph.addNode(GraphTestHelper.node("repo", "UserRepository", KgNodeType.CLASS));
                graph.addNode(GraphTestHelper.node("entity", "UserEntity", KgNodeType.DATABASE_ENTITY));
                graph.addEdge(GraphTestHelper.callsEdge("ep1", "svc"));
                graph.addEdge(GraphTestHelper.callsEdge("svc", "repo"));
                graph.addEdge(GraphTestHelper.edge("repo", "entity", com.codevision.codevisionbackend.graph.KgEdgeType.QUERIES));

                var result = detector.execute(graph);
                assertEquals("PRESENTATION", result.get("ep1"));
                assertEquals("DATA", result.get("entity"));
            }

            @Test
            void Then_IntermediateServiceIsBusiness() {
                var graph = new KnowledgeGraph();
                graph.addNode(GraphTestHelper.endpointNode("ep1", "GET /api/users"));
                graph.addNode(GraphTestHelper.classNode("svc", "UserService"));
                graph.addNode(GraphTestHelper.node("entity", "UserEntity", KgNodeType.DATABASE_ENTITY));
                graph.addEdge(GraphTestHelper.callsEdge("ep1", "svc"));
                graph.addEdge(GraphTestHelper.edge("svc", "entity", com.codevision.codevisionbackend.graph.KgEdgeType.QUERIES));

                var result = detector.execute(graph);
                assertEquals("BUSINESS", result.get("svc"),
                        "Service between endpoint and data should be BUSINESS layer");
            }

            @Test
            void Then_IsolatedNodeIsDomain() {
                var graph = new KnowledgeGraph();
                graph.addNode(GraphTestHelper.classNode("util", "StringUtils"));

                var result = detector.execute(graph);
                assertEquals("DOMAIN", result.get("util"),
                        "Isolated class should default to DOMAIN layer");
            }
        }
    }

    @Nested
    class Given_EmptyGraph {

        @Nested
        class When_DetectingLayers {

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
            void Then_ReturnsArchitecturalLayerDetector() {
                assertEquals("architectural-layer-detector", detector.name());
            }
        }
    }
}
