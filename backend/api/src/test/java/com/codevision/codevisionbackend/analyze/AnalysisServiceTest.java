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
import com.codevision.codevisionbackend.analyze.diagram.DiagramBuilderService;
import com.codevision.codevisionbackend.analyze.diagram.DiagramGenerationResult;
import com.codevision.codevisionbackend.analyze.scanner.ApiEndpointRecord;
import com.codevision.codevisionbackend.analyze.scanner.ApiScanner;
import com.codevision.codevisionbackend.analyze.scanner.AssetScanner;
import com.codevision.codevisionbackend.analyze.scanner.BuildMetadataExtractor;
import com.codevision.codevisionbackend.analyze.scanner.BuildMetadataExtractor.BuildMetadata;
import com.codevision.codevisionbackend.analyze.scanner.ClassMetadataRecord;
import com.codevision.codevisionbackend.analyze.scanner.ClassMetadataRecord.SourceSet;
import com.codevision.codevisionbackend.analyze.scanner.DaoAnalysisService;
import com.codevision.codevisionbackend.analyze.scanner.DaoOperationRecord;
import com.codevision.codevisionbackend.analyze.scanner.DbAnalysisResult;
import com.codevision.codevisionbackend.analyze.scanner.DbEntityRecord;
import com.codevision.codevisionbackend.analyze.scanner.ImageAssetRecord;
import com.codevision.codevisionbackend.analyze.scanner.JavaSourceScanner;
import com.codevision.codevisionbackend.analyze.scanner.JpaEntityScanner;
import com.codevision.codevisionbackend.analyze.scanner.LogStatementRecord;
import com.codevision.codevisionbackend.analyze.scanner.LoggerScanner;
import com.codevision.codevisionbackend.analyze.scanner.GherkinScanner;
import com.codevision.codevisionbackend.analyze.scanner.PiiPciFindingRecord;
import com.codevision.codevisionbackend.analyze.scanner.PiiPciInspector;
import com.codevision.codevisionbackend.analyze.scanner.YamlScanner;
import com.codevision.codevisionbackend.git.GitCloneService.CloneResult;
import com.codevision.codevisionbackend.git.GitCloneService;
import com.codevision.codevisionbackend.project.Project;
import com.codevision.codevisionbackend.project.ProjectService;
import com.codevision.codevisionbackend.project.ProjectSnapshotService;
import com.codevision.codevisionbackend.project.api.ApiEndpointRepository;
import com.codevision.codevisionbackend.project.asset.AssetImageRepository;
import com.codevision.codevisionbackend.project.db.DaoOperation;
import com.codevision.codevisionbackend.project.db.DaoOperationRepository;
import com.codevision.codevisionbackend.project.db.DbEntity;
import com.codevision.codevisionbackend.project.db.DbEntityRepository;
import com.codevision.codevisionbackend.project.metadata.ClassMetadata;
import com.codevision.codevisionbackend.project.metadata.ClassMetadataRepository;
import com.codevision.codevisionbackend.project.logger.LogStatementRepository;
import com.codevision.codevisionbackend.project.security.PiiPciFindingRepository;
import com.codevision.codevisionbackend.project.diagram.DiagramService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
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
    private ApiScanner apiScanner;

    @Mock
    private AssetScanner assetScanner;

    @Mock
    private JpaEntityScanner jpaEntityScanner;

    @Mock
    private DaoAnalysisService daoAnalysisService;

    @Mock
    private LoggerScanner loggerScanner;

    @Mock
    private PiiPciInspector piiPciInspector;

    @Mock
    private GherkinScanner gherkinScanner;

    @Mock
    private ProjectService projectService;

    @Mock
    private ClassMetadataRepository classMetadataRepository;

    @Mock
    private ApiEndpointRepository apiEndpointRepository;

    @Mock
    private AssetImageRepository assetImageRepository;

    @Mock
    private DbEntityRepository dbEntityRepository;

    @Mock
    private DaoOperationRepository daoOperationRepository;

    @Mock
    private LogStatementRepository logStatementRepository;

    @Mock
    private PiiPciFindingRepository piiPciFindingRepository;

    @Mock
    private ProjectSnapshotService projectSnapshotService;

    @Mock
    private DiagramBuilderService diagramBuilderService;

    @Mock
    private DiagramService diagramService;

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
                apiScanner,
                assetScanner,
                jpaEntityScanner,
                daoAnalysisService,
                loggerScanner,
                piiPciInspector,
                gherkinScanner,
                projectService,
                classMetadataRepository,
                apiEndpointRepository,
                assetImageRepository,
                dbEntityRepository,
                daoOperationRepository,
                logStatementRepository,
                piiPciFindingRepository,
                projectSnapshotService,
                diagramBuilderService,
                diagramService,
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

        MetadataDump metadataDump = new MetadataDump(List.of(), List.of(), List.of(), List.of());
        when(yamlScanner.scan(repoDir)).thenReturn(metadataDump);

        List<ApiEndpointRecord> endpointRecords = List.of(new ApiEndpointRecord(
                "REST", "GET", "/demo", "com.barclays.demo.Controller", "getDemo", List.of()));
        when(apiScanner.scan(repoDir, metadata.moduleRoots(), metadataDump)).thenReturn(endpointRecords);

        List<ImageAssetRecord> imageAssets =
                List.of(new ImageAssetRecord("diagram.png", "docs/diagram.png", 512L, "abc123"));
        when(assetScanner.scan(repoDir)).thenReturn(imageAssets);

        List<DbEntityRecord> entityRecords = List.of(new DbEntityRecord(
                "Customer",
                "com.barclays.demo.Customer",
                "customer",
                List.of("id"),
                List.of(),
                List.of()));
        when(jpaEntityScanner.scan(repoDir, metadata.moduleRoots())).thenReturn(entityRecords);
        when(gherkinScanner.scan(repoDir)).thenReturn(List.of());

        DbAnalysisResult daoAnalysisResult = new DbAnalysisResult(
                entityRecords,
                Map.of("Customer", List.of("com.barclays.demo.CustomerRepository")),
                Map.of(
                        "com.barclays.demo.CustomerRepository",
                        List.of(new DaoOperationRecord(
                                "com.barclays.demo.CustomerRepository",
                                "findAll",
                                "SELECT",
                                "Customer",
                                null))));
        when(daoAnalysisService.analyze(repoDir, metadata.moduleRoots(), entityRecords)).thenReturn(daoAnalysisResult);

        List<PiiPciFindingRecord> piiRecords =
                List.of(new PiiPciFindingRecord("application.yml", 12, "password: secret", "PII", "MEDIUM", false));
        when(piiPciInspector.scan(repoDir)).thenReturn(piiRecords);

        List<LogStatementRecord> logRecords = List.of(new LogStatementRecord(
                "com.barclays.demo.Controller",
                "src/main/java/com/barclays/demo/Controller.java",
                "INFO",
                42,
                "Processing request {}",
                List.of("requestId"),
                false,
                false));
        when(loggerScanner.scan(repoDir, metadata.moduleRoots())).thenReturn(logRecords);

        when(projectService.overwriteProject("https://example.com/repo.git", "demo-app", buildInfo)).thenReturn(project);

        DiagramGenerationResult diagramResult = new DiagramGenerationResult(List.of(), Map.of());
        when(diagramBuilderService.generate(
                        eq(repoDir), Mockito.anyList(), Mockito.anyList(), Mockito.any()))
                .thenReturn(diagramResult);
        when(diagramService.replaceProjectDiagrams(project, diagramResult.diagrams())).thenReturn(List.of());

        AnalysisOutcome outcome = analysisService.analyze("https://example.com/repo.git");

        assertEquals(project, outcome.project());
        ParsedDataResponse parsedData = outcome.parsedData();
        assertEquals(project.getId(), parsedData.projectId());
        assertEquals(buildInfo, parsedData.buildInfo());
        assertEquals(1, parsedData.classes().size());
        assertEquals(1, parsedData.apiEndpoints().size());
        assertEquals(1, parsedData.assets().images().size());
        assertEquals(1, parsedData.loggerInsights().size());
        assertEquals(1, parsedData.piiPciScan().size());

        verify(classMetadataRepository).deleteByProject(project);
        verify(classMetadataRepository)
                .saveAll(Mockito.<List<ClassMetadata>>argThat(list -> list.size() == 1
                        && "com.barclays.demo.Controller".equals(list.get(0).getFullyQualifiedName())));
        verify(apiEndpointRepository).deleteByProject(project);
        verify(apiEndpointRepository).saveAll(Mockito.anyList());
        verify(assetImageRepository).deleteByProject(project);
        verify(assetImageRepository).saveAll(Mockito.anyList());
        verify(dbEntityRepository).deleteByProject(project);
        verify(dbEntityRepository).saveAll(Mockito.<List<DbEntity>>argThat(list -> list.size() == 1
                && "Customer".equals(list.get(0).getEntityName())));
        verify(daoOperationRepository).deleteByProject(project);
        verify(daoOperationRepository).saveAll(Mockito.<List<DaoOperation>>argThat(list -> list.size() == 1
                && "findAll".equals(list.get(0).getMethodName())));
        verify(logStatementRepository).deleteByProject(project);
        verify(logStatementRepository).saveAll(Mockito.anyList());
        verify(piiPciFindingRepository).deleteByProject(project);
        verify(piiPciFindingRepository).saveAll(Mockito.anyList());

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
