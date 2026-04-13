package com.codevision.codevisionbackend.callgraph;

import com.codevision.codevisionbackend.config.AnalysisSafetyProperties;
import com.codevision.codevisionbackend.graph.KgEdge;
import com.codevision.codevisionbackend.graph.KgEdgeType;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Builds and queries a class hierarchy from a {@link KnowledgeGraph} by
 * analysing {@link KgEdgeType#EXTENDS} and {@link KgEdgeType#IMPLEMENTS} edges.
 * All traversals use cycle detection via visited-set and a time-based deadline
 * -- no hardcoded depth limits.
 */
@Component
public class ClassHierarchyAnalysis {

    /**
     * Immutable result of a class-hierarchy build.
     *
     * @param subtypeMap   maps each class/interface id to its direct subtypes
     * @param supertypeMap maps each class/interface id to its direct supertypes
     */
    public record ClassHierarchy(
            Map<String, Set<String>> subtypeMap,
            Map<String, Set<String>> supertypeMap
    ) {}

    private final long maxRuntimeSeconds;

    public ClassHierarchyAnalysis(AnalysisSafetyProperties safetyProperties) {
        this.maxRuntimeSeconds = safetyProperties.maxRuntimeSeconds();
    }

    /**
     * Builds the full class hierarchy from EXTENDS and IMPLEMENTS edges.
     *
     * @param graph the knowledge graph to analyse
     * @return a {@link ClassHierarchy} with subtype and supertype maps
     */
    public ClassHierarchy buildHierarchy(KnowledgeGraph graph) {
        Objects.requireNonNull(graph, "graph must not be null");

        Map<String, Set<String>> subtypeMap = new ConcurrentHashMap<>();
        Map<String, Set<String>> supertypeMap = new ConcurrentHashMap<>();

        processEdges(graph.edgesOfType(KgEdgeType.EXTENDS), subtypeMap, supertypeMap);
        processEdges(graph.edgesOfType(KgEdgeType.IMPLEMENTS), subtypeMap, supertypeMap);

        return new ClassHierarchy(
                Collections.unmodifiableMap(subtypeMap),
                Collections.unmodifiableMap(supertypeMap)
        );
    }

    /**
     * Returns all transitive subtypes of the given class/interface.
     *
     * @param classId the class or interface id
     * @return set of all transitive subtype ids (does not include classId itself)
     */
    public Set<String> getSubtypes(String classId) {
        return getSubtypes(classId, buildDefaultHierarchy());
    }

    /**
     * Returns all transitive subtypes using a pre-built hierarchy.
     */
    public Set<String> getSubtypes(String classId, ClassHierarchy hierarchy) {
        Objects.requireNonNull(classId, "classId must not be null");
        Objects.requireNonNull(hierarchy, "hierarchy must not be null");
        return bfsCollect(classId, hierarchy.subtypeMap());
    }

    /**
     * Returns all transitive supertypes of the given class/interface.
     *
     * @param classId the class or interface id
     * @return set of all transitive supertype ids (does not include classId itself)
     */
    public Set<String> getSupertypes(String classId) {
        return getSupertypes(classId, buildDefaultHierarchy());
    }

    /**
     * Returns all transitive supertypes using a pre-built hierarchy.
     */
    public Set<String> getSupertypes(String classId, ClassHierarchy hierarchy) {
        Objects.requireNonNull(classId, "classId must not be null");
        Objects.requireNonNull(hierarchy, "hierarchy must not be null");
        return bfsCollect(classId, hierarchy.supertypeMap());
    }

    /**
     * CHA-based dispatch resolution: given a call target class and method name,
     * returns all classes that could be the actual runtime target (the class
     * itself, if it has the method, plus all subtypes that override the method).
     *
     * @param methodCallTargetClass the declared type of the call target
     * @param methodName            the method name being called
     * @param hierarchy             pre-built class hierarchy
     * @return set of class ids that could be the runtime dispatch target
     */
    public Set<String> resolveDispatchTargets(String methodCallTargetClass,
                                               String methodName,
                                               ClassHierarchy hierarchy) {
        Objects.requireNonNull(methodCallTargetClass, "methodCallTargetClass must not be null");
        Objects.requireNonNull(methodName, "methodName must not be null");
        Objects.requireNonNull(hierarchy, "hierarchy must not be null");

        Set<String> targets = new LinkedHashSet<>();
        // The target class itself is always a candidate
        targets.add(methodCallTargetClass);

        // All transitive subtypes are also candidates (they may override the method)
        Set<String> subtypes = getSubtypes(methodCallTargetClass, hierarchy);
        targets.addAll(subtypes);

        return Collections.unmodifiableSet(targets);
    }

    // ── Internal helpers ────────────────────────────────────────────────────

    private void processEdges(List<KgEdge> edges,
                              Map<String, Set<String>> subtypeMap,
                              Map<String, Set<String>> supertypeMap) {
        for (KgEdge edge : edges) {
            if (edge.sourceNodeId() == null || edge.targetNodeId() == null) {
                continue;
            }
            // source EXTENDS/IMPLEMENTS target => source is subtype of target
            String subtype = edge.sourceNodeId();
            String supertype = edge.targetNodeId();

            subtypeMap
                    .computeIfAbsent(supertype, k -> ConcurrentHashMap.newKeySet())
                    .add(subtype);
            supertypeMap
                    .computeIfAbsent(subtype, k -> ConcurrentHashMap.newKeySet())
                    .add(supertype);
        }
    }

    private Set<String> bfsCollect(String startId, Map<String, Set<String>> adjacency) {
        Instant deadline = Instant.now().plusSeconds(maxRuntimeSeconds);
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new ArrayDeque<>();

        Set<String> directNeighbors = adjacency.getOrDefault(startId, Set.of());
        queue.addAll(directNeighbors);
        visited.add(startId); // mark start as visited to detect cycles back to it

        Set<String> result = new LinkedHashSet<>();

        while (!queue.isEmpty()) {
            if (Instant.now().isAfter(deadline)) {
                break;
            }
            String current = queue.poll();
            if (!visited.add(current)) {
                continue; // cycle detection
            }
            result.add(current);

            Set<String> neighbors = adjacency.getOrDefault(current, Set.of());
            for (String neighbor : neighbors) {
                if (!visited.contains(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }

        return Collections.unmodifiableSet(result);
    }

    /**
     * Builds a default (empty) hierarchy when no graph context is available.
     * Callers should prefer the overloads that accept a pre-built hierarchy.
     */
    private ClassHierarchy buildDefaultHierarchy() {
        return new ClassHierarchy(Map.of(), Map.of());
    }
}
