package com.codevision.codevisionbackend.analyze.scanner;

import java.util.List;
import java.util.Map;

public record DbAnalysisResult(
        List<DbEntityRecord> entities,
        Map<String, List<String>> classesByEntity,
        Map<String, List<DaoOperationRecord>> operationsByClass) {

    public DbAnalysisResult {
        entities = entities == null ? List.of() : List.copyOf(entities);
        classesByEntity = classesByEntity == null ? Map.of() : Map.copyOf(classesByEntity);
        operationsByClass = operationsByClass == null ? Map.of() : Map.copyOf(operationsByClass);
    }
}

