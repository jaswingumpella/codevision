package com.codevision.codevisionbackend.analyze;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MetadataDump(List<OpenApiSpec> openApiSpecs) {

    public MetadataDump {
        openApiSpecs = openApiSpecs == null ? List.of() : List.copyOf(openApiSpecs);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record OpenApiSpec(String fileName, String content) {
    }

    public static MetadataDump empty() {
        return new MetadataDump(List.of());
    }
}
