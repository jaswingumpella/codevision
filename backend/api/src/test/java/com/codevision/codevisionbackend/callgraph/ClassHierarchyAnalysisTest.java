package com.codevision.codevisionbackend.callgraph;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.codevision.codevisionbackend.callgraph.ClassHierarchyAnalysis.ClassHierarchy;
import com.codevision.codevisionbackend.config.AnalysisSafetyProperties;
import com.codevision.codevisionbackend.graph.ConfidenceLevel;
import com.codevision.codevisionbackend.graph.KgEdge;
import com.codevision.codevisionbackend.graph.KgEdgeType;
import com.codevision.codevisionbackend.graph.KgNode;
import com.codevision.codevisionbackend.graph.KgNodeType;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import com.codevision.codevisionbackend.graph.NodeMetadata;
import com.codevision.codevisionbackend.graph.Provenance;

@DisplayName("ClassHierarchyAnalysis")
class ClassHierarchyAnalysisTest {

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

    @Nested
    @DisplayName("Given a simple inheritance chain")
    class Given_SimpleInheritanceChain {

        private final KnowledgeGraph graph = buildGraph(
                new KgNode[]{
                        classNode("c1", "Animal"),
                        classNode("c2", "Dog"),
                        classNode("c3", "Puppy")
                },
                new KgEdge[]{
                        edge("e1", KgEdgeType.EXTENDS, "c2", "c1"),
                        edge("e2", KgEdgeType.EXTENDS, "c3", "c2")
                }
        );

        @Nested
        @DisplayName("When building hierarchy")
        class When_BuildingHierarchy {

            @Test
            @DisplayName("Then subtypes and supertypes are correct")
            void Then_SubtypesAndSupertypesAreCorrect() {
                var cha = new ClassHierarchyAnalysis(new AnalysisSafetyProperties());
                var hierarchy = cha.buildHierarchy(graph);

                assertThat(hierarchy.subtypeMap().get("c1")).containsExactly("c2");
                assertThat(hierarchy.subtypeMap().get("c2")).containsExactly("c3");
                assertThat(hierarchy.supertypeMap().get("c2")).containsExactly("c1");
                assertThat(hierarchy.supertypeMap().get("c3")).containsExactly("c2");
            }

            @Test
            @DisplayName("Then transitive subtypes include all descendants")
            void Then_TransitiveSubtypesIncludeAllDescendants() {
                var cha = new ClassHierarchyAnalysis(new AnalysisSafetyProperties());
                var hierarchy = cha.buildHierarchy(graph);

                var subtypes = cha.getSubtypes("c1", hierarchy);
                assertThat(subtypes).containsExactlyInAnyOrder("c2", "c3");
            }

            @Test
            @DisplayName("Then transitive supertypes include all ancestors")
            void Then_TransitiveSupertypesIncludeAllAncestors() {
                var cha = new ClassHierarchyAnalysis(new AnalysisSafetyProperties());
                var hierarchy = cha.buildHierarchy(graph);

                var supertypes = cha.getSupertypes("c3", hierarchy);
                assertThat(supertypes).containsExactlyInAnyOrder("c2", "c1");
            }
        }
    }

    @Nested
    @DisplayName("Given diamond inheritance")
    class Given_DiamondInheritance {

        private final KnowledgeGraph graph = buildGraph(
                new KgNode[]{
                        classNode("a", "A"),
                        classNode("b", "B"),
                        classNode("c", "C"),
                        classNode("d", "D")
                },
                new KgEdge[]{
                        edge("e1", KgEdgeType.EXTENDS, "b", "a"),
                        edge("e2", KgEdgeType.EXTENDS, "c", "a"),
                        edge("e3", KgEdgeType.EXTENDS, "d", "b"),
                        edge("e4", KgEdgeType.EXTENDS, "d", "c")
                }
        );

        @Nested
        @DisplayName("When getting subtypes")
        class When_GettingSubtypes {

            @Test
            @DisplayName("Then all paths resolved")
            void Then_AllPathsResolved() {
                var cha = new ClassHierarchyAnalysis(new AnalysisSafetyProperties());
                var hierarchy = cha.buildHierarchy(graph);

                var subtypes = cha.getSubtypes("a", hierarchy);
                assertThat(subtypes).containsExactlyInAnyOrder("b", "c", "d");
            }
        }
    }

    @Nested
    @DisplayName("Given cyclic inheritance")
    class Given_CyclicInheritance {

        private final KnowledgeGraph graph = buildGraph(
                new KgNode[]{
                        classNode("x", "X"),
                        classNode("y", "Y"),
                        classNode("z", "Z")
                },
                new KgEdge[]{
                        edge("e1", KgEdgeType.EXTENDS, "x", "y"),
                        edge("e2", KgEdgeType.EXTENDS, "y", "z"),
                        edge("e3", KgEdgeType.EXTENDS, "z", "x")
                }
        );

        @Nested
        @DisplayName("When building hierarchy")
        class When_BuildingHierarchy {

            @Test
            @DisplayName("Then cycle detected, no infinite loop")
            void Then_CycleDetectedNoInfiniteLoop() {
                var cha = new ClassHierarchyAnalysis(new AnalysisSafetyProperties());
                var hierarchy = cha.buildHierarchy(graph);

                var subtypes = cha.getSubtypes("x", hierarchy);
                assertThat(subtypes).isNotNull();
                assertThat(subtypes).containsExactlyInAnyOrder("z", "y");
            }
        }
    }

    @Nested
    @DisplayName("Given an empty graph")
    class Given_EmptyGraph {

        private final KnowledgeGraph graph = new KnowledgeGraph();

        @Nested
        @DisplayName("When building hierarchy")
        class When_BuildingHierarchy {

            @Test
            @DisplayName("Then empty maps")
            void Then_EmptyMaps() {
                var cha = new ClassHierarchyAnalysis(new AnalysisSafetyProperties());
                var hierarchy = cha.buildHierarchy(graph);

                assertThat(hierarchy.subtypeMap()).isEmpty();
                assertThat(hierarchy.supertypeMap()).isEmpty();
                assertThat(cha.getSubtypes("nonexistent", hierarchy)).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("Given interface implementation")
    class Given_InterfaceImplementation {

        private final KnowledgeGraph graph = buildGraph(
                new KgNode[]{
                        interfaceNode("iface", "Runnable"),
                        classNode("impl1", "TaskA"),
                        classNode("impl2", "TaskB")
                },
                new KgEdge[]{
                        edge("e1", KgEdgeType.IMPLEMENTS, "impl1", "iface"),
                        edge("e2", KgEdgeType.IMPLEMENTS, "impl2", "iface")
                }
        );

        @Nested
        @DisplayName("When resolving dispatch targets")
        class When_ResolvingDispatch {

            @Test
            @DisplayName("Then all implementors returned as dispatch targets")
            void Then_AllImplementorsReturnedAsDispatchTargets() {
                var cha = new ClassHierarchyAnalysis(new AnalysisSafetyProperties());
                var hierarchy = cha.buildHierarchy(graph);

                var targets = cha.resolveDispatchTargets("iface", "run", hierarchy);
                assertThat(targets).containsExactlyInAnyOrder("iface", "impl1", "impl2");
            }
        }
    }

    @Nested
    @DisplayName("Given deep hierarchy")
    class Given_DeepHierarchy {

        private final KnowledgeGraph graph;

        Given_DeepHierarchy() {
            var g = new KnowledgeGraph();
            g.addNode(classNode("root", "Root"));
            for (int i = 1; i <= 5; i++) {
                g.addNode(classNode("l" + i, "Level" + i));
                var parent = i == 1 ? "root" : "l" + (i - 1);
                g.addEdge(edge("e" + i, KgEdgeType.EXTENDS, "l" + i, parent));
            }
            this.graph = g;
        }

        @Nested
        @DisplayName("When getting transitive subtypes")
        class When_GettingTransitiveSubtypes {

            @Test
            @DisplayName("Then all levels traversed")
            void Then_AllLevelsTraversed() {
                var cha = new ClassHierarchyAnalysis(new AnalysisSafetyProperties());
                var hierarchy = cha.buildHierarchy(graph);

                var subtypes = cha.getSubtypes("root", hierarchy);
                assertThat(subtypes).containsExactlyInAnyOrder("l1", "l2", "l3", "l4", "l5");
            }
        }
    }
}
