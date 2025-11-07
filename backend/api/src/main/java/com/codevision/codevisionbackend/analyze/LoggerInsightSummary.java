package com.codevision.codevisionbackend.analyze;

import java.util.List;

public record LoggerInsightSummary(
        String className,
        String filePath,
        String logLevel,
        int lineNumber,
        String messageTemplate,
        List<String> variables,
        boolean piiRisk,
        boolean pciRisk) {

    public LoggerInsightSummary {
        variables = variables == null ? List.of() : List.copyOf(variables);
    }
}
