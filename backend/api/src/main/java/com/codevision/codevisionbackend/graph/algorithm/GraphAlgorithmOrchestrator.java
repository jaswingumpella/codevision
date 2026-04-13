package com.codevision.codevisionbackend.graph.algorithm;

import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Orchestrates execution of all registered graph algorithms.
 * Runs each algorithm and collects results keyed by algorithm name.
 */
@Component
public class GraphAlgorithmOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(GraphAlgorithmOrchestrator.class);

    private final List<GraphAlgorithm<?>> algorithms;

    public GraphAlgorithmOrchestrator(List<GraphAlgorithm<?>> algorithms) {
        this.algorithms = algorithms;
    }

    /**
     * Runs all registered algorithms on the given graph.
     *
     * @param graph the knowledge graph to analyze
     * @return map of algorithm name to result
     */
    public Map<String, Object> runAll(KnowledgeGraph graph) {
        Map<String, Object> results = new LinkedHashMap<>();
        for (var algorithm : algorithms) {
            try {
                log.info("Running algorithm: {}", algorithm.name());
                var result = algorithm.execute(graph);
                results.put(algorithm.name(), result);
                log.info("Algorithm {} completed", algorithm.name());
            } catch (Exception e) {
                log.error("Algorithm {} failed: {}", algorithm.name(), e.getMessage(), e);
                results.put(algorithm.name(), Map.of("error", e.getMessage()));
            }
        }
        return results;
    }

    /**
     * Finds an algorithm by name.
     */
    public Optional<GraphAlgorithm<?>> findByName(String name) {
        return algorithms.stream()
                .filter(a -> a.name().equals(name))
                .findFirst();
    }

    /**
     * Returns the list of registered algorithms.
     */
    public List<GraphAlgorithm<?>> registeredAlgorithms() {
        return List.copyOf(algorithms);
    }
}
