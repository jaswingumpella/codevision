package com.codevision.codevisionbackend.graph.algorithm;

import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Finds strongly connected components (cycles) using Tarjan's algorithm.
 * Returns all SCCs with more than one node, indicating circular dependencies.
 */
@Component
public class TarjanSccAlgorithm implements GraphAlgorithm<List<Set<String>>> {

    @Override
    public String name() {
        return "tarjan-scc";
    }

    @Override
    public List<Set<String>> execute(KnowledgeGraph graph) {
        var nodeIds = graph.getNodes().keySet();
        if (nodeIds.isEmpty()) {
            return List.of();
        }

        Map<String, Set<String>> adjacency = graph.buildAdjacencyMap();
        Map<String, Integer> index = new HashMap<>();
        Map<String, Integer> lowlink = new HashMap<>();
        Map<String, Boolean> onStack = new HashMap<>();
        Deque<String> stack = new ArrayDeque<>();
        List<Set<String>> sccs = new ArrayList<>();
        int[] counter = {0};

        for (var nodeId : nodeIds) {
            if (!index.containsKey(nodeId)) {
                strongConnect(nodeId, adjacency, index, lowlink, onStack, stack, sccs, counter);
            }
        }

        // Filter to only non-trivial SCCs (size > 1 = circular dependency)
        return sccs.stream()
                .filter(scc -> scc.size() > 1)
                .toList();
    }

    private void strongConnect(String v, Map<String, Set<String>> adjacency,
                               Map<String, Integer> index, Map<String, Integer> lowlink,
                               Map<String, Boolean> onStack, Deque<String> stack,
                               List<Set<String>> sccs, int[] counter) {
        // Use iterative approach to avoid stack overflow on deep graphs
        Deque<Frame> callStack = new ArrayDeque<>();
        callStack.push(new Frame(v, adjacency.getOrDefault(v, Set.of()).iterator()));

        index.put(v, counter[0]);
        lowlink.put(v, counter[0]);
        counter[0]++;
        stack.push(v);
        onStack.put(v, true);

        while (!callStack.isEmpty()) {
            var frame = callStack.peek();
            if (frame.neighbors.hasNext()) {
                var w = frame.neighbors.next();
                if (!index.containsKey(w)) {
                    index.put(w, counter[0]);
                    lowlink.put(w, counter[0]);
                    counter[0]++;
                    stack.push(w);
                    onStack.put(w, true);
                    callStack.push(new Frame(w, adjacency.getOrDefault(w, Set.of()).iterator()));
                } else if (Boolean.TRUE.equals(onStack.get(w))) {
                    lowlink.put(frame.nodeId, Math.min(lowlink.get(frame.nodeId), index.get(w)));
                }
            } else {
                // Done processing this node
                callStack.pop();
                if (!callStack.isEmpty()) {
                    var parent = callStack.peek();
                    lowlink.put(parent.nodeId, Math.min(lowlink.get(parent.nodeId), lowlink.get(frame.nodeId)));
                }

                if (lowlink.get(frame.nodeId).equals(index.get(frame.nodeId))) {
                    Set<String> scc = new LinkedHashSet<>();
                    String w;
                    do {
                        w = stack.pop();
                        onStack.put(w, false);
                        scc.add(w);
                    } while (!w.equals(frame.nodeId));
                    sccs.add(scc);
                }
            }
        }
    }

    private record Frame(String nodeId, Iterator<String> neighbors) {}
}
