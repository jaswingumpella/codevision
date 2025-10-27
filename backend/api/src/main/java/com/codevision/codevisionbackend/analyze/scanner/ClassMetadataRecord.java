package com.codevision.codevisionbackend.analyze.scanner;

import java.util.List;

public record ClassMetadataRecord(
        String fullyQualifiedName,
        String packageName,
        String className,
        List<String> annotations,
        List<String> implementedInterfaces,
        String stereotype,
        SourceSet sourceSet,
        String relativePath,
        boolean userCode) {

    public enum SourceSet {
        MAIN,
        TEST
    }
}
