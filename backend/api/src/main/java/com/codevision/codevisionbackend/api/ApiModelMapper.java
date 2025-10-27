package com.codevision.codevisionbackend.api;

import com.codevision.codevisionbackend.analyze.BuildInfo;
import com.codevision.codevisionbackend.analyze.ClassMetadataSummary;
import com.codevision.codevisionbackend.analyze.MetadataDump;
import com.codevision.codevisionbackend.analyze.ParsedDataResponse;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ApiModelMapper {

    public com.codevision.codevisionbackend.api.model.AnalyzeResponse toAnalyzeResponse(Long projectId, String status) {
        return new com.codevision.codevisionbackend.api.model.AnalyzeResponse()
                .projectId(projectId)
                .status(status);
    }

    public com.codevision.codevisionbackend.api.model.ParsedDataResponse toParsedDataResponse(
            ParsedDataResponse snapshot) {
        if (snapshot == null) {
            return null;
        }

        com.codevision.codevisionbackend.api.model.ParsedDataResponse response =
                new com.codevision.codevisionbackend.api.model.ParsedDataResponse()
                        .projectId(snapshot.projectId())
                        .projectName(snapshot.projectName())
                        .repoUrl(toUri(snapshot.repoUrl()))
                        .analyzedAt(snapshot.analyzedAt());

        if (snapshot.buildInfo() != null) {
            response.setBuildInfo(toBuildInfo(snapshot.buildInfo()));
        }

        List<ClassMetadataSummary> classes = snapshot.classes();
        if (classes != null) {
            response.setClasses(classes.stream()
                    .filter(Objects::nonNull)
                    .map(this::toClassMetadataSummary)
                    .collect(Collectors.toList()));
        }

        MetadataDump metadataDump = snapshot.metadataDump();
        if (metadataDump != null) {
            response.setMetadataDump(toMetadataDump(metadataDump));
        }

        return response;
    }

    private com.codevision.codevisionbackend.api.model.BuildInfo toBuildInfo(BuildInfo buildInfo) {
        return new com.codevision.codevisionbackend.api.model.BuildInfo()
                .groupId(buildInfo.groupId())
                .artifactId(buildInfo.artifactId())
                .version(buildInfo.version())
                .javaVersion(buildInfo.javaVersion());
    }

    private com.codevision.codevisionbackend.api.model.ClassMetadataSummary toClassMetadataSummary(
            ClassMetadataSummary summary) {
        return new com.codevision.codevisionbackend.api.model.ClassMetadataSummary()
                .fullyQualifiedName(summary.fullyQualifiedName())
                .packageName(summary.packageName())
                .className(summary.className())
                .stereotype(summary.stereotype())
                .userCode(summary.userCode())
                .sourceSet(summary.sourceSet())
                .relativePath(summary.relativePath())
                .annotations(summary.annotations())
                .interfacesImplemented(summary.interfacesImplemented());
    }

    private com.codevision.codevisionbackend.api.model.MetadataDump toMetadataDump(MetadataDump metadataDump) {
        com.codevision.codevisionbackend.api.model.MetadataDump mapped =
                new com.codevision.codevisionbackend.api.model.MetadataDump();
        List<MetadataDump.OpenApiSpec> specs = metadataDump.openApiSpecs();
        if (specs != null) {
            mapped.setOpenApiSpecs(specs.stream()
                    .filter(Objects::nonNull)
                    .map(this::toOpenApiSpec)
                    .collect(Collectors.toList()));
        }
        return mapped;
    }

    private com.codevision.codevisionbackend.api.model.OpenApiSpec toOpenApiSpec(MetadataDump.OpenApiSpec spec) {
        return new com.codevision.codevisionbackend.api.model.OpenApiSpec()
                .fileName(spec.fileName())
                .content(spec.content());
    }

    private URI toUri(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return URI.create(value);
    }
}
