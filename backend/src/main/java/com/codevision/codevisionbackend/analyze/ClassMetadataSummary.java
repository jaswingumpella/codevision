package com.codevision.codevisionbackend.analyze;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClassMetadataSummary(
        String fullyQualifiedName,
        String packageName,
        String className,
        String stereotype,
        boolean userCode,
        String sourceSet,
        String relativePath,
        List<String> annotations,
        List<String> interfacesImplemented) {
}
