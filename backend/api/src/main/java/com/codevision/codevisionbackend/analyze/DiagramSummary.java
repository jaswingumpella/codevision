package com.codevision.codevisionbackend.analyze;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DiagramSummary(
        Long diagramId,
        String diagramType,
        String title,
        String plantumlSource,
        String mermaidSource,
        String svgPath,
        Map<String, Object> metadata) {

    public DiagramSummary {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
