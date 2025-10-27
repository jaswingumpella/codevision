package com.codevision.codevisionbackend.api;

import com.codevision.codevisionbackend.analyze.ApiEndpointSummary;
import com.codevision.codevisionbackend.analyze.AssetInventory;
import com.codevision.codevisionbackend.analyze.BuildInfo;
import com.codevision.codevisionbackend.analyze.ClassMetadataSummary;
import com.codevision.codevisionbackend.analyze.MetadataDump;
import com.codevision.codevisionbackend.analyze.MetadataDump.SoapPortSummary;
import com.codevision.codevisionbackend.analyze.MetadataDump.SoapServiceSummary;
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

        List<ApiEndpointSummary> endpoints = snapshot.apiEndpoints();
        if (endpoints != null) {
            response.setApiEndpoints(endpoints.stream()
                    .filter(Objects::nonNull)
                    .map(this::toApiEndpoint)
                    .collect(Collectors.toList()));
        }

        AssetInventory assets = snapshot.assets();
        if (assets != null) {
            response.setAssets(toAssetInventory(assets));
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
        List<MetadataDump.SpecDocument> wsdlDocs = metadataDump.wsdlDocuments();
        if (wsdlDocs != null) {
            mapped.setWsdlDocuments(wsdlDocs.stream()
                    .filter(Objects::nonNull)
                    .map(this::toSpecDocument)
                    .collect(Collectors.toList()));
        }
        List<MetadataDump.SpecDocument> xsdDocs = metadataDump.xsdDocuments();
        if (xsdDocs != null) {
            mapped.setXsdDocuments(xsdDocs.stream()
                    .filter(Objects::nonNull)
                    .map(this::toSpecDocument)
                    .collect(Collectors.toList()));
        }
        List<SoapServiceSummary> soapServices = metadataDump.soapServices();
        if (soapServices != null) {
            mapped.setSoapServices(soapServices.stream()
                    .filter(Objects::nonNull)
                    .map(this::toSoapServiceSummary)
                    .collect(Collectors.toList()));
        }
        return mapped;
    }

    private com.codevision.codevisionbackend.api.model.OpenApiSpec toOpenApiSpec(MetadataDump.OpenApiSpec spec) {
        return new com.codevision.codevisionbackend.api.model.OpenApiSpec()
                .fileName(spec.fileName())
                .content(spec.content());
    }

    private com.codevision.codevisionbackend.api.model.SpecDocument toSpecDocument(MetadataDump.SpecDocument doc) {
        return new com.codevision.codevisionbackend.api.model.SpecDocument()
                .fileName(doc.fileName())
                .content(doc.content());
    }

    private com.codevision.codevisionbackend.api.model.SoapServiceSummary toSoapServiceSummary(SoapServiceSummary summary) {
        com.codevision.codevisionbackend.api.model.SoapServiceSummary mapped =
                new com.codevision.codevisionbackend.api.model.SoapServiceSummary()
                        .fileName(summary.fileName())
                        .serviceName(summary.serviceName());
        List<SoapPortSummary> ports = summary.ports();
        if (ports != null) {
            mapped.setPorts(ports.stream()
                    .filter(Objects::nonNull)
                    .map(this::toSoapPortSummary)
                    .collect(Collectors.toList()));
        }
        return mapped;
    }

    private com.codevision.codevisionbackend.api.model.SoapPortSummary toSoapPortSummary(SoapPortSummary summary) {
        return new com.codevision.codevisionbackend.api.model.SoapPortSummary()
                .portName(summary.portName())
                .operations(summary.operations());
    }

    private com.codevision.codevisionbackend.api.model.ApiEndpoint toApiEndpoint(ApiEndpointSummary summary) {
        com.codevision.codevisionbackend.api.model.ApiEndpoint mapped =
                new com.codevision.codevisionbackend.api.model.ApiEndpoint()
                        .protocol(summary.protocol())
                        .httpMethod(summary.httpMethod())
                        .pathOrOperation(summary.pathOrOperation())
                        .controllerClass(summary.controllerClass())
                        .controllerMethod(summary.controllerMethod());
        if (summary.specArtifacts() != null) {
            mapped.setSpecArtifacts(summary.specArtifacts().stream()
                    .filter(Objects::nonNull)
                    .map(this::toApiSpecArtifact)
                    .collect(Collectors.toList()));
        }
        return mapped;
    }

    private com.codevision.codevisionbackend.api.model.ApiSpecArtifact toApiSpecArtifact(
            ApiEndpointSummary.ApiSpecArtifact artifact) {
        return new com.codevision.codevisionbackend.api.model.ApiSpecArtifact()
                .type(artifact.type())
                .name(artifact.name())
                .reference(artifact.reference());
    }

    private com.codevision.codevisionbackend.api.model.AssetInventory toAssetInventory(AssetInventory assets) {
        com.codevision.codevisionbackend.api.model.AssetInventory mapped =
                new com.codevision.codevisionbackend.api.model.AssetInventory();
        if (assets.images() != null) {
            mapped.setImages(assets.images().stream()
                    .filter(Objects::nonNull)
                    .map(this::toImageAsset)
                    .collect(Collectors.toList()));
        }
        return mapped;
    }

    private com.codevision.codevisionbackend.api.model.ImageAsset toImageAsset(AssetInventory.ImageAsset asset) {
        return new com.codevision.codevisionbackend.api.model.ImageAsset()
                .fileName(asset.fileName())
                .relativePath(asset.relativePath())
                .sizeBytes(asset.sizeBytes())
                .sha256(asset.sha256());
    }

    public com.codevision.codevisionbackend.api.model.ProjectApiEndpointsResponse toApiEndpointsResponse(
            Long projectId, List<ApiEndpointSummary> endpoints) {
        com.codevision.codevisionbackend.api.model.ProjectApiEndpointsResponse response =
                new com.codevision.codevisionbackend.api.model.ProjectApiEndpointsResponse()
                        .projectId(projectId);
        if (endpoints != null) {
            response.setEndpoints(endpoints.stream()
                    .filter(Objects::nonNull)
                    .map(this::toApiEndpoint)
                    .collect(Collectors.toList()));
        }
        return response;
    }

    private URI toUri(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return URI.create(value);
    }
}
