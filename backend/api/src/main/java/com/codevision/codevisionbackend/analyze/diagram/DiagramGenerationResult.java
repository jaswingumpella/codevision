package com.codevision.codevisionbackend.analyze.diagram;

import java.util.List;
import java.util.Map;

public record DiagramGenerationResult(
        List<DiagramDefinition> diagrams, Map<String, List<String>> callFlows) {

    public DiagramGenerationResult {
        diagrams = diagrams == null ? List.of() : List.copyOf(diagrams);
        callFlows = callFlows == null ? Map.of() : Map.copyOf(callFlows);
    }
}
