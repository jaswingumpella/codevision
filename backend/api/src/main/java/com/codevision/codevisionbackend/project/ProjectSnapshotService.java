package com.codevision.codevisionbackend.project;

import static com.codevision.codevisionbackend.git.BranchUtils.normalize;

import com.codevision.codevisionbackend.analyze.ClassMetadataSummary;
import com.codevision.codevisionbackend.analyze.DiagramSummary;
import com.codevision.codevisionbackend.analyze.ParsedDataResponse;
import com.codevision.codevisionbackend.analyze.PiiPciFindingSummary;
import com.codevision.codevisionbackend.project.diagram.Diagram;
import com.codevision.codevisionbackend.project.diagram.DiagramService;
import com.codevision.codevisionbackend.project.metadata.ClassMetadata;
import com.codevision.codevisionbackend.project.metadata.ClassMetadataRepository;
import com.codevision.codevisionbackend.project.security.PiiPciFinding;
import com.codevision.codevisionbackend.project.security.PiiPciFindingRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(ProjectSnapshotService.class);
    private static final TypeReference<List<String>> LIST_OF_STRINGS = new TypeReference<>() {};
    private static final TypeReference<Map<String, String>> MAP_OF_STRINGS = new TypeReference<>() {};

    private final ProjectSnapshotRepository snapshotRepository;
    private final ProjectRepository projectRepository;
    private final ClassMetadataRepository classMetadataRepository;
    private final DiagramService diagramService;
    private final PiiPciFindingRepository piiPciFindingRepository;
    private final ObjectMapper objectMapper;

    public ProjectSnapshotService(
            ProjectSnapshotRepository snapshotRepository,
            ProjectRepository projectRepository,
            ClassMetadataRepository classMetadataRepository,
            DiagramService diagramService,
            PiiPciFindingRepository piiPciFindingRepository,
            ObjectMapper objectMapper) {
        this.snapshotRepository = snapshotRepository;
        this.projectRepository = projectRepository;
        this.classMetadataRepository = classMetadataRepository;
        this.diagramService = diagramService;
        this.piiPciFindingRepository = piiPciFindingRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ProjectSnapshot saveSnapshot(Project project, ParsedDataResponse parsedData, SnapshotMetadata metadata) {
        if (parsedData == null) {
            throw new IllegalArgumentException("Parsed data must be supplied");
        }
        Project managedProject = resolveProject(project);
        String projectName = resolveProjectName(managedProject, parsedData);
        String repoUrl = resolveRepoUrl(managedProject, parsedData);
        if (projectName == null || repoUrl == null) {
            throw new IllegalStateException("Project metadata is incomplete for snapshot persistence");
        }

        ProjectSnapshot snapshot = new ProjectSnapshot();
        snapshot.setProject(managedProject);
        snapshot.setProjectName(projectName);
        snapshot.setRepoUrl(repoUrl);
        snapshot.setBranchName(metadata.branchName());
        snapshot.setCommitHash(metadata.commitHash());
        snapshot.setModuleFingerprintsJson(writeModuleFingerprints(metadata.moduleFingerprints()));
        snapshot.setSnapshotJson(toJson(parsedData));
        snapshot.setCreatedAt(OffsetDateTime.now());

        ProjectSnapshot persisted = snapshotRepository.saveAndFlush(snapshot);
        log.info(
                "Snapshot {} persisted for projectId={} branch={} commit={}",
                persisted.getId(),
                managedProject.getId(),
                metadata.branchName(),
                metadata.commitHash());
        return persisted;
    }

    @Transactional(readOnly = true)
    public Optional<ProjectSnapshot> findLatestSnapshotEntity(Long projectId) {
        if (projectId == null) {
            return Optional.empty();
        }
        return snapshotRepository.findTopByProjectIdOrderByCreatedAtDesc(projectId);
    }

    @Transactional(readOnly = true)
    public Optional<ProjectSnapshot> findLatestSnapshotByCommit(Long projectId, String commitHash) {
        if (projectId == null || commitHash == null || commitHash.isBlank()) {
            return Optional.empty();
        }
        return snapshotRepository.findTopByProjectIdAndCommitHashOrderByCreatedAtDesc(projectId, commitHash);
    }

    @Transactional(readOnly = true)
    public Optional<ParsedDataResponse> fetchSnapshot(Long projectId) {
        return findLatestSnapshotEntity(projectId).map(this::hydrateSnapshot);
    }

    @Transactional(readOnly = true)
    public ParsedDataResponse hydrateSnapshot(ProjectSnapshot snapshot) {
        ParsedDataResponse raw = fromJson(snapshot);
        Long projectId = snapshotProjectId(snapshot);
        List<ClassMetadataSummary> classes = raw.classes();
        if (classes == null || classes.isEmpty()) {
            classes = readClassMetadata(projectId);
        }
        List<PiiPciFindingSummary> piiScan = readPiiFindings(snapshot.getProject());
        ParsedDataResponse enriched = new ParsedDataResponse(
                projectId != null ? projectId : raw.projectId(),
                Optional.ofNullable(raw.projectName()).orElse(snapshot.getProjectName()),
                Optional.ofNullable(raw.repoUrl()).orElse(snapshot.getRepoUrl()),
                raw.analyzedAt(),
                raw.buildInfo(),
                classes,
                raw.metadataDump(),
                raw.dbAnalysis(),
                raw.apiEndpoints(),
                raw.assets(),
                raw.loggerInsights(),
                piiScan.isEmpty() ? raw.piiPciScan() : piiScan,
                raw.gherkinFeatures(),
                raw.callFlows(),
                raw.diagrams());
        return enrichWithDiagrams(snapshot, enriched);
    }

    @Transactional(readOnly = true)
    public List<ProjectSnapshotSummary> listSnapshots(Long projectId) {
        if (projectId == null) {
            return List.of();
        }
        return snapshotRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(snapshot -> new ProjectSnapshotSummary(
                        snapshot.getId(),
                        snapshot.getBranchName(),
                        snapshot.getCommitHash(),
                        snapshot.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public SnapshotDiff diff(Long projectId, Long baseSnapshotId, Long compareSnapshotId) {
        if (projectId == null || baseSnapshotId == null || compareSnapshotId == null) {
            throw new IllegalArgumentException("Project and snapshot identifiers are required");
        }
        ProjectSnapshot base = snapshotRepository
                .findByIdAndProjectId(baseSnapshotId, projectId)
                .orElseThrow(() -> new IllegalArgumentException("Snapshot " + baseSnapshotId + " not found for project " + projectId));
        ProjectSnapshot compare = snapshotRepository
                .findByIdAndProjectId(compareSnapshotId, projectId)
                .orElseThrow(() -> new IllegalArgumentException("Snapshot " + compareSnapshotId + " not found for project " + projectId));

        ParsedDataResponse baseData = hydrateSnapshot(base);
        ParsedDataResponse compareData = hydrateSnapshot(compare);
        return buildDiff(base, compare, baseData, compareData);
    }

    @Transactional(readOnly = true)
    public Map<String, String> readModuleFingerprints(ProjectSnapshot snapshot) {
        String payload = snapshot.getModuleFingerprintsJson();
        if (payload == null || payload.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, String> parsed = objectMapper.readValue(payload, MAP_OF_STRINGS);
            return parsed == null ? Map.of() : parsed;
        } catch (IOException e) {
            log.warn("Failed to parse module fingerprints for snapshot {}", snapshot.getId(), e);
            return Map.of();
        }
    }

    private Project resolveProject(Project project) {
        if (project == null) {
            throw new IllegalArgumentException("Project cannot be null when saving snapshot");
        }
        if (project.getId() != null) {
            return projectRepository
                    .findById(project.getId())
                    .orElseThrow(() -> new IllegalStateException("Project not found for id " + project.getId()));
        }
        if (project.getRepoUrl() != null && project.getBranchName() != null) {
            return projectRepository
                    .findByRepoUrlAndBranchName(project.getRepoUrl(), project.getBranchName())
                    .orElseThrow(() -> new IllegalStateException(
                            "Project not found for repo " + project.getRepoUrl() + " branch " + project.getBranchName()));
        }
        throw new IllegalStateException("Unable to resolve project for snapshot persistence");
    }

    private String resolveProjectName(Project project, ParsedDataResponse parsedData) {
        return firstNonBlank(project.getProjectName(), parsedData.projectName());
    }

    private String resolveRepoUrl(Project project, ParsedDataResponse parsedData) {
        return firstNonBlank(project.getRepoUrl(), parsedData.repoUrl());
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return (fallback != null && !fallback.isBlank()) ? fallback : null;
    }

    private String toJson(ParsedDataResponse data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize project snapshot", e);
        }
    }

    private ParsedDataResponse fromJson(ProjectSnapshot snapshot) {
        try {
            return objectMapper.readValue(snapshot.getSnapshotJson(), ParsedDataResponse.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to deserialize snapshot " + snapshot.getId(), e);
        }
    }

    private String writeModuleFingerprints(Map<String, String> fingerprints) {
        if (fingerprints == null || fingerprints.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(fingerprints);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize module fingerprint map", e);
        }
    }

    private ParsedDataResponse enrichWithDiagrams(ProjectSnapshot snapshot, ParsedDataResponse data) {
        List<DiagramSummary> diagrams = data.diagrams();
        if (diagrams != null && !diagrams.isEmpty()) {
            return data;
        }
        List<Diagram> persisted = diagramService.listProjectDiagrams(snapshot.getProjectId());
        if (persisted.isEmpty()) {
            return data;
        }
        List<DiagramSummary> summaries = persisted.stream()
                .map(diagramService::toSummary)
                .filter(Objects::nonNull)
                .toList();
        return new ParsedDataResponse(
                data.projectId(),
                data.projectName(),
                data.repoUrl(),
                data.analyzedAt(),
                data.buildInfo(),
                data.classes(),
                data.metadataDump(),
                data.dbAnalysis(),
                data.apiEndpoints(),
                data.assets(),
                data.loggerInsights(),
                data.piiPciScan(),
                data.gherkinFeatures(),
                data.callFlows(),
                summaries);
    }

    private List<ClassMetadataSummary> readClassMetadata(Long projectId) {
        if (projectId == null) {
            return List.of();
        }
        return classMetadataRepository.findByProjectId(projectId).stream()
                .map(this::toSummary)
                .toList();
    }

    private ClassMetadataSummary toSummary(ClassMetadata metadata) {
        return new ClassMetadataSummary(
                metadata.getFullyQualifiedName(),
                metadata.getPackageName(),
                metadata.getClassName(),
                metadata.getStereotype(),
                metadata.isUserCode(),
                metadata.getSourceSet(),
                metadata.getRelativePath(),
                readStringList(metadata.getAnnotationsJson()),
                readStringList(metadata.getInterfacesJson()));
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, LIST_OF_STRINGS);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to deserialize metadata collection", e);
        }
    }

    private List<PiiPciFindingSummary> readPiiFindings(Project project) {
        if (project == null || project.getId() == null) {
            return List.of();
        }
        return piiPciFindingRepository.findByProject(project).stream()
                .map(finding -> new PiiPciFindingSummary(
                        finding.getId(),
                        finding.getFilePath(),
                        finding.getLineNumber(),
                        finding.getSnippet(),
                        finding.getMatchType(),
                        finding.getSeverity(),
                        finding.isIgnored()))
                .toList();
    }

    private SnapshotDiff buildDiff(
            ProjectSnapshot baseSnapshot,
            ProjectSnapshot compareSnapshot,
            ParsedDataResponse base,
            ParsedDataResponse compare) {
        List<SnapshotDiff.ClassRef> addedClasses = diffClasses(base, compare, true);
        List<SnapshotDiff.ClassRef> removedClasses = diffClasses(base, compare, false);
        List<SnapshotDiff.EndpointRef> addedEndpoints = diffEndpoints(base, compare, true);
        List<SnapshotDiff.EndpointRef> removedEndpoints = diffEndpoints(base, compare, false);
        List<SnapshotDiff.DbEntityRef> addedEntities = diffEntities(base, compare, true);
        List<SnapshotDiff.DbEntityRef> removedEntities = diffEntities(base, compare, false);

        return new SnapshotDiff(
                baseSnapshot.getId(),
                compareSnapshot.getId(),
                Optional.ofNullable(baseSnapshot.getCommitHash()).orElse(""),
                Optional.ofNullable(compareSnapshot.getCommitHash()).orElse(""),
                addedClasses,
                removedClasses,
                addedEndpoints,
                removedEndpoints,
                addedEntities,
                removedEntities);
    }

    private List<SnapshotDiff.ClassRef> diffClasses(ParsedDataResponse base, ParsedDataResponse compare, boolean additions) {
        Map<String, ClassMetadataSummary> baseMap = (base.classes() == null ? List.<ClassMetadataSummary>of() : base.classes()).stream()
                .collect(Collectors.toMap(ClassMetadataSummary::fullyQualifiedName, summary -> summary, (left, right) -> left));
        Map<String, ClassMetadataSummary> compareMap = (compare.classes() == null ? List.<ClassMetadataSummary>of() : compare.classes()).stream()
                .collect(Collectors.toMap(ClassMetadataSummary::fullyQualifiedName, summary -> summary, (left, right) -> left));
        return additions
                ? compareMap.entrySet().stream()
                        .filter(entry -> !baseMap.containsKey(entry.getKey()))
                        .map(entry -> new SnapshotDiff.ClassRef(entry.getKey(), entry.getValue().stereotype()))
                        .toList()
                : baseMap.entrySet().stream()
                        .filter(entry -> !compareMap.containsKey(entry.getKey()))
                        .map(entry -> new SnapshotDiff.ClassRef(entry.getKey(), entry.getValue().stereotype()))
                        .toList();
    }

    private List<SnapshotDiff.EndpointRef> diffEndpoints(ParsedDataResponse base, ParsedDataResponse compare, boolean additions) {
        Map<String, SnapshotDiff.EndpointRef> baseMap = toEndpointMap(base);
        Map<String, SnapshotDiff.EndpointRef> compareMap = toEndpointMap(compare);
        return additions
                ? compareMap.keySet().stream()
                        .filter(key -> !baseMap.containsKey(key))
                        .map(compareMap::get)
                        .toList()
                : baseMap.keySet().stream()
                        .filter(key -> !compareMap.containsKey(key))
                        .map(baseMap::get)
                        .toList();
    }

    private Map<String, SnapshotDiff.EndpointRef> toEndpointMap(ParsedDataResponse response) {
        if (response.apiEndpoints() == null) {
            return Map.of();
        }
        return response.apiEndpoints().stream()
                .map(endpoint -> new SnapshotDiff.EndpointRef(
                        endpoint.protocol(), endpoint.httpMethod(), endpoint.pathOrOperation()))
                .collect(Collectors.toMap(
                        SnapshotDiff.EndpointRef::identity,
                        ref -> ref,
                        (left, right) -> left));
    }

    private List<SnapshotDiff.DbEntityRef> diffEntities(ParsedDataResponse base, ParsedDataResponse compare, boolean additions) {
        Map<String, SnapshotDiff.DbEntityRef> baseEntities = toEntityMap(base);
        Map<String, SnapshotDiff.DbEntityRef> compareEntities = toEntityMap(compare);
        return additions
                ? compareEntities.keySet().stream()
                        .filter(key -> !baseEntities.containsKey(key))
                        .map(compareEntities::get)
                        .toList()
                : baseEntities.keySet().stream()
                        .filter(key -> !compareEntities.containsKey(key))
                        .map(baseEntities::get)
                        .toList();
    }

    private Map<String, SnapshotDiff.DbEntityRef> toEntityMap(ParsedDataResponse response) {
        if (response.dbAnalysis() == null || response.dbAnalysis().entities() == null) {
            return Map.of();
        }
        return response.dbAnalysis().entities().stream()
                .map(entity -> new SnapshotDiff.DbEntityRef(entity.entityName(), entity.tableName()))
                .collect(Collectors.toMap(
                        SnapshotDiff.DbEntityRef::identity,
                        ref -> ref,
                        (left, right) -> left));
    }

    private Long snapshotProjectId(ProjectSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        if (snapshot.getProjectId() != null) {
            return snapshot.getProjectId();
        }
        Project project = snapshot.getProject();
        return project != null ? project.getId() : null;
    }

    public record SnapshotMetadata(String branchName, String commitHash, Map<String, String> moduleFingerprints) {
        public SnapshotMetadata {
            branchName = normalize(branchName);
            commitHash = commitHash == null ? "" : commitHash;
            moduleFingerprints = moduleFingerprints == null ? Map.of() : Map.copyOf(moduleFingerprints);
        }
    }

    public record ProjectSnapshotSummary(Long snapshotId, String branchName, String commitHash, OffsetDateTime createdAt) {}
}
