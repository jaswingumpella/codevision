package com.codevision.codevisionbackend.analyze;

import com.codevision.codevisionbackend.analyze.scanner.BuildMetadataExtractor;
import com.codevision.codevisionbackend.analyze.scanner.BuildMetadataExtractor.BuildMetadata;
import com.codevision.codevisionbackend.analyze.scanner.ClassMetadataRecord;
import com.codevision.codevisionbackend.analyze.scanner.JavaSourceScanner;
import com.codevision.codevisionbackend.analyze.scanner.YamlScanner;
import com.codevision.codevisionbackend.git.GitCloneService;
import com.codevision.codevisionbackend.project.Project;
import com.codevision.codevisionbackend.project.ProjectService;
import com.codevision.codevisionbackend.project.ProjectSnapshotService;
import com.codevision.codevisionbackend.project.metadata.ClassMetadata;
import com.codevision.codevisionbackend.project.metadata.ClassMetadataRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    private final GitCloneService gitCloneService;
    private final BuildMetadataExtractor buildMetadataExtractor;
    private final JavaSourceScanner javaSourceScanner;
    private final YamlScanner yamlScanner;
    private final ProjectService projectService;
    private final ClassMetadataRepository classMetadataRepository;
    private final ProjectSnapshotService projectSnapshotService;
    private final ObjectMapper objectMapper;

    public AnalysisService(
            GitCloneService gitCloneService,
            BuildMetadataExtractor buildMetadataExtractor,
            JavaSourceScanner javaSourceScanner,
            YamlScanner yamlScanner,
            ProjectService projectService,
            ClassMetadataRepository classMetadataRepository,
            ProjectSnapshotService projectSnapshotService,
            ObjectMapper objectMapper) {
        this.gitCloneService = gitCloneService;
        this.buildMetadataExtractor = buildMetadataExtractor;
        this.javaSourceScanner = javaSourceScanner;
        this.yamlScanner = yamlScanner;
        this.projectService = projectService;
        this.classMetadataRepository = classMetadataRepository;
        this.projectSnapshotService = projectSnapshotService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AnalysisOutcome analyze(String repoUrl) {
        log.info("Starting analysis for {}", repoUrl);
        purgeExistingData(repoUrl);
        GitCloneService.CloneResult cloneResult = gitCloneService.cloneRepository(repoUrl);
        try {
            log.debug("Repository {} cloned to {}", repoUrl, cloneResult.directory());
            BuildMetadata buildMetadata = buildMetadataExtractor.extract(cloneResult.directory());
            log.info(
                    "Extracted build metadata for {} (groupId={}, artifactId={}, version={})",
                    repoUrl,
                    buildMetadata.buildInfo().groupId(),
                    buildMetadata.buildInfo().artifactId(),
                    buildMetadata.buildInfo().version());
            Project persistedProject =
                    projectService.overwriteProject(repoUrl, cloneResult.projectName(), buildMetadata.buildInfo());
            if (persistedProject.getId() == null) {
                persistedProject = projectService
                        .findByRepoUrl(repoUrl)
                        .orElseThrow(() -> new IllegalStateException("Project missing after save for repo: " + repoUrl));
            }
            List<ClassMetadataRecord> classRecords =
                    javaSourceScanner.scan(cloneResult.directory(), buildMetadata.moduleRoots());
            log.info("Discovered {} class metadata records for {}", classRecords.size(), repoUrl);
            replaceClassMetadata(persistedProject, classRecords);
            MetadataDump metadataDump = yamlScanner.scan(cloneResult.directory());
            log.debug(
                    "Collected {} OpenAPI specs for {}",
                    metadataDump.openApiSpecs() != null ? metadataDump.openApiSpecs().size() : 0,
                    repoUrl);
            ParsedDataResponse parsedData =
                    assembleParsedData(persistedProject, buildMetadata.buildInfo(), classRecords, metadataDump);
            projectSnapshotService.saveSnapshot(persistedProject, parsedData);
            log.info(
                    "Completed analysis for {} with projectId={}", repoUrl, persistedProject.getId());
            return new AnalysisOutcome(persistedProject, parsedData);
        } finally {
            cleanupClone(cloneResult);
        }
    }

    private void replaceClassMetadata(Project project, List<ClassMetadataRecord> classRecords) {
        classMetadataRepository.deleteByProject(project);
        log.debug("Cleared existing class metadata for projectId={}", project.getId());
        if (classRecords.isEmpty()) {
            log.info("No class metadata records to persist for projectId={}", project.getId());
            return;
        }
        List<ClassMetadata> entities = classRecords.stream()
                .map(record -> mapToEntity(project, record))
                .collect(Collectors.toList());
        classMetadataRepository.saveAll(entities);
        log.info("Persisted {} class metadata records for projectId={}", entities.size(), project.getId());
    }

    private ClassMetadata mapToEntity(Project project, ClassMetadataRecord record) {
        ClassMetadata entity = new ClassMetadata();
        entity.setProject(project);
        entity.setFullyQualifiedName(record.fullyQualifiedName());
        entity.setPackageName(record.packageName());
        entity.setClassName(record.className());
        entity.setStereotype(record.stereotype());
        entity.setSourceSet(record.sourceSet().name());
        entity.setRelativePath(record.relativePath());
        entity.setUserCode(record.userCode());
        entity.setAnnotationsJson(writeJson(record.annotations()));
        entity.setInterfacesJson(writeJson(record.implementedInterfaces()));
        return entity;
    }

    private ParsedDataResponse assembleParsedData(
            Project project,
            BuildInfo buildInfo,
            List<ClassMetadataRecord> classRecords,
            MetadataDump metadataDump) {
        List<ClassMetadataSummary> classSummaries = classRecords.stream()
                .map(record -> new ClassMetadataSummary(
                        record.fullyQualifiedName(),
                        record.packageName(),
                        record.className(),
                        record.stereotype(),
                        record.userCode(),
                        record.sourceSet().name(),
                        record.relativePath(),
                        record.annotations(),
                        record.implementedInterfaces()))
                .toList();

        return new ParsedDataResponse(
                project.getId(),
                project.getProjectName(),
                project.getRepoUrl(),
                project.getLastAnalyzedAt(),
                buildInfo,
                classSummaries,
                metadataDump);
    }

    private String writeJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize metadata values", e);
        }
    }

    private void cleanupClone(GitCloneService.CloneResult cloneResult) {
        try {
            gitCloneService.cleanupClone(cloneResult);
        } catch (Exception e) {
            log.warn("Failed to cleanup temporary clone for {}", cloneResult.projectName(), e);
        }
    }

    private void purgeExistingData(String repoUrl) {
        projectService.findByRepoUrl(repoUrl).ifPresent(existing -> {
            log.info("Purging existing project data for repo {} (projectId={})", repoUrl, existing.getId());
            projectSnapshotService.deleteSnapshot(existing);
            classMetadataRepository.deleteByProject(existing);
            projectService.delete(existing);
        });
    }
}
