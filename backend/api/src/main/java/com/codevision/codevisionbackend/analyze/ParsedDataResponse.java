package com.codevision.codevisionbackend.analyze;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ParsedDataResponse(
        Long projectId,
        String projectName,
        String repoUrl,
        OffsetDateTime analyzedAt,
        BuildInfo buildInfo,
        List<ClassMetadataSummary> classes,
        MetadataDump metadataDump,
        List<ApiEndpointSummary> apiEndpoints,
        AssetInventory assets) {

    public ParsedDataResponse {
        classes = classes == null ? List.of() : List.copyOf(classes);
        metadataDump = metadataDump == null ? MetadataDump.empty() : metadataDump;
        apiEndpoints = apiEndpoints == null ? List.of() : List.copyOf(apiEndpoints);
        assets = assets == null ? AssetInventory.empty() : assets;
    }
}
