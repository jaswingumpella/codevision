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

@DisplayName("UnlimitedCallGraphBuilder")
class UnlimitedCallGraphBuilderTest {

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

    private UnlimitedCallGraphBuilder createBuilder() {
        return new UnlimitedCallGraphBuilder(new AnalysisSafetyProperties());
    }

    @Nested
    @DisplayName("Given a simple call chain")
    class Given_SimpleCallChain {

        private final KnowledgeGraph graph = buildGraph(
                new KgNode[]{
                        methodNode("m1", "main"),
                        methodNode("m2", "service"),
                        methodNode("m3", "repository")
                },
                new KgEdge[]{
                        edge("e1", KgEdgeType.CALLS, "m1", "m2"),
                        edge("e2", KgEdgeType.CALLS, "m2", "m3")
                }
        );

        @Nested
        @DisplayName("When building call graph")
        class When_BuildingCallGraph {

            @Test
            @DisplayName("Then forward and reverse edges correct")
            void Then_ForwardAndReverseEdgesCorrect() {
                var builder = createBuilder();
                var result = builder.buildCallGraph(graph);

                assertThat(result.forwardEdges().get("m1")).containsExactly("m2");
                assertThat(result.forwardEdges().get("m2")).containsExactly("m3");
                assertThat(result.reverseEdges().get("m2")).containsExactly("m1");
                assertThat(result.reverseEdges().get("m3")).containsExactly("m2");
                assertThat(result.edgeCount()).isEqualTo(2);
            }

            @Test
            @DisplayName("Then direct callers and callees via graph queries work")
            void Then_DirectCallersAndCalleesWork() {
                var builder = createBuilder();

                assertThat(builder.getCalleesOf("m1", graph)).containsExactly("m2");
                assertThat(builder.getCallersOf("m2", graph)).containsExactly("m1");
                assertThat(builder.getCallersOf("m3", graph)).containsExactly("m2");
            }
        }
    }

    @Nested
    @DisplayName("Given cyclic calls")
    class Given_CyclicCalls {

        private final KnowledgeGraph graph = buildGraph(
                new KgNode[]{
                        methodNode("a", "methodA"),
                        methodNode("b", "methodB"),
                        methodNode("c", "methodC")
                },
                new KgEdge[]{
                        edge("e1", KgEdgeType.CALLS, "a", "b"),
                        edge("e2", KgEdgeType.CALLS, "b", "c"),
                        edge("e3", KgEdgeType.CALLS, "c", "a")
                }
        );

        @Nested
        @DisplayName("When getting transitive callees")
        class When_GettingTransitiveCallees {

            @Test
            @DisplayName("Then cycle detected, terminates")
            void Then_CycleDetectedTerminates() {
                var builder = createBuilder();

                var transitiveCallees = builder.getTransitiveCallees("a", graph);
                assertThat(transitiveCallees).containsExactlyInAnyOrder("b", "c");
            }

            @Test
            @DisplayName("Then transitive callers also handle cycle")
            void Then_TransitiveCallersAlsoHandleCycle() {
                var builder = createBuilder();

                var transitiveCallers = builder.getTransitiveCallers("a", graph);
                assertThat(transitiveCallers).containsExactlyInAnyOrder("b", "c");
            }
        }
    }

    @Nested
    @DisplayName("Given a linear call chain for reverse traversal")
    class Given_LinearCallChainForReverse {

        // m1 -> m2 -> m3
        private final KnowledgeGraph graph = buildGraph(
                new KgNode[]{
                        methodNode("m1", "main"),
                        methodNode("m2", "service"),
                        methodNode("m3", "repository")
                },
                new KgEdge[]{
                        edge("e1", KgEdgeType.CALLS, "m1", "m2"),
                        edge("e2", KgEdgeType.CALLS, "m2", "m3")
                }
        );

        @Nested
        @DisplayName("When getting transitive callers")
        class When_GettingTransitiveCallers {

            @Test
            @DisplayName("Then all upstream callers returned")
            void Then_AllUpstreamCallersReturned() {
                var builder = createBuilder();

                var callers = builder.getTransitiveCallers("m3", graph);
                assertThat(callers).containsExactlyInAnyOrder("m1", "m2");
            }

            @Test
            @DisplayName("Then root method has no callers")
            void Then_RootMethodHasNoCallers() {
                var builder = createBuilder();

                var callers = builder.getTransitiveCallers("m1", graph);
                assertThat(callers).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("Given an empty graph")
    class Given_EmptyGraph {

        private final KnowledgeGraph graph = new KnowledgeGraph();

        @Nested
        @DisplayName("When building call graph")
        class When_BuildingCallGraph {

            @Test
            @DisplayName("Then empty result")
            void Then_EmptyResult() {
                var builder = createBuilder();
                var result = builder.buildCallGraph(graph);

                assertThat(result.forwardEdges()).isEmpty();
                assertThat(result.reverseEdges()).isEmpty();
                assertThat(result.edgeCount()).isZero();
            }
        }
    }

    @Nested
    @DisplayName("Given method with multiple callers")
    class Given_MethodWithMultipleCallers {

        private final KnowledgeGraph graph = buildGraph(
                new KgNode[]{
                        methodNode("caller1", "callerOne"),
                        methodNode("caller2", "callerTwo"),
                        methodNode("caller3", "callerThree"),
                        methodNode("target", "sharedMethod")
                },
                new KgEdge[]{
                        edge("e1", KgEdgeType.CALLS, "caller1", "target"),
                        edge("e2", KgEdgeType.CALLS, "caller2", "target"),
                        edge("e3", KgEdgeType.CALLS, "caller3", "target")
                }
        );

        @Nested
        @DisplayName("When getting callers")
        class When_GettingCallers {

            @Test
            @DisplayName("Then all callers returned")
            void Then_AllCallersReturned() {
                var builder = createBuilder();

                assertThat(builder.getCallersOf("target", graph))
                        .containsExactlyInAnyOrder("caller1", "caller2", "caller3");
            }
        }
    }

    @Nested
    @DisplayName("Given an isolated method")
    class Given_IsolatedMethod {

        private final KnowledgeGraph graph = buildGraph(
                new KgNode[]{ methodNode("isolated", "loneMethod") },
                new KgEdge[]{}
        );

        @Nested
        @DisplayName("When getting callees")
        class When_GettingCallees {

            @Test
            @DisplayName("Then empty set")
            void Then_EmptySet() {
                var builder = createBuilder();

                assertThat(builder.getCalleesOf("isolated", graph)).isEmpty();
                assertThat(builder.getCallersOf("isolated", graph)).isEmpty();
            }
        }
    }
}
