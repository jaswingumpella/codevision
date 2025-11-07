package com.codevision.codevisionbackend.analyze.diagram;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CallGraph {

    private final Map<String, GraphNode> nodes;
    private final Map<String, Set<String>> edges;
    private final Map<String, List<MethodInvocation>> methodInvocations;

    public CallGraph(
            Map<String, GraphNode> nodes,
            Map<String, Set<String>> edges,
            Map<String, List<MethodInvocation>> methodInvocations) {
        this.nodes = nodes == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(nodes));
        Map<String, Set<String>> transformed = new LinkedHashMap<>();
        if (edges != null) {
            edges.forEach((key, value) -> transformed.put(key, value == null ? Set.of() : Set.copyOf(value)));
        }
        this.edges = Collections.unmodifiableMap(transformed);
        Map<String, List<MethodInvocation>> invocationCopy = new LinkedHashMap<>();
        if (methodInvocations != null) {
            methodInvocations.forEach((key, value) -> invocationCopy.put(
                    key, value == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(value))));
        }
        this.methodInvocations = Collections.unmodifiableMap(invocationCopy);
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

    public List<MethodInvocation> methodCallsFrom(String className, String methodName) {
        if (className == null || methodName == null) {
            return List.of();
        }
        return methodInvocations.getOrDefault(methodKey(className, methodName), List.of());
    }

    private static String methodKey(String className, String methodName) {
        return className + "#" + methodName;
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

    public static class MethodInvocation {
        private final String sourceClass;
        private final String sourceMethod;
        private final String targetClass;
        private final String targetMethod;
        private final boolean targetExternal;

        public MethodInvocation(
                String sourceClass,
                String sourceMethod,
                String targetClass,
                String targetMethod,
                boolean targetExternal) {
            this.sourceClass = sourceClass;
            this.sourceMethod = sourceMethod;
            this.targetClass = targetClass;
            this.targetMethod = targetMethod;
            this.targetExternal = targetExternal;
        }

        public String sourceClass() {
            return sourceClass;
        }

        public String sourceMethod() {
            return sourceMethod;
        }

        public String targetClass() {
            return targetClass;
        }

        public String targetMethod() {
            return targetMethod;
        }

        public boolean targetExternal() {
            return targetExternal;
        }
    }

    public static class Builder {
        private final Map<String, GraphNode> nodes = new LinkedHashMap<>();
        private final Map<String, Set<String>> edges = new LinkedHashMap<>();
        private final Map<String, List<MethodInvocation>> methodCalls = new LinkedHashMap<>();

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

        public void addMethodCall(
                String sourceClass,
                String sourceMethod,
                String targetClass,
                String targetMethod,
                boolean targetExternal) {
            if (sourceClass == null
                    || sourceMethod == null
                    || targetClass == null
                    || targetMethod == null) {
                return;
            }
            MethodInvocation invocation =
                    new MethodInvocation(sourceClass, sourceMethod, targetClass, targetMethod, targetExternal);
            methodCalls.computeIfAbsent(methodKey(sourceClass, sourceMethod), key -> new ArrayList<>()).add(invocation);
        }

        public CallGraph build() {
            return new CallGraph(nodes, edges, methodCalls);
        }

        private String methodKey(String className, String methodName) {
            return className + "#" + methodName;
        }
    }
}
