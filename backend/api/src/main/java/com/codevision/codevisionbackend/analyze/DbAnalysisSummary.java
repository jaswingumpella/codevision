package com.codevision.codevisionbackend.analyze;

import java.util.List;
import java.util.Map;

public record DbAnalysisSummary(
        List<DbEntitySummary> entities,
        Map<String, List<String>> classesByEntity,
        Map<String, List<DaoOperationDetails>> operationsByClass) {

    public DbAnalysisSummary {
        entities = entities == null ? List.of() : List.copyOf(entities);
        classesByEntity = classesByEntity == null ? Map.of() : Map.copyOf(classesByEntity);
        operationsByClass = operationsByClass == null ? Map.of() : Map.copyOf(operationsByClass);
    }

    public record DbEntitySummary(
            String entityName,
            String fullyQualifiedName,
            String tableName,
            List<String> primaryKeys,
            List<FieldSummary> fields,
            List<RelationshipSummary> relationships) {

        public DbEntitySummary {
            primaryKeys = primaryKeys == null ? List.of() : List.copyOf(primaryKeys);
            fields = fields == null ? List.of() : List.copyOf(fields);
            relationships = relationships == null ? List.of() : List.copyOf(relationships);
        }

        public record FieldSummary(String name, String type, String columnName) {}

        public record RelationshipSummary(String fieldName, String targetType, String relationshipType) {}
    }

    public record DaoOperationDetails(
            String methodName, String operationType, String target, String querySnippet) {}
}

