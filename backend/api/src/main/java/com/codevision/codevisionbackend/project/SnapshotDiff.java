package com.codevision.codevisionbackend.project;

import java.util.List;

public record SnapshotDiff(
        Long baseSnapshotId,
        Long compareSnapshotId,
        String baseCommitHash,
        String compareCommitHash,
        List<ClassRef> addedClasses,
        List<ClassRef> removedClasses,
        List<EndpointRef> addedEndpoints,
        List<EndpointRef> removedEndpoints,
        List<DbEntityRef> addedEntities,
        List<DbEntityRef> removedEntities) {

    public record ClassRef(String fullyQualifiedName, String stereotype) {}

    public record EndpointRef(String protocol, String httpMethod, String pathOrOperation) {
        public String identity() {
            String proto = protocol == null ? "" : protocol;
            String method = httpMethod == null ? "*" : httpMethod;
            String path = pathOrOperation == null ? "" : pathOrOperation;
            return proto + "::" + method + "::" + path;
        }
    }

    public record DbEntityRef(String entityName, String tableName) {
        public String identity() {
            String entity = entityName == null ? "" : entityName;
            String table = tableName == null ? "" : tableName;
            return entity + "::" + table;
        }
    }
}
