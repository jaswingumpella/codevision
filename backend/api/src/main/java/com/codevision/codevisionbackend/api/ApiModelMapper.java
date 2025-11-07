package com.codevision.codevisionbackend.api;

import com.codevision.codevisionbackend.analyze.ApiEndpointSummary;
import com.codevision.codevisionbackend.analyze.AssetInventory;
import com.codevision.codevisionbackend.analyze.BuildInfo;
import com.codevision.codevisionbackend.analyze.ClassMetadataSummary;
import com.codevision.codevisionbackend.analyze.DbAnalysisSummary;
import com.codevision.codevisionbackend.analyze.DiagramSummary;
import com.codevision.codevisionbackend.analyze.LoggerInsightSummary;
import com.codevision.codevisionbackend.analyze.MetadataDump;
import com.codevision.codevisionbackend.analyze.MetadataDump.SoapPortSummary;
import com.codevision.codevisionbackend.analyze.MetadataDump.SoapServiceSummary;
import com.codevision.codevisionbackend.analyze.ParsedDataResponse;
import com.codevision.codevisionbackend.analyze.PiiPciFindingSummary;
import com.codevision.codevisionbackend.api.model.DiagramDescriptor;
import com.codevision.codevisionbackend.api.model.LoggerInsight;
import com.codevision.codevisionbackend.api.model.PiiPciFinding;
import com.codevision.codevisionbackend.api.model.ProjectDiagramsResponse;
import com.codevision.codevisionbackend.api.model.ProjectLoggerInsightsResponse;
import com.codevision.codevisionbackend.api.model.ProjectPiiPciResponse;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

        DbAnalysisSummary dbAnalysis = snapshot.dbAnalysis();
        if (dbAnalysis != null) {
            response.setDbAnalysis(toDbAnalysis(dbAnalysis));
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

        List<LoggerInsightSummary> loggerInsights = snapshot.loggerInsights();
        if (loggerInsights != null) {
            response.setLoggerInsights(loggerInsights.stream()
                    .filter(Objects::nonNull)
                    .map(this::toLoggerInsight)
                    .collect(Collectors.toList()));
        }

        List<PiiPciFindingSummary> piiPciScan = snapshot.piiPciScan();
        if (piiPciScan != null) {
            response.setPiiPciScan(piiPciScan.stream()
                    .filter(Objects::nonNull)
                    .map(this::toPiiPciFinding)
                    .collect(Collectors.toList()));
        }

        if (snapshot.callFlows() != null && !snapshot.callFlows().isEmpty()) {
            response.setCallFlows(snapshot.callFlows());
        }

        List<DiagramSummary> diagrams = snapshot.diagrams();
        if (diagrams != null) {
            response.setDiagrams(diagrams.stream()
                    .filter(Objects::nonNull)
                    .map(this::toDiagramDescriptor)
                    .collect(Collectors.toList()));
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

    private LoggerInsight toLoggerInsight(LoggerInsightSummary summary) {
        return new LoggerInsight()
                .className(summary.className())
                .filePath(summary.filePath())
                .logLevel(summary.logLevel())
                .lineNumber(summary.lineNumber())
                .messageTemplate(summary.messageTemplate())
                .variables(summary.variables())
                .piiRisk(summary.piiRisk())
                .pciRisk(summary.pciRisk());
    }

    private PiiPciFinding toPiiPciFinding(PiiPciFindingSummary summary) {
        return new PiiPciFinding()
                .filePath(summary.filePath())
                .lineNumber(summary.lineNumber())
                .snippet(summary.snippet())
                .matchType(summary.matchType())
                .severity(summary.severity())
                .ignored(summary.ignored());
    }

    private DiagramDescriptor toDiagramDescriptor(DiagramSummary summary) {
        DiagramDescriptor descriptor = new DiagramDescriptor()
                .diagramId(summary.diagramId())
                .diagramType(summary.diagramType())
                .title(summary.title())
                .plantumlSource(summary.plantumlSource())
                .mermaidSource(summary.mermaidSource())
                .metadata(summary.metadata());
        descriptor.setSvgAvailable(summary.svgPath() != null && !summary.svgPath().isBlank());
        return descriptor;
    }

    private DiagramDescriptor toDiagramDescriptor(Long projectId, DiagramSummary summary) {
        DiagramDescriptor descriptor = toDiagramDescriptor(summary);
        if (summary.diagramId() != null && projectId != null && descriptor.getSvgAvailable()) {
            descriptor.setSvgDownloadUrl(String.format("/project/%d/diagram/%d/svg", projectId, summary.diagramId()));
        }
        return descriptor;
    }

    private com.codevision.codevisionbackend.api.model.DbAnalysis toDbAnalysis(DbAnalysisSummary summary) {
        com.codevision.codevisionbackend.api.model.DbAnalysis mapped =
                new com.codevision.codevisionbackend.api.model.DbAnalysis();
        if (summary.entities() != null) {
            mapped.setEntities(summary.entities().stream()
                    .filter(Objects::nonNull)
                    .map(this::toDbEntitySummary)
                    .collect(Collectors.toList()));
        }
        if (summary.classesByEntity() != null) {
            Map<String, List<String>> classesByEntity = summary.classesByEntity().entrySet().stream()
                    .filter(entry -> entry.getKey() != null)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue() == null ? List.of() : List.copyOf(entry.getValue()),
                            (left, right) -> left,
                            LinkedHashMap::new));
            mapped.setClassesByEntity(classesByEntity);
        }
        if (summary.operationsByClass() != null) {
            Map<String, List<com.codevision.codevisionbackend.api.model.DaoOperationDetails>> operationsByClass = summary
                    .operationsByClass()
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getKey() != null)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> (entry.getValue() == null
                                            ? List.<DbAnalysisSummary.DaoOperationDetails>of()
                                            : entry.getValue()).stream()
                                    .filter(Objects::nonNull)
                                    .map(this::toDaoOperationDetails)
                                    .collect(Collectors.toList()),
                            (left, right) -> left,
                            LinkedHashMap::new));
            mapped.setOperationsByClass(operationsByClass);
        }
        return mapped;
    }

    private com.codevision.codevisionbackend.api.model.DbEntitySummary toDbEntitySummary(
            DbAnalysisSummary.DbEntitySummary summary) {
        com.codevision.codevisionbackend.api.model.DbEntitySummary mapped =
                new com.codevision.codevisionbackend.api.model.DbEntitySummary()
                        .entityName(summary.entityName())
                        .fullyQualifiedName(summary.fullyQualifiedName())
                        .tableName(summary.tableName())
                        .primaryKeys(summary.primaryKeys() == null ? List.of() : List.copyOf(summary.primaryKeys()));
        if (summary.fields() != null) {
            mapped.setFields(summary.fields().stream()
                    .filter(Objects::nonNull)
                    .map(this::toDbFieldSummary)
                    .collect(Collectors.toList()));
        }
        if (summary.relationships() != null) {
            mapped.setRelationships(summary.relationships().stream()
                    .filter(Objects::nonNull)
                    .map(this::toDbRelationshipSummary)
                    .collect(Collectors.toList()));
        }
        return mapped;
    }

    private com.codevision.codevisionbackend.api.model.DbFieldSummary toDbFieldSummary(
            DbAnalysisSummary.DbEntitySummary.FieldSummary field) {
        return new com.codevision.codevisionbackend.api.model.DbFieldSummary()
                .name(field.name())
                .type(field.type())
                .columnName(field.columnName());
    }

    private com.codevision.codevisionbackend.api.model.DbRelationshipSummary toDbRelationshipSummary(
            DbAnalysisSummary.DbEntitySummary.RelationshipSummary relationship) {
        return new com.codevision.codevisionbackend.api.model.DbRelationshipSummary()
                .fieldName(relationship.fieldName())
                .targetType(relationship.targetType())
                .relationshipType(relationship.relationshipType());
    }

    private com.codevision.codevisionbackend.api.model.DaoOperationDetails toDaoOperationDetails(
            DbAnalysisSummary.DaoOperationDetails operation) {
        return new com.codevision.codevisionbackend.api.model.DaoOperationDetails()
                .methodName(operation.methodName())
                .operationType(operation.operationType())
                .target(operation.target())
                .querySnippet(operation.querySnippet());
    }

    public com.codevision.codevisionbackend.api.model.ProjectDbAnalysisResponse toDbAnalysisResponse(
            Long projectId, DbAnalysisSummary summary) {
        DbAnalysisSummary safeSummary = summary != null ? summary : new DbAnalysisSummary(List.of(), Map.of(), Map.of());
        return new com.codevision.codevisionbackend.api.model.ProjectDbAnalysisResponse()
                .projectId(projectId)
                .dbAnalysis(toDbAnalysis(safeSummary));
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

    public ProjectLoggerInsightsResponse toLoggerInsightsResponse(
            Long projectId, List<LoggerInsightSummary> summaries) {
        ProjectLoggerInsightsResponse response = new ProjectLoggerInsightsResponse().projectId(projectId);
        if (summaries != null) {
            response.setLoggerInsights(summaries.stream()
                    .filter(Objects::nonNull)
                    .map(this::toLoggerInsight)
                    .collect(Collectors.toList()));
        }
        return response;
    }

    public ProjectPiiPciResponse toPiiPciResponse(Long projectId, List<PiiPciFindingSummary> findings) {
        ProjectPiiPciResponse response = new ProjectPiiPciResponse().projectId(projectId);
        if (findings != null) {
            response.setFindings(findings.stream()
                    .filter(Objects::nonNull)
                    .map(this::toPiiPciFinding)
                    .collect(Collectors.toList()));
        }
        return response;
    }

    public ProjectDiagramsResponse toProjectDiagramsResponse(Long projectId, List<DiagramSummary> diagrams) {
        ProjectDiagramsResponse response = new ProjectDiagramsResponse().projectId(projectId);
        if (diagrams != null) {
            response.setDiagrams(diagrams.stream()
                    .filter(Objects::nonNull)
                    .map(diagram -> toDiagramDescriptor(projectId, diagram))
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
