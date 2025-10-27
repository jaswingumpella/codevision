package com.codevision.codevisionbackend.analyze.scanner;

import java.util.List;

public record ApiEndpointRecord(
        String protocol,
        String httpMethod,
        String pathOrOperation,
        String controllerClass,
        String controllerMethod,
        List<ApiSpecArtifactRecord> specArtifacts) {

    public ApiEndpointRecord {
        specArtifacts = specArtifacts == null ? List.of() : List.copyOf(specArtifacts);
    }

    public record ApiSpecArtifactRecord(String type, String name, String reference) {}
}

