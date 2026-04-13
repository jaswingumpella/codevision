package com.codevision.codevisionbackend.graph.algorithm;

import static org.junit.jupiter.api.Assertions.*;

import com.codevision.codevisionbackend.graph.KgEdgeType;
import com.codevision.codevisionbackend.graph.KgNodeType;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DesignPatternRecognizerTest {

    private final DesignPatternRecognizer recognizer = new DesignPatternRecognizer();

    @Nested
    class Given_StrategyPattern {

        @Nested
        class When_Recognizing {

            @Test
            void Then_DetectsInterfaceWithMultipleImplementors() {
                var graph = new KnowledgeGraph();
                graph.addNode(GraphTestHelper.interfaceNode("iface", "PaymentStrategy"));
                graph.addNode(GraphTestHelper.classNode("impl1", "CreditCardPayment"));
                graph.addNode(GraphTestHelper.classNode("impl2", "PayPalPayment"));
                graph.addEdge(GraphTestHelper.implementsEdge("impl1", "iface"));
                graph.addEdge(GraphTestHelper.implementsEdge("impl2", "iface"));

                var result = recognizer.execute(graph);
                assertTrue(result.containsKey("STRATEGY"));
                assertTrue(result.get("STRATEGY").contains("PaymentStrategy"));
            }
        }
    }

    @Nested
    class Given_FactoryPattern {

        @Nested
        class When_Recognizing {

            @Test
            void Then_DetectsFactoryClass() {
                var graph = new KnowledgeGraph();
                graph.addNode(GraphTestHelper.classNode("factory", "ConnectionFactory"));
                graph.addNode(GraphTestHelper.classNode("product", "Connection"));
                graph.addEdge(GraphTestHelper.edge("factory", "product", KgEdgeType.CONSTRUCTS));

                var result = recognizer.execute(graph);
                assertTrue(result.containsKey("FACTORY"));
                assertTrue(result.get("FACTORY").contains("ConnectionFactory"));
            }
        }
    }

    @Nested
    class Given_NoPatterns {

        @Nested
        class When_Recognizing {

            @Test
            void Then_ReturnsEmptyMap() {
                var graph = GraphTestHelper.linearChain(3);
                var result = recognizer.execute(graph);
                // Linear chain has no recognizable patterns
                assertTrue(result.isEmpty(), "Linear chain should have no recognized patterns");
            }
        }
    }

    @Nested
    class Given_EmptyGraph {

        @Nested
        class When_Recognizing {

            @Test
            void Then_ReturnsEmpty() {
                var graph = new KnowledgeGraph();
                var result = recognizer.execute(graph);
                assertTrue(result.isEmpty());
            }
        }
    }

    @Nested
    class Given_AlgorithmMetadata {

        @Nested
        class When_QueryingName {

            @Test
            void Then_ReturnsDesignPatternRecognizer() {
                assertEquals("design-pattern-recognizer", recognizer.name());
            }
        }
    }
}
