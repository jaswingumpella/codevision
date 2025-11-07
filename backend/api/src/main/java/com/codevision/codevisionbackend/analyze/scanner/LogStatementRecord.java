package com.codevision.codevisionbackend.analyze.scanner;

import java.util.List;

public record LogStatementRecord(
        String className,
        String filePath,
        String logLevel,
        int lineNumber,
        String messageTemplate,
        List<String> variables,
        boolean piiRisk,
        boolean pciRisk) {

    public LogStatementRecord {
        variables = variables == null ? List.of() : List.copyOf(variables);
    }
}
