package com.codevision.codevisionbackend.analyze;

public record PiiPciFindingSummary(
        String filePath, int lineNumber, String snippet, String matchType, String severity, boolean ignored) {}
