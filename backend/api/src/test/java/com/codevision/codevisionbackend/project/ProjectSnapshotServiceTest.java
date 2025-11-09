package com.codevision.codevisionbackend.project;

import com.codevision.codevisionbackend.analyze.AssetInventory;
import com.codevision.codevisionbackend.analyze.BuildInfo;
import com.codevision.codevisionbackend.analyze.DbAnalysisSummary;
import com.codevision.codevisionbackend.analyze.LoggerInsightSummary;
import com.codevision.codevisionbackend.analyze.MetadataDump;
import com.codevision.codevisionbackend.analyze.ParsedDataResponse;
import com.codevision.codevisionbackend.analyze.PiiPciFindingSummary;
import com.codevision.codevisionbackend.project.diagram.DiagramService;
import com.codevision.codevisionbackend.project.metadata.ClassMetadata;
import com.codevision.codevisionbackend.project.metadata.ClassMetadataRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Sql(scripts = "classpath:schema-h2.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ProjectSnapshotServiceTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectSnapshotRepository projectSnapshotRepository;

    @Autowired
    private ClassMetadataRepository classMetadataRepository;

    private ProjectSnapshotService projectSnapshotService;
    private DiagramService diagramService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        diagramService = Mockito.mock(DiagramService.class);
        Mockito.when(diagramService.listProjectDiagrams(Mockito.any())).thenReturn(List.of());
        projectSnapshotService = new ProjectSnapshotService(
                projectSnapshotRepository, projectRepository, classMetadataRepository, diagramService, objectMapper);
    }

    @Test
    void saveSnapshotPersistsSnapshotForManagedProject() {
        Project project = persistProject("https://example.com/repo.git", "demo-project");
        assertThat(project.getId()).isNotNull();
        ParsedDataResponse parsed = sampleParsedData(project);

        projectSnapshotService.saveSnapshot(project, parsed);
        projectSnapshotService.saveSnapshot(project, parsed);

        assertThat(projectSnapshotRepository.count()).isEqualTo(1);
        ProjectSnapshot snapshot = projectSnapshotRepository.findById(project.getId()).orElseThrow();
        assertThat(snapshot.getProjectId()).isEqualTo(project.getId());
        assertThat(snapshot.getProjectName()).isEqualTo(project.getProjectName());
        assertThat(snapshot.getRepoUrl()).isEqualTo(project.getRepoUrl());
        assertThat(snapshot.getSnapshotJson()).isNotBlank();
        assertThat(snapshot.getCreatedAt()).isNotNull();
    }

    @Test
    void saveSnapshotResolvesDetachedProjectByRepoUrl() {
        Project persisted = persistProject("https://example.com/repo.git", "demo-project");
        Project detached = new Project();
        detached.setRepoUrl(persisted.getRepoUrl());
        detached.setProjectName("detached-name");

        ParsedDataResponse parsed = new ParsedDataResponse(
                null,
                "detached-name",
                persisted.getRepoUrl(),
                OffsetDateTime.now(),
                BuildInfo.empty(),
                List.of(),
                MetadataDump.empty(),
                emptyDbAnalysis(),
                List.of(),
                AssetInventory.empty(),
                List.of(),
                List.of(),
                List.of(),
                Map.of(),
                List.of());

        projectSnapshotService.saveSnapshot(detached, parsed);

        ProjectSnapshot snapshot = projectSnapshotRepository.findById(persisted.getId()).orElseThrow();
        assertThat(snapshot.getProjectId()).isEqualTo(persisted.getId());
        assertThat(snapshot.getProjectName()).isEqualTo(persisted.getProjectName());
        assertThat(snapshot.getRepoUrl()).isEqualTo(persisted.getRepoUrl());
    }

    @Test
    void deleteSnapshotRemovesExistingEntry() {
        Project project = persistProject("https://example.com/repo.git", "demo-project");
        projectSnapshotService.saveSnapshot(project, sampleParsedData(project));
        assertThat(projectSnapshotRepository.count()).isEqualTo(1);

        projectSnapshotService.deleteSnapshot(project);

        assertThat(projectSnapshotRepository.count()).isZero();
    }

    @Test
    void directRepositoryInsertAssignsProjectId() {
        Project project = persistProject("https://example.com/repo.git", "manual-project");
        ProjectSnapshot snapshot = new ProjectSnapshot(project, "{}", OffsetDateTime.now());
        snapshot.setProjectName(project.getProjectName());
        snapshot.setRepoUrl(project.getRepoUrl());

        projectSnapshotRepository.saveAndFlush(snapshot);

        ProjectSnapshot persisted = projectSnapshotRepository.findById(project.getId()).orElseThrow();
        assertThat(persisted.getProjectId()).isEqualTo(project.getId());
        assertThat(persisted.getProjectName()).isEqualTo(project.getProjectName());
        assertThat(persisted.getRepoUrl()).isEqualTo(project.getRepoUrl());
    }

    @Test
    void fetchSnapshotEnrichesClassesFromStoredMetadata() {
        Project project = persistProject("https://example.com/repo.git", "demo-project");

        ClassMetadata metadata = new ClassMetadata();
        metadata.setProject(project);
        metadata.setFullyQualifiedName("com.example.Controller");
        metadata.setPackageName("com.example");
        metadata.setClassName("Controller");
        metadata.setStereotype("CONTROLLER");
        metadata.setSourceSet("MAIN");
        metadata.setRelativePath("src/main/java/com/example/Controller.java");
        metadata.setUserCode(true);
        metadata.setAnnotationsJson("[]");
        metadata.setInterfacesJson("[]");
        classMetadataRepository.saveAndFlush(metadata);

        projectSnapshotService.saveSnapshot(project, sampleParsedData(project));

        ParsedDataResponse fetched =
                projectSnapshotService.fetchSnapshot(project.getId()).orElseThrow();
        assertThat(fetched.classes()).hasSize(1);
        assertThat(fetched.classes().get(0).fullyQualifiedName()).isEqualTo("com.example.Controller");
    }

    private Project persistProject(String repoUrl, String projectName) {
        Project project = new Project(repoUrl, projectName, OffsetDateTime.now());
        Project saved = projectRepository.saveAndFlush(project);
        assertThat(saved.getId()).isNotNull();
        return saved;
    }

    private ParsedDataResponse sampleParsedData(Project project) {
        return new ParsedDataResponse(
                project.getId(),
                project.getProjectName(),
                project.getRepoUrl(),
                project.getLastAnalyzedAt(),
                BuildInfo.empty(),
                List.of(),
                MetadataDump.empty(),
                emptyDbAnalysis(),
                List.of(),
                AssetInventory.empty(),
                List.of(),
                List.of(),
                List.of(),
                Map.of(),
                List.of());
    }

    private DbAnalysisSummary emptyDbAnalysis() {
        return new DbAnalysisSummary(List.of(), Map.of(), Map.of());
    }
}
