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
            void Then_DetectsClassConstructingMultipleTypes() {
                var graph = new KnowledgeGraph();
                graph.addNode(GraphTestHelper.classNode("factory", "ConnectionFactory"));
                graph.addNode(GraphTestHelper.classNode("product1", "MySqlConnection"));
                graph.addNode(GraphTestHelper.classNode("product2", "PostgresConnection"));
                graph.addEdge(GraphTestHelper.edge("factory", "product1", KgEdgeType.CONSTRUCTS));
                graph.addEdge(GraphTestHelper.edge("factory", "product2", KgEdgeType.CONSTRUCTS));

                var result = recognizer.execute(graph);
                assertTrue(result.containsKey("FACTORY"));
                assertTrue(result.get("FACTORY").contains("ConnectionFactory"));
            }

            @Test
            void Then_DetectsFactoryWithoutFactoryInName() {
                var graph = new KnowledgeGraph();
                graph.addNode(GraphTestHelper.classNode("creator", "VehicleCreator"));
                graph.addNode(GraphTestHelper.classNode("car", "Car"));
                graph.addNode(GraphTestHelper.classNode("truck", "Truck"));
                graph.addEdge(GraphTestHelper.edge("creator", "car", KgEdgeType.CONSTRUCTS));
                graph.addEdge(GraphTestHelper.edge("creator", "truck", KgEdgeType.INSTANTIATES));

                var result = recognizer.execute(graph);
                assertTrue(result.containsKey("FACTORY"));
                assertTrue(result.get("FACTORY").contains("VehicleCreator"));
            }

            @Test
            void Then_DoesNotDetectSingleConstructAsFactory() {
                var graph = new KnowledgeGraph();
                graph.addNode(GraphTestHelper.classNode("svc", "MyService"));
                graph.addNode(GraphTestHelper.classNode("dep", "Dependency"));
                graph.addEdge(GraphTestHelper.edge("svc", "dep", KgEdgeType.CONSTRUCTS));

                var result = recognizer.execute(graph);
                assertFalse(result.containsKey("FACTORY"));
            }
        }
    }

    @Nested
    class Given_SingletonPattern {

        @Nested
        class When_Recognizing {

            @Test
            void Then_DetectsClassWithSelfConstructsEdge() {
                var graph = new KnowledgeGraph();
                graph.addNode(GraphTestHelper.classNode("cfg", "Configuration"));
                graph.addEdge(GraphTestHelper.edge("cfg", "cfg", KgEdgeType.CONSTRUCTS));

                var result = recognizer.execute(graph);
                assertTrue(result.containsKey("SINGLETON"));
                assertTrue(result.get("SINGLETON").contains("Configuration"));
            }

            @Test
            void Then_DetectsClassWithSelfFieldReference() {
                var graph = new KnowledgeGraph();
                graph.addNode(GraphTestHelper.classNode("reg", "Registry"));
                graph.addEdge(GraphTestHelper.edge("reg", "reg", KgEdgeType.READS_FIELD));

                var result = recognizer.execute(graph);
                assertTrue(result.containsKey("SINGLETON"));
                assertTrue(result.get("SINGLETON").contains("Registry"));
            }
        }
    }

    @Nested
    class Given_ObserverPattern {

        @Nested
        class When_Recognizing {

            @Test
            void Then_DetectsClassWithPublishesEdge() {
                var graph = new KnowledgeGraph();
                graph.addNode(GraphTestHelper.classNode("bus", "EventBus"));
                graph.addNode(GraphTestHelper.classNode("evt", "UserCreatedEvent"));
                graph.addEdge(GraphTestHelper.edge("bus", "evt", KgEdgeType.PUBLISHES));

                var result = recognizer.execute(graph);
                assertTrue(result.containsKey("OBSERVER"));
                assertTrue(result.get("OBSERVER").contains("EventBus"));
            }

            @Test
            void Then_DetectsClassWithListenerManagementMethods() {
                var graph = new KnowledgeGraph();
                graph.addNode(GraphTestHelper.classNode("subject", "EventEmitter"));
                graph.addNode(GraphTestHelper.methodNode("m1", "addListener"));
                graph.addNode(GraphTestHelper.methodNode("m2", "removeListener"));
                graph.addNode(GraphTestHelper.methodNode("m3", "notifyAll"));
                graph.addEdge(GraphTestHelper.containsEdge("subject", "m1"));
                graph.addEdge(GraphTestHelper.containsEdge("subject", "m2"));
                graph.addEdge(GraphTestHelper.containsEdge("subject", "m3"));

                var result = recognizer.execute(graph);
                assertTrue(result.containsKey("OBSERVER"));
                assertTrue(result.get("OBSERVER").contains("EventEmitter"));
            }

            @Test
            void Then_DoesNotDetectClassWithFewerThanThreeListenerMethods() {
                var graph = new KnowledgeGraph();
                graph.addNode(GraphTestHelper.classNode("svc", "MyService"));
                graph.addNode(GraphTestHelper.methodNode("m1", "addItem"));
                graph.addNode(GraphTestHelper.methodNode("m2", "removeItem"));
                graph.addEdge(GraphTestHelper.containsEdge("svc", "m1"));
                graph.addEdge(GraphTestHelper.containsEdge("svc", "m2"));

                var result = recognizer.execute(graph);
                assertFalse(result.containsKey("OBSERVER"));
            }
        }
    }

    @Nested
    class Given_DecoratorPattern {

        @Nested
        class When_Recognizing {

            @Test
            void Then_DetectsClassExtendingAndUsingParent() {
                var graph = new KnowledgeGraph();
                graph.addNode(GraphTestHelper.classNode("base", "Component"));
                graph.addNode(GraphTestHelper.classNode("decorator", "LoggingDecorator"));
                graph.addEdge(GraphTestHelper.extendsEdge("decorator", "base"));
                graph.addEdge(GraphTestHelper.edge("decorator", "base", KgEdgeType.USES));

                var result = recognizer.execute(graph);
                assertTrue(result.containsKey("DECORATOR"));
                assertTrue(result.get("DECORATOR").contains("LoggingDecorator"));
            }

            @Test
            void Then_DoesNotDetectClassThatOnlyExtends() {
                var graph = new KnowledgeGraph();
                graph.addNode(GraphTestHelper.classNode("base", "Animal"));
                graph.addNode(GraphTestHelper.classNode("child", "Dog"));
                graph.addEdge(GraphTestHelper.extendsEdge("child", "base"));

                var result = recognizer.execute(graph);
                assertFalse(result.containsKey("DECORATOR"));
            }
        }
    }

    @Nested
    class Given_StrategyPatternNegativeCase {

        @Nested
        class When_Recognizing {

            @Test
            void Then_DoesNotDetectSingleImplementorAsStrategy() {
                var graph = new KnowledgeGraph();
                graph.addNode(GraphTestHelper.interfaceNode("iface", "Logger"));
                graph.addNode(GraphTestHelper.classNode("impl", "ConsoleLogger"));
                graph.addEdge(GraphTestHelper.implementsEdge("impl", "iface"));

                var result = recognizer.execute(graph);
                assertFalse(result.containsKey("STRATEGY"));
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
