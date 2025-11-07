package com.codevision.codevisionbackend.analyze.scanner;

public record PiiPciFindingRecord(
        String filePath, int lineNumber, String snippet, String matchType, String severity, boolean ignored) {}
