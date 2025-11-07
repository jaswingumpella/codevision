package com.codevision.codevisionbackend.analyze.diagram;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class CallGraph {

    private final Map<String, GraphNode> nodes;
    private final Map<String, Set<String>> edges;

    public CallGraph(Map<String, GraphNode> nodes, Map<String, Set<String>> edges) {
        this.nodes = nodes == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(nodes));
        Map<String, Set<String>> transformed = new LinkedHashMap<>();
        if (edges != null) {
            edges.forEach((key, value) -> transformed.put(key, value == null ? Set.of() : Set.copyOf(value)));
        }
        this.edges = Collections.unmodifiableMap(transformed);
    }

    public Map<String, GraphNode> nodes() {
        return nodes;
    }

    public Map<String, Set<String>> edges() {
        return edges;
    }

    public Set<String> targets(String source) {
        return edges.getOrDefault(source, Set.of());
    }

    public static class GraphNode {
        private final String fullyQualifiedName;
        private final String simpleName;
        private final String stereotype;
        private final boolean userCode;
        private final boolean external;

        public GraphNode(String fullyQualifiedName, String simpleName, String stereotype, boolean userCode, boolean external) {
            this.fullyQualifiedName = fullyQualifiedName;
            this.simpleName = simpleName;
            this.stereotype = stereotype;
            this.userCode = userCode;
            this.external = external;
        }

        public String fullyQualifiedName() {
            return fullyQualifiedName;
        }

        public String simpleName() {
            return simpleName;
        }

        public String stereotype() {
            return stereotype;
        }

        public boolean userCode() {
            return userCode;
        }

        public boolean external() {
            return external;
        }
    }

    public static class Builder {
        private final Map<String, GraphNode> nodes = new LinkedHashMap<>();
        private final Map<String, Set<String>> edges = new LinkedHashMap<>();

        public void addNode(GraphNode node) {
            if (node == null || node.fullyQualifiedName() == null) {
                return;
            }
            nodes.putIfAbsent(node.fullyQualifiedName(), node);
            edges.putIfAbsent(node.fullyQualifiedName(), new LinkedHashSet<>());
        }

        public void addEdge(String source, String target) {
            if (source == null || target == null || source.equals(target)) {
                return;
            }
            edges.computeIfAbsent(source, key -> new LinkedHashSet<>()).add(target);
        }

        public CallGraph build() {
            return new CallGraph(nodes, edges);
        }
    }
}
