package com.codevision.codevisionbackend.usecase;

import com.codevision.codevisionbackend.graph.KgNodeType;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Use case: "Find duplicate code patterns in the graph."
 * Identifies classes with similar structural signatures (same method count, field patterns).
 */
@Service
public class DuplicationFinderService {

    public record DuplicationReport(List<DuplicateGroup> groups, int totalDuplicates) {}
    public record DuplicateGroup(String signature, List<String> nodeNames) {}

    public DuplicationReport find(KnowledgeGraph graph) {
        var classIds = graph.nodesOfType(KgNodeType.CLASS);
        if (classIds.isEmpty()) {
            return new DuplicationReport(List.of(), 0);
        }

        // Group classes by their structural signature (outgoing edge count + types)
        Map<String, List<String>> groups = new HashMap<>();
        for (var classId : classIds) {
            var node = graph.getNode(classId);
            var outEdges = graph.getNeighbors(classId);
            var signature = outEdges.stream()
                    .map(e -> e.type().name())
                    .sorted()
                    .collect(Collectors.joining(","));
            if (!signature.isEmpty()) {
                groups.computeIfAbsent(signature, k -> new ArrayList<>())
                        .add(node != null ? node.name() : classId);
            }
        }

        var duplicateGroups = groups.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .map(e -> new DuplicateGroup(e.getKey(), e.getValue()))
                .toList();

        int totalDupes = duplicateGroups.stream()
                .mapToInt(g -> g.nodeNames().size())
                .sum();

        return new DuplicationReport(duplicateGroups, totalDupes);
    }
}
