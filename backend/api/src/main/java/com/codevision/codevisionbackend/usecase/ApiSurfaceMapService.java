package com.codevision.codevisionbackend.usecase;

import com.codevision.codevisionbackend.graph.KgNodeType;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Use case: "Show me my API surface: endpoint -> service -> repo -> table."
 * Traces each endpoint through its call chain to data sources.
 */
@Service
public class ApiSurfaceMapService {

    public record ApiSurfaceResult(List<EndpointChain> chains) {}
    public record EndpointChain(String endpointId, String endpointName, List<String> chainNodeIds) {}

    public ApiSurfaceResult map(KnowledgeGraph graph) {
        var endpointIds = graph.nodesOfType(KgNodeType.ENDPOINT);
        if (endpointIds.isEmpty()) {
            return new ApiSurfaceResult(List.of());
        }

        var chains = new ArrayList<EndpointChain>();
        for (var epId : endpointIds) {
            var node = graph.getNode(epId);
            var chain = traceChain(graph, epId);
            chains.add(new EndpointChain(epId,
                    node != null ? node.name() : epId, chain));
        }

        return new ApiSurfaceResult(chains);
    }

    private List<String> traceChain(KnowledgeGraph graph, String startId) {
        List<String> chain = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(startId);
        visited.add(startId);

        while (!queue.isEmpty()) {
            var current = queue.poll();
            chain.add(current);
            for (var edge : graph.getNeighbors(current)) {
                var target = edge.targetNodeId();
                if (target != null && visited.add(target)) {
                    queue.add(target);
                }
            }
        }

        return chain;
    }
}
