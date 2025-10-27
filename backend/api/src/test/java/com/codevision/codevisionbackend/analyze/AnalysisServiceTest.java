package com.codevision.codevisionbackend.analyze;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codevision.codevisionbackend.analyze.BuildInfo;
import com.codevision.codevisionbackend.analyze.MetadataDump;
import com.codevision.codevisionbackend.analyze.ParsedDataResponse;
import com.codevision.codevisionbackend.analyze.scanner.BuildMetadataExtractor;
import com.codevision.codevisionbackend.analyze.scanner.BuildMetadataExtractor.BuildMetadata;
import com.codevision.codevisionbackend.analyze.scanner.ClassMetadataRecord;
import com.codevision.codevisionbackend.analyze.scanner.ClassMetadataRecord.SourceSet;
import com.codevision.codevisionbackend.analyze.scanner.JavaSourceScanner;
import com.codevision.codevisionbackend.analyze.scanner.YamlScanner;
import com.codevision.codevisionbackend.git.GitCloneService.CloneResult;
import com.codevision.codevisionbackend.git.GitCloneService;
import com.codevision.codevisionbackend.project.Project;
import com.codevision.codevisionbackend.project.ProjectService;
import com.codevision.codevisionbackend.project.ProjectSnapshotService;
import com.codevision.codevisionbackend.project.metadata.ClassMetadata;
import com.codevision.codevisionbackend.project.metadata.ClassMetadataRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    @Mock
    private BuildMetadataExtractor buildMetadataExtractor;

    @Mock
    private JavaSourceScanner javaSourceScanner;

    @Mock
    private YamlScanner yamlScanner;

    @Mock
    private ProjectService projectService;

    @Mock
    private ClassMetadataRepository classMetadataRepository;

    @Mock
    private ProjectSnapshotService projectSnapshotService;

    private AnalysisService analysisService;

    @Test
    void analyzePersistsMetadataAndCleansUp(@TempDir Path repoDir) throws Exception {
        Files.createDirectories(repoDir);

        CloneResult cloneResult = new CloneResult("demo-app", repoDir);
        StubGitCloneService stubGitCloneService = new StubGitCloneService(cloneResult);
        analysisService = new AnalysisService(
                stubGitCloneService,
                buildMetadataExtractor,
                javaSourceScanner,
                yamlScanner,
                projectService,
                classMetadataRepository,
                projectSnapshotService,
                new ObjectMapper());

        BuildInfo buildInfo = new BuildInfo("com.barclays", "demo-app", "1.0.0", "21");
        BuildMetadata metadata = new BuildMetadata(buildInfo, List.of(repoDir));

        Project project = new Project("https://example.com/repo.git", "demo-app", OffsetDateTime.now());
        project.setId(101L);

        when(buildMetadataExtractor.extract(repoDir)).thenReturn(metadata);
        when(projectService.findByRepoUrl("https://example.com/repo.git")).thenReturn(Optional.empty());

        List<ClassMetadataRecord> classRecords = List.of(new ClassMetadataRecord(
                "com.barclays.demo.Controller",
                "com.barclays.demo",
                "Controller",
                List.of("RestController"),
                List.of("Serializable"),
                "CONTROLLER",
                SourceSet.MAIN,
                "src/main/java/com/barclays/demo/Controller.java",
                true));
        when(javaSourceScanner.scan(repoDir, metadata.moduleRoots())).thenReturn(classRecords);

        MetadataDump metadataDump = new MetadataDump(List.of());
        when(yamlScanner.scan(repoDir)).thenReturn(metadataDump);

        when(projectService.overwriteProject("https://example.com/repo.git", "demo-app", buildInfo)).thenReturn(project);

        AnalysisOutcome outcome = analysisService.analyze("https://example.com/repo.git");

        assertEquals(project, outcome.project());
        ParsedDataResponse parsedData = outcome.parsedData();
        assertEquals(project.getId(), parsedData.projectId());
        assertEquals(buildInfo, parsedData.buildInfo());
        assertEquals(1, parsedData.classes().size());

        verify(classMetadataRepository).deleteByProject(project);
        verify(classMetadataRepository)
                .saveAll(Mockito.<List<ClassMetadata>>argThat(list -> list.size() == 1
                        && "com.barclays.demo.Controller".equals(list.get(0).getFullyQualifiedName())));

        ArgumentCaptor<ParsedDataResponse> snapshotCaptor = ArgumentCaptor.forClass(ParsedDataResponse.class);
        verify(projectSnapshotService).saveSnapshot(eq(project), snapshotCaptor.capture());
        ParsedDataResponse savedSnapshot = snapshotCaptor.getValue();
        assertEquals(project.getRepoUrl(), savedSnapshot.repoUrl());
        assertNotNull(savedSnapshot.analyzedAt());

        assertTrue(stubGitCloneService.wasCleanupCalled());
    }

    private static class StubGitCloneService extends GitCloneService {

        private final CloneResult cloneResult;
        private boolean cleanupCalled;

        StubGitCloneService(CloneResult cloneResult) {
            super(new com.codevision.codevisionbackend.config.GitAuthProperties());
            this.cloneResult = cloneResult;
        }

        @Override
        public CloneResult cloneRepository(String repoUrl) {
            return cloneResult;
        }

        @Override
        public void cleanupClone(CloneResult cloneResult) {
            if (this.cloneResult.equals(cloneResult)) {
                cleanupCalled = true;
            }
        }

        @Override
        public void cleanupClone(Path directory) {
            if (cloneResult.directory().equals(directory)) {
                cleanupCalled = true;
            }
        }

        boolean wasCleanupCalled() {
            return cleanupCalled;
        }
    }
}
