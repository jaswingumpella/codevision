package com.codevision.codevisionbackend.analyze.diagram;

import com.codevision.codevisionbackend.project.diagram.DiagramType;
import java.util.LinkedHashMap;
import java.util.Map;

public record DiagramDefinition(
        DiagramType type,
        String title,
        String plantumlSource,
        String mermaidSource,
        Map<String, Object> metadata) {

    public DiagramDefinition {
        if (type == null) {
            throw new IllegalArgumentException("Diagram type is required");
        }
        title = (title == null || title.isBlank()) ? type.name() : title;
        metadata = sanitizeMetadata(metadata);
    }

    private Map<String, Object> sanitizeMetadata(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> sanitized = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (key != null && value != null) {
                sanitized.put(key, value);
            }
        });
        return sanitized.isEmpty() ? Map.of() : Map.copyOf(sanitized);
    }
}
