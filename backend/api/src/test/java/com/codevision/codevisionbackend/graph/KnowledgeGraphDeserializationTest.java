package com.codevision.codevisionbackend.graph;

import static org.junit.jupiter.api.Assertions.*;

import com.codevision.codevisionbackend.graph.algorithm.DesignPatternRecognizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class KnowledgeGraphDeserializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Nested
    class Given_JsonWithStrategyPattern {

        @Nested
        class When_DeserializedAndAnalyzed {

            @Test
            void Then_SecondaryIndicesArePopulated() throws Exception {
                var json = """
                    {
                      "nodes": {
                        "iface": {"id":"iface","type":"INTERFACE","name":"PaymentStrategy","qualifiedName":"com.PaymentStrategy","origin":"SOURCE"},
                        "impl1": {"id":"impl1","type":"CLASS","name":"CreditCard","qualifiedName":"com.CreditCard","origin":"SOURCE"},
                        "impl2": {"id":"impl2","type":"CLASS","name":"PayPal","qualifiedName":"com.PayPal","origin":"SOURCE"}
                      },
                      "edges": [
                        {"id":"e1","type":"IMPLEMENTS","sourceNodeId":"impl1","targetNodeId":"iface"},
                        {"id":"e2","type":"IMPLEMENTS","sourceNodeId":"impl2","targetNodeId":"iface"}
                      ]
                    }
                    """;

                var graph = mapper.readValue(json, KnowledgeGraph.class);

                // Verify primary data
                assertEquals(3, graph.nodeCount());
                assertEquals(2, graph.edgeCount());

                // Verify secondary indices are populated
                assertFalse(graph.nodesOfType(KgNodeType.INTERFACE).isEmpty(),
                        "nodesByType should contain INTERFACE entries");
                assertFalse(graph.nodesOfType(KgNodeType.CLASS).isEmpty(),
                        "nodesByType should contain CLASS entries");
                assertFalse(graph.edgesOfType(KgEdgeType.IMPLEMENTS).isEmpty(),
                        "edgesByType should contain IMPLEMENTS entries");

                assertEquals(1, graph.nodesOfType(KgNodeType.INTERFACE).size());
                assertEquals(2, graph.nodesOfType(KgNodeType.CLASS).size());
                assertEquals(2, graph.edgesOfType(KgEdgeType.IMPLEMENTS).size());

                // Verify adjacency indices
                assertEquals(1, graph.getNeighbors("impl1").size());
                assertEquals(1, graph.getNeighbors("impl2").size());
                assertEquals(2, graph.getIncoming("iface").size());
            }

            @Test
            void Then_DesignPatternRecognizerDetectsStrategy() throws Exception {
                var json = """
                    {
                      "nodes": {
                        "iface": {"id":"iface","type":"INTERFACE","name":"PaymentStrategy","qualifiedName":"com.PaymentStrategy","origin":"SOURCE"},
                        "impl1": {"id":"impl1","type":"CLASS","name":"CreditCard","qualifiedName":"com.CreditCard","origin":"SOURCE"},
                        "impl2": {"id":"impl2","type":"CLASS","name":"PayPal","qualifiedName":"com.PayPal","origin":"SOURCE"}
                      },
                      "edges": [
                        {"id":"e1","type":"IMPLEMENTS","sourceNodeId":"impl1","targetNodeId":"iface"},
                        {"id":"e2","type":"IMPLEMENTS","sourceNodeId":"impl2","targetNodeId":"iface"}
                      ]
                    }
                    """;

                var graph = mapper.readValue(json, KnowledgeGraph.class);
                var recognizer = new DesignPatternRecognizer();
                var result = recognizer.execute(graph);

                assertTrue(result.containsKey("STRATEGY"),
                        "Should detect STRATEGY pattern, got: " + result);
                assertTrue(result.get("STRATEGY").contains("PaymentStrategy"));
            }
        }
    }

    @Nested
    class Given_JsonWithFactoryPattern {

        @Nested
        class When_DeserializedAndAnalyzed {

            @Test
            void Then_DesignPatternRecognizerDetectsFactory() throws Exception {
                var json = """
                    {
                      "nodes": {
                        "factory": {"id":"factory","type":"CLASS","name":"ConnectionFactory","qualifiedName":"com.ConnectionFactory","origin":"SOURCE"},
                        "product1": {"id":"product1","type":"CLASS","name":"MySqlConn","qualifiedName":"com.MySqlConn","origin":"SOURCE"},
                        "product2": {"id":"product2","type":"CLASS","name":"PgConn","qualifiedName":"com.PgConn","origin":"SOURCE"}
                      },
                      "edges": [
                        {"id":"e1","type":"CONSTRUCTS","sourceNodeId":"factory","targetNodeId":"product1"},
                        {"id":"e2","type":"CONSTRUCTS","sourceNodeId":"factory","targetNodeId":"product2"}
                      ]
                    }
                    """;

                var graph = mapper.readValue(json, KnowledgeGraph.class);
                var recognizer = new DesignPatternRecognizer();
                var result = recognizer.execute(graph);

                assertTrue(result.containsKey("FACTORY"),
                        "Should detect FACTORY pattern, got: " + result);
                assertTrue(result.get("FACTORY").contains("ConnectionFactory"));
            }
        }
    }

    @Nested
    class Given_JsonWithSingletonPattern {

        @Nested
        class When_DeserializedAndAnalyzed {

            @Test
            void Then_DesignPatternRecognizerDetectsSingleton() throws Exception {
                var json = """
                    {
                      "nodes": {
                        "cfg": {"id":"cfg","type":"CLASS","name":"Configuration","qualifiedName":"com.Configuration","origin":"SOURCE"}
                      },
                      "edges": [
                        {"id":"e1","type":"CONSTRUCTS","sourceNodeId":"cfg","targetNodeId":"cfg"}
                      ]
                    }
                    """;

                var graph = mapper.readValue(json, KnowledgeGraph.class);
                var recognizer = new DesignPatternRecognizer();
                var result = recognizer.execute(graph);

                assertTrue(result.containsKey("SINGLETON"),
                        "Should detect SINGLETON pattern, got: " + result);
                assertTrue(result.get("SINGLETON").contains("Configuration"));
            }
        }
    }

    @Nested
    class Given_RoundTrip {

        @Nested
        class When_SerializedAndDeserialized {

            @Test
            void Then_SecondaryIndicesArePreserved() throws Exception {
                var original = new KnowledgeGraph();
                original.addNode(new KgNode("n1", KgNodeType.CLASS, "MyClass", "com.MyClass",
                        null, null, "SOURCE", null));
                original.addEdge(new KgEdge("e1", KgEdgeType.CALLS, "n1", "n1",
                        null, null, null, null));

                var json = mapper.writeValueAsString(original);
                var deserialized = mapper.readValue(json, KnowledgeGraph.class);

                assertEquals(1, deserialized.nodeCount());
                assertEquals(1, deserialized.edgeCount());
                assertEquals(1, deserialized.nodesOfType(KgNodeType.CLASS).size());
                assertEquals(1, deserialized.edgesOfType(KgEdgeType.CALLS).size());
                assertEquals(1, deserialized.getNeighbors("n1").size());
            }
        }
    }
}
