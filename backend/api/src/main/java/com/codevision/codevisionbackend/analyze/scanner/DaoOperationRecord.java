package com.codevision.codevisionbackend.analyze.scanner;

public record DaoOperationRecord(
        String repositoryClass,
        String methodName,
        String operationType,
        String target,
        String querySnippet) {}

