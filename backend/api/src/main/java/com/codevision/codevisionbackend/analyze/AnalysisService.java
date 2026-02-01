package com.codevision.codevisionbackend.analyze;

import static com.codevision.codevisionbackend.git.BranchUtils.normalize;

import com.codevision.codevisionbackend.analysis.CompiledAnalysisService;
import com.codevision.codevisionbackend.analysis.CompiledAnalysisService.CompiledAnalysisParameters;
import com.codevision.codevisionbackend.analyze.GherkinFeatureSummary;
import com.codevision.codevisionbackend.analyze.diagram.DiagramBuilderService;
import com.codevision.codevisionbackend.analyze.diagram.DiagramGenerationResult;
import com.codevision.codevisionbackend.analyze.scanner.ApiEndpointRecord;
import com.codevision.codevisionbackend.analyze.scanner.ApiScanner;
import com.codevision.codevisionbackend.analyze.scanner.AnalysisExclusions;
import com.codevision.codevisionbackend.analyze.scanner.AssetScanner;
import com.codevision.codevisionbackend.analyze.scanner.BuildMetadataExtractor;
import com.codevision.codevisionbackend.analyze.scanner.BuildMetadataExtractor.BuildMetadata;
import com.codevision.codevisionbackend.analyze.scanner.ClassMetadataRecord;
import com.codevision.codevisionbackend.analyze.scanner.ClassMetadataRecord.SourceSet;
import com.codevision.codevisionbackend.analyze.scanner.DaoAnalysisService;
import com.codevision.codevisionbackend.analyze.scanner.DaoOperationRecord;
import com.codevision.codevisionbackend.analyze.scanner.DbEntityRecord;
import com.codevision.codevisionbackend.analyze.scanner.GherkinScanner;
import com.codevision.codevisionbackend.analyze.scanner.ImageAssetRecord;
import com.codevision.codevisionbackend.analyze.scanner.JavaSourceScanner;
import com.codevision.codevisionbackend.analyze.scanner.JpaEntityScanner;
import com.codevision.codevisionbackend.analyze.scanner.LogStatementRecord;
import com.codevision.codevisionbackend.analyze.scanner.LoggerScanner;
import com.codevision.codevisionbackend.analyze.scanner.PiiPciFindingRecord;
import com.codevision.codevisionbackend.analyze.scanner.PiiPciInspector;
import com.codevision.codevisionbackend.analyze.scanner.YamlScanner;
import com.codevision.codevisionbackend.analyze.scanner.DbAnalysisResult;
import com.codevision.codevisionbackend.git.GitCloneService;
import com.codevision.codevisionbackend.project.Project;
import com.codevision.codevisionbackend.project.ProjectService;
import com.codevision.codevisionbackend.project.ProjectSnapshot;
import com.codevision.codevisionbackend.project.ProjectSnapshotService;
import com.codevision.codevisionbackend.project.ProjectSnapshotService.SnapshotMetadata;
import com.codevision.codevisionbackend.project.api.ApiEndpoint;
import com.codevision.codevisionbackend.project.api.ApiEndpointRepository;
import com.codevision.codevisionbackend.project.asset.AssetImage;
import com.codevision.codevisionbackend.project.asset.AssetImageRepository;
import com.codevision.codevisionbackend.project.db.DaoOperation;
import com.codevision.codevisionbackend.project.db.DaoOperationRepository;
import com.codevision.codevisionbackend.project.db.DbEntity;
import com.codevision.codevisionbackend.project.db.DbEntityRepository;
import com.codevision.codevisionbackend.project.logger.LogStatement;
import com.codevision.codevisionbackend.project.logger.LogStatementRepository;
import com.codevision.codevisionbackend.project.metadata.ClassMetadata;
import com.codevision.codevisionbackend.project.metadata.ClassMetadataRepository;
import com.codevision.codevisionbackend.project.security.PiiPciFinding;
import com.codevision.codevisionbackend.project.security.PiiPciFindingRepository;
import com.codevision.codevisionbackend.project.diagram.Diagram;
import com.codevision.codevisionbackend.project.diagram.DiagramService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);
    private static final String ROOT_MODULE_KEY = "/";

    private final GitCloneService gitCloneService;
    private final BuildMetadataExtractor buildMetadataExtractor;
    private final JavaSourceScanner javaSourceScanner;
    private final YamlScanner yamlScanner;
    private final ApiScanner apiScanner;
    private final AssetScanner assetScanner;
    private final JpaEntityScanner jpaEntityScanner;
    private final DaoAnalysisService daoAnalysisService;
    private final LoggerScanner loggerScanner;
    private final PiiPciInspector piiPciInspector;
    private final ProjectService projectService;
    private final ClassMetadataRepository classMetadataRepository;
    private final ApiEndpointRepository apiEndpointRepository;
    private final AssetImageRepository assetImageRepository;
    private final DbEntityRepository dbEntityRepository;
    private final DaoOperationRepository daoOperationRepository;
    private final LogStatementRepository logStatementRepository;
    private final PiiPciFindingRepository piiPciFindingRepository;
    private final ProjectSnapshotService projectSnapshotService;
    private final DiagramBuilderService diagramBuilderService;
    private final DiagramService diagramService;
    private final ObjectMapper objectMapper;
    private final GherkinScanner gherkinScanner;
    private final CompiledAnalysisService compiledAnalysisService;

    public AnalysisService(
            GitCloneService gitCloneService,
            BuildMetadataExtractor buildMetadataExtractor,
            JavaSourceScanner javaSourceScanner,
            YamlScanner yamlScanner,
            ApiScanner apiScanner,
            AssetScanner assetScanner,
            JpaEntityScanner jpaEntityScanner,
            DaoAnalysisService daoAnalysisService,
            LoggerScanner loggerScanner,
            PiiPciInspector piiPciInspector,
            GherkinScanner gherkinScanner,
            ProjectService projectService,
            ClassMetadataRepository classMetadataRepository,
            ApiEndpointRepository apiEndpointRepository,
            AssetImageRepository assetImageRepository,
            DbEntityRepository dbEntityRepository,
            DaoOperationRepository daoOperationRepository,
            LogStatementRepository logStatementRepository,
            PiiPciFindingRepository piiPciFindingRepository,
            ProjectSnapshotService projectSnapshotService,
            DiagramBuilderService diagramBuilderService,
            DiagramService diagramService,
            ObjectMapper objectMapper,
            CompiledAnalysisService compiledAnalysisService) {
        this.gitCloneService = gitCloneService;
        this.buildMetadataExtractor = buildMetadataExtractor;
        this.javaSourceScanner = javaSourceScanner;
        this.yamlScanner = yamlScanner;
        this.apiScanner = apiScanner;
        this.assetScanner = assetScanner;
        this.jpaEntityScanner = jpaEntityScanner;
        this.daoAnalysisService = daoAnalysisService;
        this.loggerScanner = loggerScanner;
        this.piiPciInspector = piiPciInspector;
        this.gherkinScanner = gherkinScanner;
        this.projectService = projectService;
        this.classMetadataRepository = classMetadataRepository;
        this.apiEndpointRepository = apiEndpointRepository;
        this.assetImageRepository = assetImageRepository;
        this.dbEntityRepository = dbEntityRepository;
        this.daoOperationRepository = daoOperationRepository;
        this.logStatementRepository = logStatementRepository;
        this.piiPciFindingRepository = piiPciFindingRepository;
        this.projectSnapshotService = projectSnapshotService;
        this.diagramBuilderService = diagramBuilderService;
        this.diagramService = diagramService;
        this.objectMapper = objectMapper;
        this.compiledAnalysisService = compiledAnalysisService;
    }

    @Transactional
    public AnalysisOutcome analyze(String repoUrl, String branchName) {
        return analyze(repoUrl, branchName, true);
    }

    @Transactional
    public AnalysisOutcome analyze(String repoUrl, String branchName, boolean includeSecurity) {
        String normalizedBranch = normalize(branchName);
        log.info("Starting analysis for {} (branch={})", repoUrl, normalizedBranch);
        GitCloneService.CloneResult cloneResult = gitCloneService.cloneRepository(repoUrl, normalizedBranch);
        try {
            log.debug("Repository {} cloned to {}", repoUrl, cloneResult.directory());
            BuildMetadata buildMetadata = buildMetadataExtractor.extract(cloneResult.directory());
            List<ModuleDescriptor> moduleDescriptors = describeModules(cloneResult.directory(), buildMetadata.moduleRoots());
            Map<String, String> moduleFingerprints =
                    computeModuleFingerprints(cloneResult.directory(), moduleDescriptors, cloneResult.commitHash());
            Project persistedProject = projectService.overwriteProject(
                    repoUrl, cloneResult.branchName(), cloneResult.projectName(), buildMetadata.buildInfo());

            ParsedDataResponse previousSnapshotData = null;
            Map<String, String> previousFingerprints = Map.of();
            Long previousSnapshotId = null;
            Optional<ProjectSnapshot> previousSnapshot =
                    persistedProject.getId() == null
                            ? Optional.empty()
                            : projectSnapshotService.findLatestSnapshotEntity(persistedProject.getId());
            if (previousSnapshot.isPresent()) {
                previousSnapshotData = projectSnapshotService.hydrateSnapshot(previousSnapshot.get());
                previousFingerprints = projectSnapshotService.readModuleFingerprints(previousSnapshot.get());
                previousSnapshotId = previousSnapshot.get().getId();
                if (includeSecurity
                        && cloneResult.commitHash() != null
                        && cloneResult.commitHash().equals(previousSnapshot.get().getCommitHash())
                        && previousSnapshotData != null) {
                    log.info(
                            "Reusing snapshot {} for {} ({}) because commit {} is unchanged",
                            previousSnapshotId,
                            repoUrl,
                            cloneResult.branchName(),
                            cloneResult.commitHash());
                    return new AnalysisOutcome(
                            persistedProject,
                            previousSnapshotData,
                            cloneResult.branchName(),
                            cloneResult.commitHash(),
                            previousSnapshotId,
                            true);
                }
            }

            Set<String> changedModules = determineChangedModules(previousFingerprints, moduleFingerprints);
            ModuleIndex moduleIndex = new ModuleIndex(moduleDescriptors);
            List<Path> modulesToScan = selectModulesToScan(moduleDescriptors, changedModules);
            boolean scanAllModules = modulesToScan.isEmpty();
            boolean hasSubModules = moduleDescriptors.stream().anyMatch(descriptor -> descriptor.depth() > 0);
            List<Path> effectiveModuleRoots = scanAllModules
                    ? moduleDescriptors.stream()
                            .filter(descriptor -> !hasSubModules || descriptor.depth() > 0)
                            .map(ModuleDescriptor::absolutePath)
                            .toList()
                    : modulesToScan;
            List<Path> piiScanRoots = includeSecurity
                    ? (scanAllModules ? List.of(cloneResult.directory()) : modulesToScan)
                    : List.of();
            ReusedData reusedData =
                    reusePreviousData(previousSnapshotData, moduleDescriptors, moduleIndex, changedModules);

            List<ClassMetadataRecord> newClassRecords =
                    javaSourceScanner.scan(cloneResult.directory(), effectiveModuleRoots);
            List<ClassMetadataRecord> classRecords = mergeLists(reusedData.classMetadata(), newClassRecords);
            classRecords = classRecords.stream()
                    .filter(record -> record.sourceSet() != SourceSet.TEST)
                    .filter(record -> !AnalysisExclusions.isExcludedPath(record.relativePath()))
                    .filter(record -> !AnalysisExclusions.isMockClassName(record.className()))
                    .toList();
            replaceClassMetadata(persistedProject, classRecords);

            List<DbEntityRecord> entityRecords =
                    jpaEntityScanner.scan(cloneResult.directory(), effectiveModuleRoots);
            DbAnalysisResult dbAnalysisResult =
                    daoAnalysisService.analyze(cloneResult.directory(), effectiveModuleRoots, entityRecords);
            replaceDbEntities(persistedProject, dbAnalysisResult.entities());
            replaceDaoOperations(persistedProject, dbAnalysisResult.operationsByClass());

            MetadataDump metadataDump = yamlScanner.scan(cloneResult.directory());
            List<ApiEndpointRecord> apiEndpoints =
                    apiScanner.scan(cloneResult.directory(), effectiveModuleRoots, metadataDump);
            replaceApiEndpoints(persistedProject, apiEndpoints);

            List<ImageAssetRecord> imageAssets = assetScanner.scan(cloneResult.directory());
            replaceAssetImages(persistedProject, imageAssets);

            List<PiiPciFindingRecord> piiFindings = includeSecurity
                    ? mergeLists(reusedData.piiFindings(), piiPciInspector.scan(cloneResult.directory(), piiScanRoots))
                    : List.of();
            if (!piiFindings.isEmpty()) {
                piiFindings = piiFindings.stream()
                        .filter(record -> !AnalysisExclusions.isExcludedPath(record.filePath()))
                        .toList();
            }
            replacePiiPciFindings(persistedProject, piiFindings);

            List<LogStatementRecord> logStatements = includeSecurity
                    ? mergeLists(reusedData.logStatements(), loggerScanner.scan(cloneResult.directory(), effectiveModuleRoots))
                    : List.of();
            if (!logStatements.isEmpty()) {
                logStatements = logStatements.stream()
                        .filter(record -> !AnalysisExclusions.isExcludedPath(record.filePath()))
                        .filter(record -> !AnalysisExclusions.isMockClassName(record.className()))
                        .toList();
            }
            replaceLogStatements(persistedProject, logStatements);

            List<GherkinFeatureSummary> gherkinFeatures = gherkinScanner.scan(cloneResult.directory());
            DbAnalysisSummary dbAnalysisSummary = toDbAnalysisSummary(dbAnalysisResult);
            DiagramGenerationResult diagramGeneration =
                    diagramBuilderService.generate(
                            cloneResult.directory(), classRecords, apiEndpoints, dbAnalysisResult);
            List<Diagram> persistedDiagrams =
                    diagramService.replaceProjectDiagrams(persistedProject, diagramGeneration.diagrams());
            List<DiagramSummary> diagramSummaries = persistedDiagrams.stream()
                    .map(diagramService::toSummary)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            ParsedDataResponse parsedData =
                    assembleParsedData(
                            persistedProject,
                            buildMetadata.buildInfo(),
                            classRecords,
                            metadataDump,
                            dbAnalysisSummary,
                            apiEndpoints,
                            imageAssets,
                            logStatements,
                            piiFindings,
                            gherkinFeatures,
                            diagramGeneration.callFlows(),
                            diagramSummaries);
            ProjectSnapshot snapshot = projectSnapshotService.saveSnapshot(
                    persistedProject,
                    parsedData,
                    new SnapshotMetadata(cloneResult.branchName(), cloneResult.commitHash(), moduleFingerprints));

            try {
                compiledAnalysisService.analyze(new CompiledAnalysisParameters(
                        cloneResult.directory(), null, null, persistedProject.getId()));
            } catch (Exception compiledEx) {
                log.warn(
                        "Compiled analysis failed for projectId={} repo={} : {}",
                        persistedProject.getId(),
                        repoUrl,
                        compiledEx.getMessage());
            }

            log.info(
                    "Completed analysis for {} ({}) with projectId={} snapshotId={}",
                    repoUrl,
                    cloneResult.branchName(),
                    persistedProject.getId(),
                    snapshot.getId());
            return new AnalysisOutcome(
                    persistedProject,
                    parsedData,
                    cloneResult.branchName(),
                    cloneResult.commitHash(),
                    snapshot.getId(),
                    false);
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
        entity.setAnnotationsJson(writeJsonValue(record.annotations()));
        entity.setInterfacesJson(writeJsonValue(record.implementedInterfaces()));
        return entity;
    }

    private void replaceDbEntities(Project project, List<DbEntityRecord> entityRecords) {
        dbEntityRepository.deleteByProject(project);
        if (entityRecords == null || entityRecords.isEmpty()) {
            log.info("No database entities to persist for projectId={}", project.getId());
            return;
        }
        List<DbEntity> entities = entityRecords.stream()
                .map(record -> mapDbEntity(project, record))
                .collect(Collectors.toList());
        dbEntityRepository.saveAll(entities);
        log.info("Persisted {} database entities for projectId={}", entities.size(), project.getId());
    }

    private DbEntity mapDbEntity(Project project, DbEntityRecord record) {
        DbEntity entity = new DbEntity();
        entity.setProject(project);
        entity.setEntityName(record.className());
        entity.setFullyQualifiedName(record.fullyQualifiedName());
        entity.setTableName(record.tableName());
        entity.setPrimaryKeysJson(writeJsonValue(record.primaryKeys()));
        entity.setFieldsJson(writeJsonValue(record.fields()));
        entity.setRelationshipsJson(writeJsonValue(record.relationships()));
        return entity;
    }

    private void replaceDaoOperations(Project project, Map<String, List<DaoOperationRecord>> operationsByClass) {
        daoOperationRepository.deleteByProject(project);
        if (operationsByClass == null || operationsByClass.isEmpty()) {
            log.info("No DAO operations to persist for projectId={}", project.getId());
            return;
        }
        List<DaoOperation> operations = operationsByClass.values().stream()
                .flatMap(List::stream)
                .map(record -> mapDaoOperation(project, record))
                .collect(Collectors.toList());
        if (operations.isEmpty()) {
            log.info("No DAO operations to persist after flattening for projectId={}", project.getId());
            return;
        }
        daoOperationRepository.saveAll(operations);
        log.info("Persisted {} DAO operations for projectId={}", operations.size(), project.getId());
    }

    private DaoOperation mapDaoOperation(Project project, DaoOperationRecord record) {
        DaoOperation entity = new DaoOperation();
        entity.setProject(project);
        entity.setRepositoryClass(record.repositoryClass());
        entity.setMethodName(record.methodName());
        entity.setOperationType(record.operationType());
        entity.setTargetDescriptor(record.target());
        entity.setQuerySnippet(record.querySnippet());
        return entity;
    }

    private DbAnalysisSummary toDbAnalysisSummary(DbAnalysisResult dbAnalysisResult) {
        if (dbAnalysisResult == null) {
            return new DbAnalysisSummary(List.of(), Map.of(), Map.of());
        }

        List<DbAnalysisSummary.DbEntitySummary> entities = dbAnalysisResult.entities().stream()
                .map(record -> new DbAnalysisSummary.DbEntitySummary(
                        record.className(),
                        record.fullyQualifiedName(),
                        record.tableName(),
                        record.primaryKeys(),
                        record.fields().stream()
                                .map(field -> new DbAnalysisSummary.DbEntitySummary.FieldSummary(
                                        field.name(), field.type(), field.columnName()))
                                .collect(Collectors.toList()),
                        record.relationships().stream()
                                .map(rel -> new DbAnalysisSummary.DbEntitySummary.RelationshipSummary(
                                        rel.fieldName(), rel.targetType(), rel.relationshipType()))
                                .collect(Collectors.toList())))
                .collect(Collectors.toList());

        Map<String, List<String>> classesByEntity = dbAnalysisResult.classesByEntity().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue() == null ? List.of() : List.copyOf(entry.getValue())));

        Map<String, List<DbAnalysisSummary.DaoOperationDetails>> operationsByClass =
                dbAnalysisResult.operationsByClass().entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> (entry.getValue() == null ? List.<DaoOperationRecord>of() : entry.getValue()).stream()
                                        .map(op -> new DbAnalysisSummary.DaoOperationDetails(
                                                op.methodName(), op.operationType(), op.target(), op.querySnippet()))
                                        .collect(Collectors.toList())));

        return new DbAnalysisSummary(entities, classesByEntity, operationsByClass);
    }

    private ParsedDataResponse assembleParsedData(
            Project project,
            BuildInfo buildInfo,
            List<ClassMetadataRecord> classRecords,
            MetadataDump metadataDump,
            DbAnalysisSummary dbAnalysis,
            List<ApiEndpointRecord> apiEndpoints,
            List<ImageAssetRecord> imageAssets,
            List<LogStatementRecord> logStatements,
            List<PiiPciFindingRecord> piiFindings,
            List<GherkinFeatureSummary> gherkinFeatures,
            Map<String, List<String>> callFlows,
            List<DiagramSummary> diagrams) {
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

        List<ApiEndpointSummary> endpointSummaries = apiEndpoints.stream()
                .map(record -> new ApiEndpointSummary(
                        record.protocol(),
                        record.httpMethod(),
                        record.pathOrOperation(),
                        record.controllerClass(),
                        record.controllerMethod(),
                        record.specArtifacts().stream()
                                .map(artifact -> new ApiEndpointSummary.ApiSpecArtifact(
                                        artifact.type(), artifact.name(), artifact.reference()))
                                .toList()))
                .toList();

        List<AssetInventory.ImageAsset> images = imageAssets.stream()
                .map(asset -> new AssetInventory.ImageAsset(
                        asset.fileName(), asset.relativePath(), asset.sizeBytes(), asset.sha256()))
                .toList();
        AssetInventory assetInventory = images.isEmpty() ? AssetInventory.empty() : new AssetInventory(images);

        List<LoggerInsightSummary> loggerInsights = logStatements.stream()
                .map(record -> new LoggerInsightSummary(
                        record.className(),
                        record.filePath(),
                        record.logLevel(),
                        record.lineNumber(),
                        record.messageTemplate(),
                        record.variables(),
                        record.piiRisk(),
                        record.pciRisk()))
                .toList();

        List<PiiPciFindingSummary> piiSummaries = piiFindings.stream()
                .map(record -> new PiiPciFindingSummary(
                        null,
                        record.filePath(),
                        record.lineNumber(),
                        record.snippet(),
                        record.matchType(),
                        record.severity(),
                        record.ignored()))
                .toList();

        return new ParsedDataResponse(
                project.getId(),
                project.getProjectName(),
                project.getRepoUrl(),
                project.getLastAnalyzedAt(),
                buildInfo,
                classSummaries,
                metadataDump,
                dbAnalysis,
                endpointSummaries,
                assetInventory,
                loggerInsights,
                piiSummaries,
                gherkinFeatures,
                callFlows,
                diagrams);
    }

    private String writeJsonValue(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize metadata values", e);
        }
    }

    private void replaceApiEndpoints(Project project, List<ApiEndpointRecord> endpointRecords) {
        apiEndpointRepository.deleteByProject(project);
        if (endpointRecords.isEmpty()) {
            log.info("No API endpoints to persist for projectId={}", project.getId());
            return;
        }
        List<ApiEndpoint> entities = endpointRecords.stream()
                .map(record -> mapEndpoint(project, record))
                .toList();
        apiEndpointRepository.saveAll(entities);
        log.info("Persisted {} API endpoints for projectId={}", entities.size(), project.getId());
    }

    private ApiEndpoint mapEndpoint(Project project, ApiEndpointRecord record) {
        ApiEndpoint entity = new ApiEndpoint();
        entity.setProject(project);
        entity.setProtocol(record.protocol());
        entity.setHttpMethod(record.httpMethod());
        entity.setPathOrOperation(record.pathOrOperation());
        entity.setControllerClass(record.controllerClass());
        entity.setControllerMethod(record.controllerMethod());
        entity.setSpecArtifactsJson(writeJsonValue(record.specArtifacts()));
        return entity;
    }

    private void replaceAssetImages(Project project, List<ImageAssetRecord> imageAssets) {
        assetImageRepository.deleteByProject(project);
        if (imageAssets.isEmpty()) {
            log.info("No image assets to persist for projectId={}", project.getId());
            return;
        }
        List<AssetImage> entities = imageAssets.stream()
                .map(asset -> mapAsset(project, asset))
                .toList();
        assetImageRepository.saveAll(entities);
        log.info("Persisted {} image assets for projectId={}", entities.size(), project.getId());
    }

    private AssetImage mapAsset(Project project, ImageAssetRecord asset) {
        AssetImage entity = new AssetImage();
        entity.setProject(project);
        entity.setFileName(asset.fileName());
        entity.setRelativePath(asset.relativePath());
        entity.setSizeBytes(asset.sizeBytes());
        entity.setSha256(asset.sha256());
        return entity;
    }

    private void replaceLogStatements(Project project, List<LogStatementRecord> logStatements) {
        logStatementRepository.deleteByProject(project);
        if (logStatements == null || logStatements.isEmpty()) {
            log.info("No log statements to persist for projectId={}", project.getId());
            return;
        }
        List<LogStatement> entities = logStatements.stream()
                .map(record -> mapLogStatement(project, record))
                .toList();
        logStatementRepository.saveAll(entities);
        log.info("Persisted {} log statements for projectId={}", entities.size(), project.getId());
    }

    private LogStatement mapLogStatement(Project project, LogStatementRecord record) {
        LogStatement entity = new LogStatement();
        entity.setProject(project);
        entity.setClassName(record.className());
        entity.setFilePath(record.filePath());
        entity.setLogLevel(record.logLevel());
        entity.setLineNumber(record.lineNumber());
        entity.setMessageTemplate(record.messageTemplate());
        entity.setVariablesJson(writeJsonValue(record.variables()));
        entity.setPiiRisk(record.piiRisk());
        entity.setPciRisk(record.pciRisk());
        return entity;
    }

    private void replacePiiPciFindings(Project project, List<PiiPciFindingRecord> findings) {
        piiPciFindingRepository.deleteByProject(project);
        if (findings == null || findings.isEmpty()) {
            log.info("No PII/PCI findings to persist for projectId={}", project.getId());
            return;
        }
        List<PiiPciFinding> entities = findings.stream()
                .map(record -> mapPiiPciFinding(project, record))
                .toList();
        piiPciFindingRepository.saveAll(entities);
        log.info("Persisted {} PII/PCI findings for projectId={}", entities.size(), project.getId());
    }

    private PiiPciFinding mapPiiPciFinding(Project project, PiiPciFindingRecord record) {
        PiiPciFinding entity = new PiiPciFinding();
        entity.setProject(project);
        entity.setFilePath(record.filePath());
        entity.setLineNumber(record.lineNumber());
        entity.setSnippet(record.snippet());
        entity.setMatchType(record.matchType());
        entity.setSeverity(record.severity());
        entity.setIgnored(record.ignored());
        return entity;
    }

    private void cleanupClone(GitCloneService.CloneResult cloneResult) {
        try {
            gitCloneService.cleanupClone(cloneResult);
        } catch (Exception e) {
            log.warn("Failed to cleanup temporary clone for {}", cloneResult.projectName(), e);
        }
    }

    private List<ModuleDescriptor> describeModules(Path repoRoot, List<Path> moduleRoots) {
        Path normalizedRoot = repoRoot.toAbsolutePath().normalize();
        Map<String, ModuleDescriptor> descriptors = new LinkedHashMap<>();
        descriptors.put(ROOT_MODULE_KEY, new ModuleDescriptor(ROOT_MODULE_KEY, normalizedRoot, Path.of(""), 0));
        for (Path moduleRoot : moduleRoots) {
            if (moduleRoot == null) {
                continue;
            }
            Path normalizedModule = moduleRoot.toAbsolutePath().normalize();
            if (!Files.isDirectory(normalizedModule)) {
                continue;
            }
            Path relative;
            try {
                relative = normalizedRoot.relativize(normalizedModule);
            } catch (IllegalArgumentException ex) {
                relative = Path.of("");
            }
            String key = relative.getNameCount() == 0 ? ROOT_MODULE_KEY : relative.toString().replace('\\', '/');
            descriptors.put(key, new ModuleDescriptor(key, normalizedModule, relative, relative.getNameCount()));
        }
        return new ArrayList<>(descriptors.values());
    }

    private Map<String, String> computeModuleFingerprints(
            Path repoRoot, List<ModuleDescriptor> modules, String commitHash) {
        Map<String, String> fingerprints = new LinkedHashMap<>();
        if (modules.isEmpty()) {
            return fingerprints;
        }
        try (Git git = Git.open(repoRoot.toFile()); RevWalk revWalk = new RevWalk(git.getRepository())) {
            ObjectId headId = commitHash != null
                    ? git.getRepository().resolve(commitHash)
                    : git.getRepository().resolve(Constants.HEAD);
            if (headId == null) {
                modules.forEach(descriptor -> fingerprints.put(descriptor.key(), commitHash));
                return fingerprints;
            }
            RevCommit commit = revWalk.parseCommit(headId);
            RevTree tree = commit.getTree();
            for (ModuleDescriptor descriptor : modules) {
                if (descriptor.depth() == 0) {
                    fingerprints.put(descriptor.key(), tree.getId().name());
                    continue;
                }
                String modulePath = descriptor.relativePath() == null
                        ? ""
                        : descriptor.relativePath().toString().replace('\\', '/');
                if (modulePath.isBlank()) {
                    fingerprints.put(descriptor.key(), tree.getId().name());
                    continue;
                }
                try (TreeWalk treeWalk = TreeWalk.forPath(git.getRepository(), modulePath, tree)) {
                    fingerprints.put(descriptor.key(), treeWalk != null ? treeWalk.getObjectId(0).name() : commitHash);
                }
            }
            return fingerprints;
        } catch (IOException e) {
            log.warn("Failed to compute module fingerprints", e);
            modules.forEach(descriptor -> fingerprints.put(descriptor.key(), commitHash));
            return fingerprints;
        }
    }

    private Set<String> determineChangedModules(Map<String, String> previous, Map<String, String> current) {
        if (previous.isEmpty()) {
            return new LinkedHashSet<>(current.keySet());
        }
        Set<String> changed = new LinkedHashSet<>();
        boolean hasSubModules = current.keySet().stream().anyMatch(key -> !ROOT_MODULE_KEY.equals(key));
        current.forEach((key, value) -> {
            if (hasSubModules && ROOT_MODULE_KEY.equals(key)) {
                return;
            }
            if (!Objects.equals(value, previous.get(key))) {
                changed.add(key);
            }
        });
        previous.keySet().stream()
                .filter(key -> !current.containsKey(key))
                .forEach(changed::add);
        if (changed.isEmpty() && !current.isEmpty() && !hasSubModules) {
            changed.add(ROOT_MODULE_KEY);
        }
        return changed;
    }

    private List<Path> selectModulesToScan(List<ModuleDescriptor> modules, Set<String> changedModules) {
        if (changedModules.isEmpty() || changedModules.contains(ROOT_MODULE_KEY)) {
            return List.of();
        }
        Map<String, Path> pathByKey = modules.stream()
                .collect(Collectors.toMap(
                        ModuleDescriptor::key,
                        ModuleDescriptor::absolutePath,
                        (left, right) -> left,
                        LinkedHashMap::new));
        if (!pathByKey.keySet().containsAll(changedModules)) {
            return List.of();
        }
        return changedModules.stream()
                .map(pathByKey::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private ReusedData reusePreviousData(
            ParsedDataResponse previous,
            List<ModuleDescriptor> modules,
            ModuleIndex moduleIndex,
            Set<String> changedModules) {
        if (previous == null) {
            return ReusedData.empty();
        }
        Set<String> reusableModules = modules.stream()
                .map(ModuleDescriptor::key)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        reusableModules.removeAll(changedModules);
        if (reusableModules.isEmpty()) {
            return ReusedData.empty();
        }
        List<ClassMetadataRecord> classRecords = previous.classes() == null
                ? List.of()
                : previous.classes().stream()
                        .filter(summary -> reusableModules.contains(moduleIndex.detect(summary.relativePath())))
                        .map(this::fromSummary)
                        .toList();
        List<LogStatementRecord> logRecords = previous.loggerInsights() == null
                ? List.of()
                : previous.loggerInsights().stream()
                        .filter(entry -> reusableModules.contains(moduleIndex.detect(entry.filePath())))
                        .map(this::fromInsight)
                        .toList();
        List<PiiPciFindingRecord> piiRecords = previous.piiPciScan() == null
                ? List.of()
                : previous.piiPciScan().stream()
                        .filter(entry -> reusableModules.contains(moduleIndex.detect(entry.filePath())))
                        .map(this::fromFinding)
                        .toList();
        return new ReusedData(classRecords, logRecords, piiRecords);
    }

    private <T> List<T> mergeLists(List<T> reused, List<T> fresh) {
        if ((reused == null || reused.isEmpty()) && (fresh == null || fresh.isEmpty())) {
            return List.of();
        }
        List<T> merged = new ArrayList<>();
        if (reused != null && !reused.isEmpty()) {
            merged.addAll(reused);
        }
        if (fresh != null && !fresh.isEmpty()) {
            merged.addAll(fresh);
        }
        return merged;
    }

    private ClassMetadataRecord fromSummary(ClassMetadataSummary summary) {
        SourceSet sourceSet;
        try {
            sourceSet = summary.sourceSet() != null ? SourceSet.valueOf(summary.sourceSet()) : SourceSet.MAIN;
        } catch (IllegalArgumentException ex) {
            sourceSet = SourceSet.MAIN;
        }
        return new ClassMetadataRecord(
                summary.fullyQualifiedName(),
                summary.packageName(),
                summary.className(),
                summary.annotations(),
                summary.interfacesImplemented(),
                summary.stereotype(),
                sourceSet,
                summary.relativePath(),
                summary.userCode());
    }

    private LogStatementRecord fromInsight(LoggerInsightSummary summary) {
        return new LogStatementRecord(
                summary.className(),
                summary.filePath(),
                summary.logLevel(),
                summary.lineNumber(),
                summary.messageTemplate(),
                summary.variables(),
                summary.piiRisk(),
                summary.pciRisk());
    }

    private PiiPciFindingRecord fromFinding(PiiPciFindingSummary summary) {
        return new PiiPciFindingRecord(
                summary.filePath(),
                summary.lineNumber(),
                summary.snippet(),
                summary.matchType(),
                summary.severity(),
                summary.ignored());
    }

    private record ReusedData(
            List<ClassMetadataRecord> classMetadata,
            List<LogStatementRecord> logStatements,
            List<PiiPciFindingRecord> piiFindings) {

        private static ReusedData empty() {
            return new ReusedData(List.of(), List.of(), List.of());
        }
    }

    private record ModuleDescriptor(String key, Path absolutePath, Path relativePath, int depth) {}

    private static final class ModuleIndex {

        private final List<ModuleDescriptor> orderedModules;

        private ModuleIndex(List<ModuleDescriptor> modules) {
            this.orderedModules = modules.stream()
                    .sorted(Comparator.comparingInt(ModuleDescriptor::depth).reversed())
                    .toList();
        }

        private String detect(String relativePath) {
            Path candidate = normalize(relativePath);
            for (ModuleDescriptor descriptor : orderedModules) {
                if (descriptor.depth() == 0) {
                    return ROOT_MODULE_KEY;
                }
                if (candidate.startsWith(descriptor.relativePath())) {
                    return descriptor.key();
                }
            }
            return ROOT_MODULE_KEY;
        }

        private Path normalize(String value) {
            if (value == null || value.isBlank()) {
                return Path.of("");
            }
            return Path.of(value.replace('\\', '/')).normalize();
        }
    }

}
