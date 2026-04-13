package com.codevision.codevisionbackend.callgraph;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.codevision.codevisionbackend.config.AnalysisSafetyProperties;
import com.codevision.codevisionbackend.graph.ConfidenceLevel;
import com.codevision.codevisionbackend.graph.KgEdge;
import com.codevision.codevisionbackend.graph.KgEdgeType;
import com.codevision.codevisionbackend.graph.KgNode;
import com.codevision.codevisionbackend.graph.KgNodeType;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import com.codevision.codevisionbackend.graph.NodeMetadata;
import com.codevision.codevisionbackend.graph.Provenance;

@DisplayName("DispatchResolver")
class DispatchResolverTest {

    private static KgNode classNode(String id, String name) {
        return new KgNode(id, KgNodeType.CLASS, name, "com.test." + name,
                new NodeMetadata("public", Set.of(), emptyList(), emptyList(), "void",
                        emptyList(), emptyList(), null, 0, 0, 0, null, null, 0, 0, null),
                null, "SOURCE",
                new Provenance("test", "Test.java", 1, ConfidenceLevel.EXTRACTED));
    }

    private static KgNode interfaceNode(String id, String name) {
        return new KgNode(id, KgNodeType.INTERFACE, name, "com.test." + name,
                new NodeMetadata("public", Set.of(), emptyList(), emptyList(), "void",
                        emptyList(), emptyList(), null, 0, 0, 0, null, null, 0, 0, null),
                null, "SOURCE",
                new Provenance("test", "Test.java", 1, ConfidenceLevel.EXTRACTED));
    }

    private static KgNode methodNode(String id, String name) {
        return new KgNode(id, KgNodeType.METHOD, name, "com.test." + name,
                new NodeMetadata("public", Set.of(), emptyList(), emptyList(), "void",
                        emptyList(), emptyList(), null, 0, 0, 0, null, null, 0, 0, null),
                null, "SOURCE",
                new Provenance("test", "Test.java", 1, ConfidenceLevel.EXTRACTED));
    }

    private static KgEdge edge(String id, KgEdgeType type, String source, String target) {
        return new KgEdge(id, type, source, target, type.name(), ConfidenceLevel.EXTRACTED,
                new Provenance("test", "Test.java", 1, ConfidenceLevel.EXTRACTED), Map.of());
    }

    private KnowledgeGraph buildGraph(KgNode[] nodes, KgEdge[] edges) {
        var graph = new KnowledgeGraph();
        for (var node : nodes) graph.addNode(node);
        for (var e : edges) graph.addEdge(e);
        return graph;
    }

    private DispatchResolver createResolver() {
        var cha = new ClassHierarchyAnalysis(new AnalysisSafetyProperties());
        return new DispatchResolver(cha);
    }

    @Nested
    @DisplayName("Given class with override")
    class Given_ClassWithOverride {

        private final KnowledgeGraph graph = buildGraph(
                new KgNode[]{
                        classNode("parent", "Parent"),
                        classNode("child", "Child"),
                        methodNode("parent.doWork", "doWork"),
                        methodNode("child.doWork", "doWork")
                },
                new KgEdge[]{
                        edge("e1", KgEdgeType.EXTENDS, "child", "parent"),
                        edge("e2", KgEdgeType.DECLARES, "parent", "parent.doWork"),
                        edge("e3", KgEdgeType.DECLARES, "child", "child.doWork"),
                        edge("e4", KgEdgeType.OVERRIDES, "child.doWork", "parent.doWork")
                }
        );

        @Nested
        @DisplayName("When resolving targets")
        class When_ResolvingTargets {

            @Test
            @DisplayName("Then both parent and child returned")
            void Then_BothParentAndChildReturned() {
                var resolver = createResolver();

                var targets = resolver.resolveTargets("parent", "doWork", graph);
                assertThat(targets).containsExactlyInAnyOrder("parent", "child");
            }
        }
    }

    @Nested
    @DisplayName("Given interface with multiple implementations")
    class Given_InterfaceWithMultipleImpls {

        private final KnowledgeGraph graph = buildGraph(
                new KgNode[]{
                        interfaceNode("iface", "Service"),
                        classNode("implA", "ServiceImplA"),
                        classNode("implB", "ServiceImplB"),
                        methodNode("iface.execute", "execute"),
                        methodNode("implA.execute", "execute"),
                        methodNode("implB.execute", "execute")
                },
                new KgEdge[]{
                        edge("e1", KgEdgeType.IMPLEMENTS, "implA", "iface"),
                        edge("e2", KgEdgeType.IMPLEMENTS, "implB", "iface"),
                        edge("e3", KgEdgeType.DECLARES, "iface", "iface.execute"),
                        edge("e4", KgEdgeType.DECLARES, "implA", "implA.execute"),
                        edge("e5", KgEdgeType.DECLARES, "implB", "implB.execute")
                }
        );

        @Nested
        @DisplayName("When resolving targets")
        class When_ResolvingTargets {

            @Test
            @DisplayName("Then all implementations returned")
            void Then_AllImplementationsReturned() {
                var resolver = createResolver();

                var targets = resolver.resolveTargets("iface", "execute", graph);
                assertThat(targets).containsExactlyInAnyOrder("iface", "implA", "implB");
            }
        }
    }

    @Nested
    @DisplayName("Given no overrides")
    class Given_NoOverrides {

        private final KnowledgeGraph graph = buildGraph(
                new KgNode[]{
                        classNode("cls", "MyClass"),
                        methodNode("cls.uniqueMethod", "uniqueMethod")
                },
                new KgEdge[]{
                        edge("e1", KgEdgeType.DECLARES, "cls", "cls.uniqueMethod")
                }
        );

        @Nested
        @DisplayName("When resolving targets")
        class When_ResolvingTargets {

            @Test
            @DisplayName("Then only original target")
            void Then_OnlyOriginalTarget() {
                var resolver = createResolver();

                var targets = resolver.resolveTargets("cls", "uniqueMethod", graph);
                assertThat(targets).containsExactly("cls");
            }
        }
    }

    @Nested
    @DisplayName("Given method override")
    class Given_MethodOverride {

        private final KnowledgeGraph graph = buildGraph(
                new KgNode[]{
                        classNode("base", "Base"),
                        classNode("derived", "Derived"),
                        methodNode("base.run", "run"),
                        methodNode("derived.run", "run")
                },
                new KgEdge[]{
                        edge("e1", KgEdgeType.EXTENDS, "derived", "base"),
                        edge("e2", KgEdgeType.DECLARES, "base", "base.run"),
                        edge("e3", KgEdgeType.DECLARES, "derived", "derived.run"),
                        edge("e4", KgEdgeType.OVERRIDES, "derived.run", "base.run")
                }
        );

        @Nested
        @DisplayName("When checking isOverride")
        class When_CheckingIsOverride {

            @Test
            @DisplayName("Then returns true")
            void Then_ReturnsTrue() {
                var resolver = createResolver();

                assertThat(resolver.isOverride("derived.run", "base.run", graph)).isTrue();
            }
        }
    }

    @Nested
    @DisplayName("Given unrelated method")
    class Given_UnrelatedMethod {

        private final KnowledgeGraph graph = buildGraph(
                new KgNode[]{
                        classNode("standalone", "Standalone"),
                        methodNode("standalone.process", "process")
                },
                new KgEdge[]{
                        edge("e1", KgEdgeType.DECLARES, "standalone", "standalone.process")
                }
        );

        @Nested
        @DisplayName("When checking isOverride")
        class When_CheckingIsOverride {

            @Test
            @DisplayName("Then returns false")
            void Then_ReturnsFalse() {
                var resolver = createResolver();

                assertThat(resolver.isOverride("standalone.process", "nonexistent", graph)).isFalse();
            }
        }
    }
}
