package com.codevision.codevisionbackend.callgraph;

import com.codevision.codevisionbackend.callgraph.ClassHierarchyAnalysis.ClassHierarchy;
import com.codevision.codevisionbackend.graph.KgEdge;
import com.codevision.codevisionbackend.graph.KgEdgeType;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Resolves virtual/interface dispatch: given a call site (target class,
 * target method), determines all possible runtime targets using
 * Class Hierarchy Analysis (CHA).
 */
@Component
public class DispatchResolver {

    private final ClassHierarchyAnalysis classHierarchyAnalysis;

    public DispatchResolver(ClassHierarchyAnalysis classHierarchyAnalysis) {
        this.classHierarchyAnalysis = Objects.requireNonNull(classHierarchyAnalysis);
    }

    /**
     * Uses CHA to find all possible concrete implementations of a method call.
     * Returns the target class itself plus all subtypes that could override
     * the method at runtime.
     *
     * @param targetClassId the declared type of the call target
     * @param methodName    the method name being called
     * @param graph         the knowledge graph
     * @return set of class ids that could be the runtime dispatch target
     */
    public Set<String> resolveTargets(String targetClassId,
                                       String methodName,
                                       KnowledgeGraph graph) {
        Objects.requireNonNull(targetClassId, "targetClassId must not be null");
        Objects.requireNonNull(methodName, "methodName must not be null");
        Objects.requireNonNull(graph, "graph must not be null");

        ClassHierarchy hierarchy = classHierarchyAnalysis.buildHierarchy(graph);

        Set<String> candidates = new LinkedHashSet<>();
        candidates.add(targetClassId);

        Set<String> subtypes = classHierarchyAnalysis.getSubtypes(targetClassId, hierarchy);
        candidates.addAll(subtypes);

        // Filter to only classes that actually declare/override the method
        Set<String> targets = new LinkedHashSet<>();
        for (String classId : candidates) {
            if (hasMethod(classId, methodName, graph)) {
                targets.add(classId);
            }
        }

        // If no concrete implementations found, still return the original target
        if (targets.isEmpty()) {
            targets.add(targetClassId);
        }

        return Collections.unmodifiableSet(targets);
    }

    /**
     * Checks if a method overrides a parent method by looking for an
     * {@link KgEdgeType#OVERRIDES} edge from methodId to parentMethodId.
     *
     * @param methodId       the potentially overriding method id
     * @param parentMethodId the parent method id
     * @param graph          the knowledge graph
     * @return true if an OVERRIDES edge exists from methodId to parentMethodId
     */
    public boolean isOverride(String methodId,
                              String parentMethodId,
                              KnowledgeGraph graph) {
        Objects.requireNonNull(methodId, "methodId must not be null");
        Objects.requireNonNull(parentMethodId, "parentMethodId must not be null");
        Objects.requireNonNull(graph, "graph must not be null");

        for (KgEdge edge : graph.getNeighbors(methodId)) {
            if (edge.type() == KgEdgeType.OVERRIDES
                    && parentMethodId.equals(edge.targetNodeId())) {
                return true;
            }
        }
        return false;
    }

    // ── Internal helpers ────────────────────────────────────────────────────

    /**
     * Checks whether a class declares a method with the given name by
     * examining DECLARES edges from the class and checking the node names.
     */
    private boolean hasMethod(String classId, String methodName, KnowledgeGraph graph) {
        for (KgEdge edge : graph.getNeighbors(classId)) {
            if (edge.type() == KgEdgeType.DECLARES && edge.targetNodeId() != null) {
                var targetNode = graph.getNode(edge.targetNodeId());
                if (targetNode != null && methodName.equals(targetNode.name())) {
                    return true;
                }
            }
        }
        return false;
    }
}
