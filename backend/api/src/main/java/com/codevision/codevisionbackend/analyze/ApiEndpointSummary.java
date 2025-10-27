package com.codevision.codevisionbackend.analyze;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiEndpointSummary(
        String protocol,
        String httpMethod,
        String pathOrOperation,
        String controllerClass,
        String controllerMethod,
        List<ApiSpecArtifact> specArtifacts) {

    public ApiEndpointSummary {
        protocol = protocol == null ? "UNKNOWN" : protocol;
        specArtifacts = specArtifacts == null ? List.of() : List.copyOf(specArtifacts);
    }

    public ApiEndpointSummary withSpecArtifacts(List<ApiSpecArtifact> artifacts) {
        return new ApiEndpointSummary(
                protocol, httpMethod, pathOrOperation, controllerClass, controllerMethod, artifacts);
    }

    public boolean hasSpecArtifacts() {
        return specArtifacts != null && !specArtifacts.isEmpty();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ApiSpecArtifact(String type, String name, String reference) {

        public ApiSpecArtifact {
            type = type == null ? "UNKNOWN" : type;
            name = Objects.requireNonNullElse(name, "");
        }
    }
}

