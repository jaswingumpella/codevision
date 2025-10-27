package com.codevision.codevisionbackend.project;

import com.codevision.codevisionbackend.analyze.ClassMetadataSummary;
import com.codevision.codevisionbackend.analyze.ParsedDataResponse;
import com.codevision.codevisionbackend.project.metadata.ClassMetadata;
import com.codevision.codevisionbackend.project.metadata.ClassMetadataRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(ProjectSnapshotService.class);

    private final ProjectSnapshotRepository projectSnapshotRepository;
    private final ProjectRepository projectRepository;
    private final ClassMetadataRepository classMetadataRepository;
    private final ObjectMapper objectMapper;
    private static final TypeReference<List<String>> LIST_OF_STRINGS = new TypeReference<>() {};

    public ProjectSnapshotService(
            ProjectSnapshotRepository projectSnapshotRepository,
            ProjectRepository projectRepository,
            ClassMetadataRepository classMetadataRepository,
            ObjectMapper objectMapper) {
        this.projectSnapshotRepository = projectSnapshotRepository;
        this.projectRepository = projectRepository;
        this.classMetadataRepository = classMetadataRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void saveSnapshot(Project project, ParsedDataResponse parsedData) {
        log.debug("Saving snapshot for project candidate id={} repo={}", project.getId(), project.getRepoUrl());
        Project managedProject = resolveProject(project, parsedData);
        String snapshotJson = toJson(parsedData);
        OffsetDateTime now = OffsetDateTime.now();
        Long projectId = managedProject.getId();
        if (projectId == null) {
            throw new IllegalStateException("Resolved project must have an identifier before saving snapshot");
        }
        String projectName = resolveProjectName(managedProject, parsedData);
        String repoUrl = resolveRepoUrl(managedProject, parsedData);

        if (projectName == null) {
            throw new IllegalStateException("Unable to resolve project name for snapshot persistence");
        }
        if (repoUrl == null) {
            throw new IllegalStateException("Unable to resolve repository url for snapshot persistence");
        }

        ProjectSnapshot snapshot = projectSnapshotRepository
                .findById(projectId)
                .orElseGet(() -> new ProjectSnapshot(managedProject, snapshotJson, now));
        snapshot.setProject(managedProject);
        snapshot.setProjectName(projectName);
        snapshot.setRepoUrl(repoUrl);
        snapshot.setSnapshotJson(snapshotJson);
        snapshot.setCreatedAt(now);
        if (snapshot.getProjectId() == null) {
            throw new IllegalStateException("Snapshot missing project identifier before persistence");
        }

        projectSnapshotRepository.saveAndFlush(snapshot);
        log.info("Snapshot persisted for projectId={} at {}", snapshot.getProjectId(), now);
    }

    @Transactional
    public void deleteSnapshot(Project project) {
        Project managedProject = resolveProject(project, null);
        Long projectId = managedProject.getId();
        if (projectId == null) {
            return;
        }
        projectSnapshotRepository
                .findById(projectId)
                .ifPresent(existing -> {
                    projectSnapshotRepository.delete(existing);
                    log.info("Deleted snapshot for projectId={}", projectId);
                });
    }

    private Project resolveProject(Project project, ParsedDataResponse parsedData) {
        if (project == null) {
            throw new IllegalArgumentException("Project cannot be null when saving snapshot");
        }
        if (project.getId() != null) {
            return projectRepository
                    .findById(project.getId())
                    .orElseThrow(() -> new IllegalStateException("Project not found for id " + project.getId()));
        }
        String repoUrl = firstNonBlank(project.getRepoUrl(), parsedData != null ? parsedData.repoUrl() : null);
        if (repoUrl != null) {
            log.debug("Resolving project by repoUrl={}", repoUrl);
            return projectRepository
                    .findByRepoUrl(repoUrl)
                    .orElseThrow(() -> new IllegalStateException("Project not found for repoUrl " + repoUrl));
        }
        String projectName = firstNonBlank(project.getProjectName(), parsedData != null ? parsedData.projectName() : null);
        if (projectName != null) {
            log.debug("Resolving project by projectName={}", projectName);
            return projectRepository
                    .findByProjectName(projectName)
                    .orElseThrow(() -> new IllegalStateException("Project not found for projectName " + projectName));
        }
        throw new IllegalStateException("Unable to resolve project for snapshot persistence");
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return (fallback != null && !fallback.isBlank()) ? fallback : null;
    }

    private String resolveProjectName(Project project, ParsedDataResponse parsedData) {
        return firstNonBlank(project.getProjectName(), parsedData != null ? parsedData.projectName() : null);
    }

    private String resolveRepoUrl(Project project, ParsedDataResponse parsedData) {
        return firstNonBlank(project.getRepoUrl(), parsedData != null ? parsedData.repoUrl() : null);
    }

    @Transactional(readOnly = true)
    public Optional<ParsedDataResponse> fetchSnapshot(Long projectId) {
        return projectSnapshotRepository.findById(projectId).map(snapshot -> {
            log.debug("Hydrating snapshot for projectId={}", projectId);
            return hydrateSnapshot(snapshot);
        });
    }

    private String toJson(ParsedDataResponse data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize snapshot for projectId={}", data != null ? data.projectId() : null, e);
            throw new IllegalStateException("Failed to serialize project snapshot", e);
        }
    }

    private ParsedDataResponse fromJson(ProjectSnapshot snapshot) {
        try {
            return objectMapper.readValue(snapshot.getSnapshotJson(), ParsedDataResponse.class);
        } catch (IOException e) {
            log.error("Failed to deserialize snapshot for projectId={}", snapshot.getProjectId(), e);
            throw new IllegalStateException("Failed to deserialize project snapshot", e);
        }
    }

    private ParsedDataResponse hydrateSnapshot(ProjectSnapshot snapshot) {
        ParsedDataResponse fromSnapshot = fromJson(snapshot);
        if (fromSnapshot.classes() != null && !fromSnapshot.classes().isEmpty()) {
            return fromSnapshot;
        }
        List<ClassMetadataSummary> classSummaries = classMetadataRepository.findByProjectId(snapshot.getProjectId()).stream()
                .map(this::toSummary)
                .toList();
        if (classSummaries.isEmpty()) {
            return fromSnapshot;
        }
        return new ParsedDataResponse(
                fromSnapshot.projectId(),
                Optional.ofNullable(fromSnapshot.projectName()).orElse(snapshot.getProjectName()),
                Optional.ofNullable(fromSnapshot.repoUrl()).orElse(snapshot.getRepoUrl()),
                fromSnapshot.analyzedAt(),
                fromSnapshot.buildInfo(),
                classSummaries,
                fromSnapshot.metadataDump(),
                fromSnapshot.dbAnalysis(),
                fromSnapshot.apiEndpoints(),
                fromSnapshot.assets());
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
}
