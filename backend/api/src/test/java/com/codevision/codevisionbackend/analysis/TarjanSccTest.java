package com.codevision.codevisionbackend.analysis;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TarjanSccTest {

    private final TarjanScc tarjanScc = new TarjanScc();

    @Test
    void detectsCycles() {
        Map<String, Set<String>> graph = new HashMap<>();
        graph.put("A", Set.of("B"));
        graph.put("B", Set.of("C"));
        graph.put("C", Set.of("A"));

        TarjanScc.Result result = tarjanScc.compute(graph);
        assertTrue(result.isCyclic("A"));
        assertTrue(result.isCyclic("B"));
        assertTrue(result.isCyclic("C"));
    }

    @Test
    void handlesAcyclicGraph() {
        Map<String, Set<String>> graph = new HashMap<>();
        graph.put("A", Set.of("B"));
        graph.put("B", Set.of("C"));
        graph.put("C", Set.of());

        TarjanScc.Result result = tarjanScc.compute(graph);
        assertFalse(result.isCyclic("A"));
        assertFalse(result.isCyclic("B"));
        assertFalse(result.isCyclic("C"));
    }

    @Test
    void assignsComponentIdsToLeafTargets() {
        Map<String, Set<String>> graph = new HashMap<>();
        graph.put("A", Set.of("B"));

        TarjanScc.Result result = tarjanScc.compute(graph);
        assertTrue(result.componentIds().containsKey("B"));
    }
}
