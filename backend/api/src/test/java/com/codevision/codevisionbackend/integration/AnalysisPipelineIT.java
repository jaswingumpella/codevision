package com.codevision.codevisionbackend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.codevision.codevisionbackend.analysis.CompiledAnalysisService;
import com.codevision.codevisionbackend.analysis.ExportedFile;
import com.codevision.codevisionbackend.analysis.persistence.AnalysisEntityRepository;
import com.codevision.codevisionbackend.analysis.persistence.CompiledAnalysisRun;
import com.codevision.codevisionbackend.analysis.persistence.CompiledAnalysisRunRepository;
import com.codevision.codevisionbackend.analysis.persistence.CompiledAnalysisRunStatus;
import com.codevision.codevisionbackend.analysis.persistence.CompiledEndpointRecord;
import com.codevision.codevisionbackend.analysis.persistence.CompiledEndpointRepository;
import com.codevision.codevisionbackend.analysis.persistence.EntitySequenceUsageRepository;
import com.codevision.codevisionbackend.analysis.persistence.SequenceRecord;
import com.codevision.codevisionbackend.analysis.persistence.SequenceRecordRepository;
import com.codevision.codevisionbackend.analyze.AnalysisOutcome;
import com.codevision.codevisionbackend.analyze.AnalysisService;
import com.codevision.codevisionbackend.analyze.ParsedDataResponse;
import com.codevision.codevisionbackend.git.GitCloneService;
import com.codevision.codevisionbackend.git.GitCloneService.CloneResult;
import com.codevision.codevisionbackend.project.ProjectSnapshot;
import com.codevision.codevisionbackend.project.ProjectSnapshotRepository;
import com.codevision.codevisionbackend.project.ProjectSnapshotService;
import com.codevision.codevisionbackend.project.api.ApiEndpoint;
import com.codevision.codevisionbackend.project.api.ApiEndpointRepository;
import com.codevision.codevisionbackend.project.diagram.DiagramRepository;
import com.codevision.codevisionbackend.project.metadata.ClassMetadata;
import com.codevision.codevisionbackend.project.metadata.ClassMetadataRepository;
import java.nio.file.Path;
import java.util.List;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AnalysisPipelineIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private ProjectSnapshotService projectSnapshotService;

    @Autowired
    private ProjectSnapshotRepository projectSnapshotRepository;

    @Autowired
    private ClassMetadataRepository classMetadataRepository;

    @Autowired
    private ApiEndpointRepository apiEndpointRepository;

    @Autowired
    private DiagramRepository diagramRepository;

    @Autowired
    private CompiledAnalysisRunRepository compiledAnalysisRunRepository;

    @Autowired
    private CompiledAnalysisService compiledAnalysisService;

    @Autowired
    private AnalysisEntityRepository analysisEntityRepository;

    @Autowired
    private SequenceRecordRepository sequenceRecordRepository;

    @Autowired
    private EntitySequenceUsageRepository entitySequenceUsageRepository;

    @Autowired
    private CompiledEndpointRepository compiledEndpointRepository;

    @MockBean
    private GitCloneService gitCloneService;

    @Test
    void analyzeRepositoryPersistsSnapshotAndCompiledArtifacts(@TempDir Path workspace) throws Exception {
        Path fixtureRoot = copyCompiledFixture(workspace);
        String commitHash = initializeGitRepo(fixtureRoot);
        CloneResult cloneResult = new CloneResult("compiled-app", fixtureRoot, "main", commitHash);
        when(gitCloneService.cloneRepository(anyString(), anyString())).thenReturn(cloneResult);

        AnalysisOutcome outcome = analysisService.analyze("https://git.local/compiled-app.git", "main");

        Long projectId = outcome.project().getId();
        assertThat(projectId).as("project persisted").isNotNull();
        assertThat(outcome.commitHash()).isEqualTo(commitHash);
        assertThat(outcome.branchName()).isEqualTo("main");

        ProjectSnapshot snapshot = projectSnapshotRepository
                .findTopByProjectIdOrderByCreatedAtDesc(projectId)
                .orElseThrow();
        assertThat(snapshot.getCommitHash()).isEqualTo(commitHash);
        assertThat(projectSnapshotService.readModuleFingerprints(snapshot)).containsKey("/");

        ParsedDataResponse hydrated = projectSnapshotService.hydrateSnapshot(snapshot);
        assertThat(hydrated.classes()).isNotEmpty();
        assertThat(projectSnapshotService.listSnapshots(projectId)).hasSize(1);

        List<ClassMetadata> metadata = classMetadataRepository.findByProjectId(projectId);
        assertThat(metadata)
                .extracting(ClassMetadata::getFullyQualifiedName)
                .contains("com.codevision.fixtures.controller.FixtureController");

        List<ApiEndpoint> endpoints = apiEndpointRepository.findByProjectId(projectId);
        assertThat(endpoints)
                .extracting(ApiEndpoint::getPathOrOperation)
                .contains("/fixtures", "/fixtures/{id}");

        assertThat(diagramRepository.findByProjectIdOrderByDiagramTypeAscSequenceOrderAscTitleAsc(projectId))
                .isNotEmpty();

        CompiledAnalysisRun run = compiledAnalysisRunRepository
                .findTopByProjectIdOrderByStartedAtDesc(projectId)
                .orElseThrow();
        assertThat(run.getStatus()).isEqualTo(CompiledAnalysisRunStatus.SUCCEEDED);
        Path outputDir = Path.of(run.getOutputDirectory());
        assertThat(outputDir).exists();
        assertThat(outputDir.resolve("analysis.json")).exists();

        List<ExportedFile> exports = compiledAnalysisService.listExports(run.getId());
        assertThat(exports)
                .extracting(ExportedFile::name)
                .contains("analysis.json", "entities.csv", "endpoints.csv", "dependencies.csv");

        assertThat(analysisEntityRepository.findByClassName("com.codevision.fixtures.domain.FixtureEntity"))
                .as("compiled entity persisted")
                .isNotNull();
        assertThat(sequenceRecordRepository.findAll())
                .extracting(SequenceRecord::getGeneratorName)
                .contains("fixture_seq");
        assertThat(entitySequenceUsageRepository.count()).isGreaterThan(0);
        assertThat(compiledEndpointRepository.findAll())
                .extracting(CompiledEndpointRecord::getType)
                .extracting(Enum::name)
                .contains("HTTP", "KAFKA", "SCHEDULED");
    }

    private String initializeGitRepo(Path repoRoot) throws Exception {
        try (Git git = Git.init().setDirectory(repoRoot.toFile()).call()) {
            git.add().addFilepattern(".").call();
            git.commit()
                    .setMessage("initial fixture commit")
                    .setAuthor("CodeVision", "it@codevision.dev")
                    .call();
            return git.getRepository().resolve(Constants.HEAD).name();
        }
    }
}
