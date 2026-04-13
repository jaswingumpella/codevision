package com.codevision.codevisionbackend.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codevision.codevisionbackend.analysis.GraphModel;
import com.codevision.codevisionbackend.analysis.GraphModel.ClassNode;
import com.codevision.codevisionbackend.analysis.GraphModel.DependencyEdge;
import com.codevision.codevisionbackend.analysis.GraphModel.DependencyKind;
import com.codevision.codevisionbackend.analysis.GraphModel.EndpointNode;
import com.codevision.codevisionbackend.analysis.GraphModel.EndpointType;
import com.codevision.codevisionbackend.analysis.GraphModel.MethodCallEdge;
import com.codevision.codevisionbackend.analysis.GraphModel.NodeKind;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GraphModelAdapter")
class GraphModelAdapterTest {

    // ── Fixture helpers ─────────────────────────────────────────────────

    private static ClassNode classNode(String fqn, String simpleName, NodeKind kind) {
        ClassNode cn = new ClassNode();
        cn.setName(fqn);
        cn.setSimpleName(simpleName);
        cn.setKind(kind);
        cn.setOrigin(GraphModel.Origin.SOURCE);
        return cn;
    }

    private static EndpointNode endpoint(String controller, String method, String httpMethod, String path) {
        EndpointNode ep = new EndpointNode();
        ep.setControllerClass(controller);
        ep.setControllerMethod(method);
        ep.setHttpMethod(httpMethod);
        ep.setPath(path);
        ep.setType(EndpointType.HTTP);
        ep.setFramework("spring-mvc");
        return ep;
    }

    private static GraphModel modelWithClasses(ClassNode... nodes) {
        GraphModel model = new GraphModel();
        for (ClassNode cn : nodes) {
            model.addClass(cn);
        }
        return model;
    }

    // ── Forward conversion tests ────────────────────────────────────────

    @Nested
    @DisplayName("Given a legacy GraphModel with classes")
    class Given_LegacyGraphModelWithClasses {

        private final GraphModel model = modelWithClasses(
                classNode("com.example.Foo", "Foo", NodeKind.CLASS),
                classNode("com.example.Bar", "Bar", NodeKind.CLASS)
        );

        @Nested
        @DisplayName("When converting to KnowledgeGraph")
        class When_ConvertingToKnowledgeGraph {

            private final KnowledgeGraph kg = GraphModelAdapter.toKnowledgeGraph(model);

            @Test
            @DisplayName("Then all class nodes are preserved")
            void Then_AllClassNodesPreserved() {
                assertNotNull(kg.getNode("com.example.Foo"));
                assertNotNull(kg.getNode("com.example.Bar"));
                assertEquals(2, kg.nodeCount());
            }
        }
    }

    @Nested
    @DisplayName("Given a legacy GraphModel with various node kinds")
    class Given_LegacyGraphModelWithNodeKinds {

        @Nested
        @DisplayName("When converting to KnowledgeGraph")
        class When_ConvertingToKnowledgeGraph {

            @Test
            @DisplayName("Then CLASS kind maps to CLASS type")
            void Then_ClassMapsCorrectly() {
                GraphModel model = modelWithClasses(classNode("A", "A", NodeKind.CLASS));
                KnowledgeGraph kg = GraphModelAdapter.toKnowledgeGraph(model);
                assertEquals(KgNodeType.CLASS, kg.getNode("A").type());
            }

            @Test
            @DisplayName("Then INTERFACE kind maps to INTERFACE type")
            void Then_InterfaceMapsCorrectly() {
                GraphModel model = modelWithClasses(classNode("B", "B", NodeKind.INTERFACE));
                KnowledgeGraph kg = GraphModelAdapter.toKnowledgeGraph(model);
                assertEquals(KgNodeType.INTERFACE, kg.getNode("B").type());
            }

            @Test
            @DisplayName("Then ENUM kind maps to ENUM type")
            void Then_EnumMapsCorrectly() {
                GraphModel model = modelWithClasses(classNode("C", "C", NodeKind.ENUM));
                KnowledgeGraph kg = GraphModelAdapter.toKnowledgeGraph(model);
                assertEquals(KgNodeType.ENUM, kg.getNode("C").type());
            }

            @Test
            @DisplayName("Then RECORD kind maps to RECORD type")
            void Then_RecordMapsCorrectly() {
                GraphModel model = modelWithClasses(classNode("D", "D", NodeKind.RECORD));
                KnowledgeGraph kg = GraphModelAdapter.toKnowledgeGraph(model);
                assertEquals(KgNodeType.RECORD, kg.getNode("D").type());
            }
        }
    }

    @Nested
    @DisplayName("Given a legacy GraphModel with endpoints")
    class Given_LegacyGraphModelWithEndpoints {

        @Nested
        @DisplayName("When converting to KnowledgeGraph")
        class When_ConvertingToKnowledgeGraph {

            @Test
            @DisplayName("Then endpoint nodes are created")
            void Then_EndpointNodesCreated() {
                GraphModel model = new GraphModel();
                model.addEndpoint(endpoint("com.example.Controller", "getUsers", "GET", "/api/users"));

                KnowledgeGraph kg = GraphModelAdapter.toKnowledgeGraph(model);

                Set<String> endpointIds = kg.nodesOfType(KgNodeType.ENDPOINT);
                assertEquals(1, endpointIds.size());

                String epId = endpointIds.iterator().next();
                KgNode epNode = kg.getNode(epId);
                assertNotNull(epNode);
                assertEquals(KgNodeType.ENDPOINT, epNode.type());
            }

            @Test
            @DisplayName("Then endpoint metadata contains HTTP details")
            void Then_EndpointMetadataContainsHttpDetails() {
                GraphModel model = new GraphModel();
                model.addEndpoint(endpoint("com.example.Controller", "getUsers", "GET", "/api/users"));

                KnowledgeGraph kg = GraphModelAdapter.toKnowledgeGraph(model);
                String epId = kg.nodesOfType(KgNodeType.ENDPOINT).iterator().next();
                KgNode epNode = kg.getNode(epId);

                Map<String, Object> ls = epNode.metadata().languageSpecific();
                assertEquals("GET", ls.get("httpMethod"));
                assertEquals("/api/users", ls.get("path"));
            }
        }
    }

    @Nested
    @DisplayName("Given a legacy GraphModel with dependency edges")
    class Given_LegacyGraphModelWithDependencyEdges {

        @Nested
        @DisplayName("When converting to KnowledgeGraph")
        class When_ConvertingToKnowledgeGraph {

            @Test
            @DisplayName("Then EXTENDS edges are preserved with correct type")
            void Then_ExtendsEdgesPreserved() {
                GraphModel model = modelWithClasses(
                        classNode("A", "A", NodeKind.CLASS),
                        classNode("B", "B", NodeKind.CLASS)
                );
                model.addDependency(new DependencyEdge(DependencyKind.EXTENDS, "A", "B", "extends"));

                KnowledgeGraph kg = GraphModelAdapter.toKnowledgeGraph(model);

                List<KgEdge> extendsEdges = kg.edgesOfType(KgEdgeType.EXTENDS);
                assertEquals(1, extendsEdges.size());
                assertEquals("A", extendsEdges.get(0).sourceNodeId());
                assertEquals("B", extendsEdges.get(0).targetNodeId());
            }

            @Test
            @DisplayName("Then IMPLEMENTS edges are preserved with correct type")
            void Then_ImplementsEdgesPreserved() {
                GraphModel model = modelWithClasses(
                        classNode("A", "A", NodeKind.CLASS),
                        classNode("I", "I", NodeKind.INTERFACE)
                );
                model.addDependency(new DependencyEdge(DependencyKind.IMPLEMENTS, "A", "I", "implements"));

                KnowledgeGraph kg = GraphModelAdapter.toKnowledgeGraph(model);

                List<KgEdge> implEdges = kg.edgesOfType(KgEdgeType.IMPLEMENTS);
                assertEquals(1, implEdges.size());
            }

            @Test
            @DisplayName("Then INJECTION edges map to INJECTS type")
            void Then_InjectionEdgesMapToInjects() {
                GraphModel model = modelWithClasses(
                        classNode("A", "A", NodeKind.CLASS),
                        classNode("B", "B", NodeKind.CLASS)
                );
                model.addDependency(new DependencyEdge(DependencyKind.INJECTION, "A", "B", "injects"));

                KnowledgeGraph kg = GraphModelAdapter.toKnowledgeGraph(model);

                List<KgEdge> injectEdges = kg.edgesOfType(KgEdgeType.INJECTS);
                assertEquals(1, injectEdges.size());
            }

            @Test
            @DisplayName("Then FIELD edges map to READS_FIELD type")
            void Then_FieldEdgesMapToReadsField() {
                GraphModel model = modelWithClasses(
                        classNode("A", "A", NodeKind.CLASS),
                        classNode("B", "B", NodeKind.CLASS)
                );
                model.addDependency(new DependencyEdge(DependencyKind.FIELD, "A", "B", "field"));

                KnowledgeGraph kg = GraphModelAdapter.toKnowledgeGraph(model);

                List<KgEdge> fieldEdges = kg.edgesOfType(KgEdgeType.READS_FIELD);
                assertEquals(1, fieldEdges.size());
            }
        }
    }

    @Nested
    @DisplayName("Given a legacy GraphModel with method call edges")
    class Given_LegacyGraphModelWithMethodCallEdges {

        @Nested
        @DisplayName("When converting to KnowledgeGraph")
        class When_ConvertingToKnowledgeGraph {

            @Test
            @DisplayName("Then call edges are created")
            void Then_CallEdgesCreated() {
                GraphModel model = modelWithClasses(
                        classNode("com.example.A", "A", NodeKind.CLASS),
                        classNode("com.example.B", "B", NodeKind.CLASS)
                );
                model.addMethodCallEdge(new MethodCallEdge(
                        "com.example.A", "doSomething", "()V",
                        "com.example.B", "process", "(Ljava/lang/String;)V"
                ));

                KnowledgeGraph kg = GraphModelAdapter.toKnowledgeGraph(model);

                List<KgEdge> callEdges = kg.edgesOfType(KgEdgeType.CALLS);
                assertEquals(1, callEdges.size());
                assertEquals("com.example.A", callEdges.get(0).sourceNodeId());
                assertEquals("com.example.B", callEdges.get(0).targetNodeId());
            }

            @Test
            @DisplayName("Then call edge properties contain method details")
            void Then_CallEdgePropertiesContainMethodDetails() {
                GraphModel model = new GraphModel();
                model.addMethodCallEdge(new MethodCallEdge(
                        "com.A", "foo", "()V",
                        "com.B", "bar", "(I)V"
                ));

                KnowledgeGraph kg = GraphModelAdapter.toKnowledgeGraph(model);
                KgEdge callEdge = kg.edgesOfType(KgEdgeType.CALLS).get(0);

                assertEquals("foo", callEdge.properties().get("callerMethod"));
                assertEquals("bar", callEdge.properties().get("calleeMethod"));
            }
        }
    }

    @Nested
    @DisplayName("Given a null legacy GraphModel")
    class Given_NullLegacyGraphModel {

        @Test
        @DisplayName("Then returns an empty KnowledgeGraph")
        void Then_ReturnsEmptyKnowledgeGraph() {
            KnowledgeGraph kg = GraphModelAdapter.toKnowledgeGraph(null);
            assertNotNull(kg);
            assertEquals(0, kg.nodeCount());
        }
    }

    // ── Reverse conversion tests ────────────────────────────────────────

    @Nested
    @DisplayName("Given a KnowledgeGraph")
    class Given_KnowledgeGraph {

        @Nested
        @DisplayName("When converting to legacy GraphModel")
        class When_ConvertingToLegacyGraphModel {

            @Test
            @DisplayName("Then class nodes are preserved")
            void Then_ClassNodesPreserved() {
                KnowledgeGraph kg = new KnowledgeGraph();
                kg.addNode(new KgNode(
                        "com.example.Foo",
                        KgNodeType.CLASS,
                        "Foo",
                        "com.example.Foo",
                        new NodeMetadata(
                                null, Set.of(), List.of(), List.of(),
                                null, List.of(), List.of(), null,
                                0, 0, 0, null, null, 0, 0, Map.of()
                        ),
                        null, "SOURCE", null
                ));

                GraphModel model = GraphModelAdapter.toGraphModel(kg);

                assertNotNull(model.getClasses().get("com.example.Foo"));
                assertEquals("Foo", model.getClasses().get("com.example.Foo").getSimpleName());
            }

            @Test
            @DisplayName("Then INTERFACE nodes reverse to INTERFACE kind")
            void Then_InterfaceNodesReverse() {
                KnowledgeGraph kg = new KnowledgeGraph();
                kg.addNode(new KgNode("I", KgNodeType.INTERFACE, "I", "I", null, null, "SOURCE", null));

                GraphModel model = GraphModelAdapter.toGraphModel(kg);
                assertEquals(NodeKind.INTERFACE, model.getClasses().get("I").getKind());
            }

            @Test
            @DisplayName("Then ENUM nodes reverse to ENUM kind")
            void Then_EnumNodesReverse() {
                KnowledgeGraph kg = new KnowledgeGraph();
                kg.addNode(new KgNode("E", KgNodeType.ENUM, "E", "E", null, null, "SOURCE", null));

                GraphModel model = GraphModelAdapter.toGraphModel(kg);
                assertEquals(NodeKind.ENUM, model.getClasses().get("E").getKind());
            }

            @Test
            @DisplayName("Then RECORD nodes reverse to RECORD kind")
            void Then_RecordNodesReverse() {
                KnowledgeGraph kg = new KnowledgeGraph();
                kg.addNode(new KgNode("R", KgNodeType.RECORD, "R", "R", null, null, "SOURCE", null));

                GraphModel model = GraphModelAdapter.toGraphModel(kg);
                assertEquals(NodeKind.RECORD, model.getClasses().get("R").getKind());
            }
        }
    }

    @Nested
    @DisplayName("Given a KnowledgeGraph with endpoint nodes")
    class Given_KnowledgeGraphWithEndpoints {

        @Nested
        @DisplayName("When converting to legacy GraphModel")
        class When_ConvertingToLegacyGraphModel {

            @Test
            @DisplayName("Then endpoint nodes are converted back")
            void Then_EndpointNodesConverted() {
                KnowledgeGraph kg = new KnowledgeGraph();
                kg.addNode(new KgNode(
                        "ep1",
                        KgNodeType.ENDPOINT,
                        "GET /api/users",
                        "ep1",
                        new NodeMetadata(
                                null, Set.of(), List.of(), List.of(),
                                null, List.of(), List.of(), null,
                                0, 0, 0, null, null, 0, 0,
                                Map.of("httpMethod", "GET", "path", "/api/users",
                                        "controllerClass", "com.example.Ctrl",
                                        "controllerMethod", "getUsers",
                                        "produces", "", "consumes", "",
                                        "framework", "spring-mvc", "endpointType", "HTTP")
                        ),
                        null, "SOURCE", null
                ));

                GraphModel model = GraphModelAdapter.toGraphModel(kg);

                assertEquals(1, model.getEndpoints().size());
                assertEquals("GET", model.getEndpoints().get(0).getHttpMethod());
                assertEquals("/api/users", model.getEndpoints().get(0).getPath());
            }
        }
    }

    @Nested
    @DisplayName("Given a KnowledgeGraph with CALLS edges")
    class Given_KnowledgeGraphWithCallsEdges {

        @Nested
        @DisplayName("When converting to legacy GraphModel")
        class When_ConvertingToLegacyGraphModel {

            @Test
            @DisplayName("Then method call edges are created")
            void Then_MethodCallEdgesCreated() {
                KnowledgeGraph kg = new KnowledgeGraph();
                kg.addNode(new KgNode("A", KgNodeType.CLASS, "A", "A", null, null, "SOURCE", null));
                kg.addNode(new KgNode("B", KgNodeType.CLASS, "B", "B", null, null, "SOURCE", null));
                kg.addEdge(new KgEdge(
                        "e1", KgEdgeType.CALLS, "A", "B", "foo -> bar",
                        ConfidenceLevel.EXTRACTED, null,
                        Map.of("callerMethod", "foo", "calleeMethod", "bar",
                                "callerDescriptor", "()V", "calleeDescriptor", "(I)V")
                ));

                GraphModel model = GraphModelAdapter.toGraphModel(kg);

                assertEquals(1, model.getMethodCallEdges().size());
                MethodCallEdge mce = model.getMethodCallEdges().get(0);
                assertEquals("A", mce.getCallerClass());
                assertEquals("B", mce.getCalleeClass());
                assertEquals("foo", mce.getCallerMethod());
                assertEquals("bar", mce.getCalleeMethod());
            }
        }
    }

    @Nested
    @DisplayName("Given a null KnowledgeGraph")
    class Given_NullKnowledgeGraph {

        @Test
        @DisplayName("Then returns an empty GraphModel")
        void Then_ReturnsEmptyGraphModel() {
            GraphModel model = GraphModelAdapter.toGraphModel(null);
            assertNotNull(model);
            assertTrue(model.getClasses().isEmpty());
        }
    }

    @Nested
    @DisplayName("Given a legacy GraphModel with entity class")
    class Given_LegacyGraphModelWithEntityClass {

        @Test
        @DisplayName("Then entity modifier is preserved in round trip")
        void Then_EntityModifierPreserved() {
            ClassNode cn = classNode("com.example.User", "User", NodeKind.CLASS);
            cn.setEntity(true);
            cn.setTableName("users");

            GraphModel original = modelWithClasses(cn);
            KnowledgeGraph kg = GraphModelAdapter.toKnowledgeGraph(original);

            assertTrue(kg.getNode("com.example.User").metadata().modifiers().contains("entity"));
        }
    }

    @Nested
    @DisplayName("Given a legacy GraphModel with annotated class")
    class Given_LegacyGraphModelWithAnnotatedClass {

        @Test
        @DisplayName("Then annotations are carried over")
        void Then_AnnotationsCarriedOver() {
            ClassNode cn = classNode("com.example.Svc", "Svc", NodeKind.CLASS);
            cn.setAnnotations(List.of("org.springframework.stereotype.Service"));

            GraphModel model = modelWithClasses(cn);
            KnowledgeGraph kg = GraphModelAdapter.toKnowledgeGraph(model);

            assertFalse(kg.getNode("com.example.Svc").metadata().annotations().isEmpty());
            assertEquals("org.springframework.stereotype.Service",
                    kg.getNode("com.example.Svc").metadata().annotations().get(0).name());
        }
    }
}
