package com.codevision.codevisionbackend.graph.algorithm;

import static org.junit.jupiter.api.Assertions.*;

import com.codevision.codevisionbackend.graph.KgEdge;
import com.codevision.codevisionbackend.graph.KgEdgeType;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import com.codevision.codevisionbackend.graph.ConfidenceLevel;
import com.codevision.codevisionbackend.graph.Provenance;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CommunityDetectionAlgorithmTest {

    private final CommunityDetectionAlgorithm algorithm = new CommunityDetectionAlgorithm();

    @Nested
    class Given_TwoDisconnectedCliques {

        @Nested
        class When_DetectingCommunities {

            @Test
            void Then_FindsTwoCommunities() {
                var graph = GraphTestHelper.twoCliques(3);
                var result = algorithm.execute(graph);
                var communityIds = new HashSet<>(result.values());
                assertTrue(communityIds.size() >= 2,
                        "Should detect at least 2 communities in disconnected cliques, found: " + communityIds.size());
            }

            @Test
            void Then_NodesInSameCliqueShareCommunity() {
                var graph = GraphTestHelper.twoCliques(3);
                var result = algorithm.execute(graph);
                int comm0 = result.get("c0n0");
                assertEquals(comm0, result.get("c0n1"));
                assertEquals(comm0, result.get("c0n2"));
            }
        }
    }

    @Nested
    class Given_TwoCliquesConnectedByBridge {

        @Nested
        class When_DetectingCommunities {

            @Test
            void Then_FindsTwoCommunities() {
                // Use larger cliques (6 nodes each) so the single bridge is
                // clearly a weak link that the algorithm can detect
                var graph = new KnowledgeGraph();
                int cliqueSize = 6;
                // Clique A: a0-a5, fully connected
                for (int i = 0; i < cliqueSize; i++) {
                    graph.addNode(GraphTestHelper.classNode("a" + i, "A" + i));
                }
                for (int i = 0; i < cliqueSize; i++) {
                    for (int j = i + 1; j < cliqueSize; j++) {
                        graph.addEdge(GraphTestHelper.callsEdge("a" + i, "a" + j));
                        graph.addEdge(GraphTestHelper.callsEdge("a" + j, "a" + i));
                    }
                }
                // Clique B: b0-b5, fully connected
                for (int i = 0; i < cliqueSize; i++) {
                    graph.addNode(GraphTestHelper.classNode("b" + i, "B" + i));
                }
                for (int i = 0; i < cliqueSize; i++) {
                    for (int j = i + 1; j < cliqueSize; j++) {
                        graph.addEdge(GraphTestHelper.callsEdge("b" + i, "b" + j));
                        graph.addEdge(GraphTestHelper.callsEdge("b" + j, "b" + i));
                    }
                }
                // Single bridge
                graph.addEdge(GraphTestHelper.callsEdge("a0", "b0"));

                var result = algorithm.execute(graph);
                var communityIds = new HashSet<>(result.values());
                assertTrue(communityIds.size() >= 2,
                        "Two cliques with bridge should yield at least 2 communities, found: " + communityIds.size());

                // All nodes in same clique should share a community
                int commA = result.get("a0");
                for (int i = 1; i < cliqueSize; i++) {
                    assertEquals(commA, result.get("a" + i),
                            "Node a" + i + " should be in same community as a0");
                }

                int commB = result.get("b0");
                for (int i = 1; i < cliqueSize; i++) {
                    assertEquals(commB, result.get("b" + i),
                            "Node b" + i + " should be in same community as b0");
                }
            }
        }
    }

    @Nested
    class Given_SingleClique {

        @Nested
        class When_DetectingCommunities {

            @Test
            void Then_AllNodesInOneCommunity() {
                var graph = new KnowledgeGraph();
                for (int i = 0; i < 4; i++) {
                    graph.addNode(GraphTestHelper.classNode("n" + i, "Node" + i));
                }
                for (int i = 0; i < 4; i++) {
                    for (int j = 0; j < 4; j++) {
                        if (i != j) {
                            graph.addEdge(GraphTestHelper.callsEdge("n" + i, "n" + j));
                        }
                    }
                }
                var result = algorithm.execute(graph);
                var communityIds = new HashSet<>(result.values());
                assertEquals(1, communityIds.size(), "Fully connected graph should be one community");
            }
        }
    }

    @Nested
    class Given_DisconnectedComponents {

        @Nested
        class When_DetectingCommunities {

            @Test
            void Then_EachComponentIsSeparateCommunity() {
                var graph = new KnowledgeGraph();
                // 3 isolated pairs
                for (int i = 0; i < 3; i++) {
                    var a = "pair" + i + "a";
                    var b = "pair" + i + "b";
                    graph.addNode(GraphTestHelper.classNode(a, "A" + i));
                    graph.addNode(GraphTestHelper.classNode(b, "B" + i));
                    graph.addEdge(GraphTestHelper.callsEdge(a, b));
                    graph.addEdge(GraphTestHelper.callsEdge(b, a));
                }
                var result = algorithm.execute(graph);
                var communityIds = new HashSet<>(result.values());
                assertTrue(communityIds.size() >= 3,
                        "3 disconnected pairs should yield at least 3 communities, found: " + communityIds.size());
            }
        }
    }

    @Nested
    class Given_NodesWithNoEdges {

        @Nested
        class When_DetectingCommunities {

            @Test
            void Then_EachNodeIsOwnCommunity() {
                var graph = new KnowledgeGraph();
                graph.addNode(GraphTestHelper.classNode("a", "A"));
                graph.addNode(GraphTestHelper.classNode("b", "B"));
                graph.addNode(GraphTestHelper.classNode("c", "C"));

                var result = algorithm.execute(graph);

                assertEquals(3, result.size());
                var communityIds = new HashSet<>(result.values());
                assertEquals(3, communityIds.size(),
                        "Nodes with no edges should each be in their own community");
            }
        }
    }

    @Nested
    class Given_GraphWithSelfLoops {

        @Nested
        class When_DetectingCommunities {

            @Test
            void Then_SelfLoopsAreIgnored() {
                var graph = new KnowledgeGraph();
                graph.addNode(GraphTestHelper.classNode("a", "A"));
                graph.addNode(GraphTestHelper.classNode("b", "B"));
                // Self-loop on a
                graph.addEdge(GraphTestHelper.callsEdge("a", "a"));
                // Real edge between a and b
                graph.addEdge(GraphTestHelper.callsEdge("a", "b"));
                graph.addEdge(GraphTestHelper.callsEdge("b", "a"));

                var result = algorithm.execute(graph);
                assertEquals(2, result.size());
                // Both should be in the same community since they're connected
                assertEquals(result.get("a"), result.get("b"));
            }
        }
    }

    @Nested
    class Given_EdgesWithNullIds {

        @Nested
        class When_DetectingCommunities {

            @Test
            void Then_NullEdgesAreSkipped() {
                var graph = new KnowledgeGraph();
                graph.addNode(GraphTestHelper.classNode("a", "A"));
                graph.addNode(GraphTestHelper.classNode("b", "B"));
                // Add edge with null source
                graph.addEdge(new KgEdge("e1", KgEdgeType.CALLS, null, "b",
                        null, ConfidenceLevel.EXTRACTED,
                        new Provenance("test", "test.java", 0, ConfidenceLevel.EXTRACTED),
                        Map.of()));
                // Add edge with null target
                graph.addEdge(new KgEdge("e2", KgEdgeType.CALLS, "a", null,
                        null, ConfidenceLevel.EXTRACTED,
                        new Provenance("test", "test.java", 0, ConfidenceLevel.EXTRACTED),
                        Map.of()));

                var result = algorithm.execute(graph);
                // Should still produce results without NPE
                assertEquals(2, result.size());
                // No edges, so each node in own community
                var communityIds = new HashSet<>(result.values());
                assertEquals(2, communityIds.size());
            }
        }
    }

    @Nested
    class Given_HigherResolution {

        @Nested
        class When_DetectingCommunities {

            @Test
            void Then_ProducesAtLeastAsManyCommunitiesAsDefault() {
                var graph = GraphTestHelper.twoCliques(4);
                var defaultResult = new CommunityDetectionAlgorithm(1.0).execute(graph);
                var highResResult = new CommunityDetectionAlgorithm(2.0).execute(graph);

                var defaultCommunities = new HashSet<>(defaultResult.values()).size();
                var highResCommunities = new HashSet<>(highResResult.values()).size();

                assertTrue(highResCommunities >= defaultCommunities,
                        "Higher resolution should produce >= communities: default=" +
                                defaultCommunities + ", highRes=" + highResCommunities);
            }
        }
    }

    @Nested
    class Given_EmptyGraph {

        @Nested
        class When_DetectingCommunities {

            @Test
            void Then_ReturnsEmpty() {
                var graph = new KnowledgeGraph();
                var result = algorithm.execute(graph);
                assertTrue(result.isEmpty());
            }
        }
    }

    @Nested
    class Given_AlgorithmMetadata {

        @Nested
        class When_QueryingName {

            @Test
            void Then_ReturnsCommunityDetection() {
                assertEquals("community-detection", algorithm.name());
            }
        }
    }

    @Nested
    class Given_ConfigurableParameters {

        @Nested
        class When_UsingCustomConfig {

            @Test
            void Then_RespectsMaxRuntimeSeconds() {
                // Algorithm with 1-second deadline should still produce results
                var fastAlgorithm = new CommunityDetectionAlgorithm(1.0, 1, 100, 10);
                var graph = GraphTestHelper.twoCliques(3);
                var result = fastAlgorithm.execute(graph);
                assertFalse(result.isEmpty(), "Should produce results even with short deadline");
            }

            @Test
            void Then_RespectsMaxIterations() {
                // Algorithm with 1 outer iteration should still produce results
                var limitedAlgorithm = new CommunityDetectionAlgorithm(1.0, 60, 1, 1);
                var graph = GraphTestHelper.twoCliques(3);
                var result = limitedAlgorithm.execute(graph);
                assertFalse(result.isEmpty(), "Should produce results with limited iterations");
            }
        }
    }
}
