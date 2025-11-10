package com.codevision.codevisionbackend.analysis;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Tarjan strongly connected components implementation used to detect cycles within the class
 * dependency graph.
 */
@Component
public class TarjanScc {

    public Result compute(Map<String, Set<String>> adjacency) {
        Objects.requireNonNull(adjacency, "adjacency");
        Map<String, Integer> index = new HashMap<>();
        Map<String, Integer> lowLink = new HashMap<>();
        Deque<String> stack = new ArrayDeque<>();
        Set<String> onStack = new HashSet<>();
        Map<String, Long> componentIds = new HashMap<>();
        Set<Long> cyclicComponents = new HashSet<>();
        int[] counter = {0};
        long[] componentCounter = {0};

        for (String node : adjacency.keySet()) {
            if (!index.containsKey(node)) {
                strongConnect(node, adjacency, index, lowLink, stack, onStack, componentIds, cyclicComponents, counter, componentCounter);
            }
        }

        // Isolated vertices may not appear as keys if they only appear as targets
        adjacency.values().forEach(neighbors -> neighbors.forEach(target -> {
            if (!componentIds.containsKey(target)) {
                componentCounter[0]++;
                componentIds.put(target, componentCounter[0]);
            }
        }));

        return new Result(componentIds, cyclicComponents);
    }

    private void strongConnect(
            String node,
            Map<String, Set<String>> adjacency,
            Map<String, Integer> index,
            Map<String, Integer> lowLink,
            Deque<String> stack,
            Set<String> onStack,
            Map<String, Long> componentIds,
            Set<Long> cyclicComponents,
            int[] depthCounter,
            long[] componentCounter) {
        index.put(node, depthCounter[0]);
        lowLink.put(node, depthCounter[0]);
        depthCounter[0]++;
        stack.push(node);
        onStack.add(node);

        for (String neighbor : adjacency.getOrDefault(node, Set.of())) {
            if (!index.containsKey(neighbor)) {
                strongConnect(neighbor, adjacency, index, lowLink, stack, onStack, componentIds, cyclicComponents, depthCounter, componentCounter);
                lowLink.put(node, Math.min(lowLink.get(node), lowLink.get(neighbor)));
            } else if (onStack.contains(neighbor)) {
                lowLink.put(node, Math.min(lowLink.get(node), index.get(neighbor)));
            }
        }

        if (Objects.equals(lowLink.get(node), index.get(node))) {
            componentCounter[0]++;
            List<String> members = new ArrayList<>();
            String w;
            do {
                w = stack.pop();
                onStack.remove(w);
                componentIds.put(w, componentCounter[0]);
                members.add(w);
            } while (!w.equals(node));
            if (members.size() > 1) {
                cyclicComponents.add(componentCounter[0]);
            }
        }
    }

    public record Result(Map<String, Long> componentIds, Set<Long> cyclicComponents) {
        public boolean isCyclic(String className) {
            Long componentId = componentIds.get(className);
            return componentId != null && cyclicComponents.contains(componentId);
        }
    }
}
