package com.codevision.codevisionbackend.project;

import com.codevision.codevisionbackend.analyze.AssetInventory;
import com.codevision.codevisionbackend.analyze.BuildInfo;
import com.codevision.codevisionbackend.analyze.ClassMetadataSummary;
import com.codevision.codevisionbackend.analyze.DbAnalysisSummary;
import com.codevision.codevisionbackend.analyze.MetadataDump;
import com.codevision.codevisionbackend.analyze.ParsedDataResponse;
import com.codevision.codevisionbackend.analyze.PiiPciFindingSummary;
import com.codevision.codevisionbackend.project.ProjectSnapshotService.ProjectSnapshotSummary;
import com.codevision.codevisionbackend.project.ProjectSnapshotService.SnapshotMetadata;
import com.codevision.codevisionbackend.project.diagram.DiagramService;
import com.codevision.codevisionbackend.project.metadata.ClassMetadata;
import com.codevision.codevisionbackend.project.metadata.ClassMetadataRepository;
import com.codevision.codevisionbackend.project.security.PiiPciFindingRepository;
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

    @Autowired
    private PiiPciFindingRepository piiPciFindingRepository;

    private ProjectSnapshotService projectSnapshotService;
    private DiagramService diagramService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        diagramService = Mockito.mock(DiagramService.class);
        Mockito.when(diagramService.listProjectDiagrams(Mockito.anyLong())).thenReturn(List.of());
        projectSnapshotService = new ProjectSnapshotService(
                projectSnapshotRepository,
                projectRepository,
                classMetadataRepository,
                diagramService,
                piiPciFindingRepository,
                objectMapper);
    }

    @Test
    void saveSnapshotPersistsNewVersionEachRun() {
        Project project = persistProject("https://example.com/repo.git", "demo-project");
        ParsedDataResponse parsed = sampleParsedData(project, List.of());

        projectSnapshotService.saveSnapshot(project, parsed, new SnapshotMetadata("main", "abc123", Map.of()));
        projectSnapshotService.saveSnapshot(project, parsed, new SnapshotMetadata("main", "def456", Map.of("/", "tree")));

        List<ProjectSnapshot> snapshots = projectSnapshotRepository.findByProjectIdOrderByCreatedAtDesc(project.getId());
        assertThat(snapshots).hasSize(2);
        assertThat(snapshots.get(0).getCommitHash()).isEqualTo("def456");
        assertThat(snapshots.get(1).getCommitHash()).isEqualTo("abc123");
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

        ParsedDataResponse parsed = sampleParsedData(project, List.of());
        projectSnapshotService.saveSnapshot(project, parsed, new SnapshotMetadata("main", "abc", Map.of()));

        ParsedDataResponse fetched = projectSnapshotService
                .fetchSnapshot(project.getId())
                .orElseThrow();
        assertThat(fetched.classes()).hasSize(1);
        assertThat(fetched.classes().get(0).fullyQualifiedName()).isEqualTo("com.example.Controller");
    }

    @Test
    void listSnapshotsReturnsDescendingTimeline() {
        Project project = persistProject("https://example.com/repo.git", "timeline-project");
        ParsedDataResponse parsed = sampleParsedData(project, List.of());
        projectSnapshotService.saveSnapshot(project, parsed, new SnapshotMetadata("main", "one", Map.of()));
        projectSnapshotService.saveSnapshot(project, parsed, new SnapshotMetadata("main", "two", Map.of()));

        List<ProjectSnapshotSummary> summaries = projectSnapshotService.listSnapshots(project.getId());
        assertThat(summaries).hasSize(2);
        assertThat(summaries.get(0).commitHash()).isEqualTo("two");
        assertThat(summaries.get(1).commitHash()).isEqualTo("one");
    }

    @Test
    void diffHighlightsNewClasses() {
        Project project = persistProject("https://example.com/repo.git", "diff-project");
        ParsedDataResponse base = sampleParsedData(project, List.of(new ClassMetadataSummary(
                "com.example.Base",
                "com.example",
                "Base",
                "CONTROLLER",
                true,
                "MAIN",
                "src/Base.java",
                List.of(),
                List.of())));
        ProjectSnapshot baseSnapshot = projectSnapshotService.saveSnapshot(
                project, base, new SnapshotMetadata("main", "commit-1", Map.of()));

        ParsedDataResponse compare = sampleParsedData(project, List.of(new ClassMetadataSummary(
                "com.example.New",
                "com.example",
                "New",
                "SERVICE",
                true,
                "MAIN",
                "src/New.java",
                List.of(),
                List.of())));
        ProjectSnapshot compareSnapshot = projectSnapshotService.saveSnapshot(
                project, compare, new SnapshotMetadata("main", "commit-2", Map.of()));

        SnapshotDiff diff = projectSnapshotService.diff(project.getId(), baseSnapshot.getId(), compareSnapshot.getId());
        assertThat(diff.addedClasses()).extracting(SnapshotDiff.ClassRef::fullyQualifiedName).containsExactly("com.example.New");
        assertThat(diff.removedClasses()).extracting(SnapshotDiff.ClassRef::fullyQualifiedName).containsExactly("com.example.Base");
    }

    private Project persistProject(String repoUrl, String projectName) {
        Project project = new Project(repoUrl, projectName, "main", OffsetDateTime.now());
        return projectRepository.saveAndFlush(project);
    }

    private ParsedDataResponse sampleParsedData(Project project, List<ClassMetadataSummary> classes) {
        return new ParsedDataResponse(
                project.getId(),
                project.getProjectName(),
                project.getRepoUrl(),
                project.getLastAnalyzedAt(),
                BuildInfo.empty(),
                classes,
                MetadataDump.empty(),
                emptyDbAnalysis(),
                List.of(),
                AssetInventory.empty(),
                List.of(),
                List.of(new PiiPciFindingSummary(1L, "src/data.txt", 9, "card=4111", "PCI", "HIGH", false)),
                List.of(),
                Map.of(),
                List.of());
    }

    private DbAnalysisSummary emptyDbAnalysis() {
        return new DbAnalysisSummary(List.of(), Map.of(), Map.of());
    }
}
