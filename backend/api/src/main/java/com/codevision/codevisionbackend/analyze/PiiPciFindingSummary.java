package com.codevision.codevisionbackend.analyze;

public record PiiPciFindingSummary(
        Long findingId,
        String filePath,
        int lineNumber,
        String snippet,
        String matchType,
        String severity,
        boolean ignored) {}
