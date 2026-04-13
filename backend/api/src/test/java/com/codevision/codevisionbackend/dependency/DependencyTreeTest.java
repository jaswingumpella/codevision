package com.codevision.codevisionbackend.dependency;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DependencyTreeTest {

    @Nested
    class Given_SingleNodeTree {

        @Nested
        class When_Querying {

            @Test
            void Then_FlattenReturnsOneArtifact() {
                var root = new DependencyTree(new ResolvedArtifact("com.example", "root", "1.0.0"));
                assertEquals(1, root.flatten().size());
                assertEquals("com.example", root.flatten().get(0).groupId());
            }

            @Test
            void Then_MaxDepthIsZero() {
                var root = new DependencyTree(new ResolvedArtifact("com.example", "root", "1.0.0"));
                assertEquals(0, root.maxDepth());
            }

            @Test
            void Then_TotalCountIsOne() {
                var root = new DependencyTree(new ResolvedArtifact("com.example", "root", "1.0.0"));
                assertEquals(1, root.totalArtifactCount());
            }
        }
    }

    @Nested
    class Given_TreeWithChildren {

        @Nested
        class When_Querying {

            @Test
            void Then_FlattenReturnsAllArtifacts() {
                var child1 = new DependencyTree(new ResolvedArtifact("com.dep", "dep-a", "2.0.0"));
                var child2 = new DependencyTree(new ResolvedArtifact("com.dep", "dep-b", "3.0.0"));
                var root = new DependencyTree(
                        new ResolvedArtifact("com.example", "root", "1.0.0"),
                        List.of(child1, child2));

                var flat = root.flatten();
                assertEquals(3, flat.size());
            }

            @Test
            void Then_MaxDepthIsOne() {
                var child = new DependencyTree(new ResolvedArtifact("com.dep", "dep-a", "2.0.0"));
                var root = new DependencyTree(
                        new ResolvedArtifact("com.example", "root", "1.0.0"),
                        List.of(child));
                assertEquals(1, root.maxDepth());
            }
        }
    }

    @Nested
    class Given_DeeplyNestedTree {

        @Nested
        class When_Querying {

            @Test
            void Then_MaxDepthReflectsDeepestChain() {
                var leaf = new DependencyTree(new ResolvedArtifact("com.deep", "leaf", "1.0.0"));
                var mid = new DependencyTree(
                        new ResolvedArtifact("com.deep", "mid", "1.0.0"),
                        List.of(leaf));
                var root = new DependencyTree(
                        new ResolvedArtifact("com.deep", "root", "1.0.0"),
                        List.of(mid));

                assertEquals(2, root.maxDepth());
                assertEquals(3, root.totalArtifactCount());
            }
        }
    }
}
