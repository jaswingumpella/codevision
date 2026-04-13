package com.codevision.codevisionbackend.dependency;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Recursive tree representation of resolved dependencies.
 * Uses a mutable internal children list during construction, then provides
 * an unmodifiable view via {@link #children()}.
 */
public final class DependencyTree {

    private final ResolvedArtifact artifact;
    private final List<DependencyTree> mutableChildren;

    public DependencyTree(ResolvedArtifact artifact) {
        this(artifact, new ArrayList<>());
    }

    public DependencyTree(ResolvedArtifact artifact, List<DependencyTree> children) {
        this.artifact = artifact;
        this.mutableChildren = new ArrayList<>(children);
    }

    public ResolvedArtifact artifact() {
        return artifact;
    }

    /**
     * Returns an unmodifiable view of the children.
     * Use {@link #addChild(DependencyTree)} during construction.
     */
    public List<DependencyTree> children() {
        return Collections.unmodifiableList(mutableChildren);
    }

    /**
     * Adds a child node during tree construction.
     */
    void addChild(DependencyTree child) {
        mutableChildren.add(child);
    }

    /**
     * Flattens the tree into a list of all artifacts (depth-first).
     * Uses cycle detection via visited set.
     */
    public List<ResolvedArtifact> flatten() {
        List<ResolvedArtifact> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        flattenRecursive(this, result, visited);
        return result;
    }

    private static void flattenRecursive(DependencyTree node, List<ResolvedArtifact> accumulator,
                                         Set<String> visited) {
        var coords = node.artifact().coordinates();
        if (!visited.add(coords)) {
            return;
        }
        accumulator.add(node.artifact());
        for (DependencyTree child : node.mutableChildren) {
            flattenRecursive(child, accumulator, visited);
        }
    }

    /**
     * Returns the depth of the deepest transitive dependency chain.
     * Uses cycle detection to prevent infinite recursion.
     */
    public int maxDepth() {
        return maxDepthWithVisited(new HashSet<>());
    }

    private int maxDepthWithVisited(Set<String> visited) {
        if (!visited.add(artifact.coordinates())) {
            return 0;
        }
        if (mutableChildren.isEmpty()) {
            return 0;
        }
        int max = 0;
        for (DependencyTree child : mutableChildren) {
            max = Math.max(max, child.maxDepthWithVisited(visited) + 1);
        }
        return max;
    }

    /**
     * Total number of unique artifacts in this tree.
     */
    public int totalArtifactCount() {
        return flatten().size();
    }
}
