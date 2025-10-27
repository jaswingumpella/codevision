package com.codevision.codevisionbackend.analyze.scanner;

import java.util.List;

public record DbEntityRecord(
        String className,
        String fullyQualifiedName,
        String tableName,
        List<String> primaryKeys,
        List<EntityField> fields,
        List<EntityRelationship> relationships) {

    public DbEntityRecord {
        primaryKeys = primaryKeys == null ? List.of() : List.copyOf(primaryKeys);
        fields = fields == null ? List.of() : List.copyOf(fields);
        relationships = relationships == null ? List.of() : List.copyOf(relationships);
    }

    public record EntityField(String name, String type, String columnName) {}

    public record EntityRelationship(String fieldName, String targetType, String relationshipType) {}
}

