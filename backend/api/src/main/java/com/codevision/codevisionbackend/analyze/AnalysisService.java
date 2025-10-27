package com.codevision.codevisionbackend.analyze;

import com.codevision.codevisionbackend.analyze.scanner.ApiEndpointRecord;
import com.codevision.codevisionbackend.analyze.scanner.ApiScanner;
import com.codevision.codevisionbackend.analyze.scanner.AssetScanner;
import com.codevision.codevisionbackend.analyze.scanner.BuildMetadataExtractor;
import com.codevision.codevisionbackend.analyze.scanner.BuildMetadataExtractor.BuildMetadata;
import com.codevision.codevisionbackend.analyze.scanner.ClassMetadataRecord;
import com.codevision.codevisionbackend.analyze.scanner.DaoAnalysisService;
import com.codevision.codevisionbackend.analyze.scanner.DaoOperationRecord;
import com.codevision.codevisionbackend.analyze.scanner.DbEntityRecord;
import com.codevision.codevisionbackend.analyze.scanner.ImageAssetRecord;
import com.codevision.codevisionbackend.analyze.scanner.JavaSourceScanner;
import com.codevision.codevisionbackend.analyze.scanner.JpaEntityScanner;
import com.codevision.codevisionbackend.analyze.scanner.YamlScanner;
import com.codevision.codevisionbackend.analyze.scanner.DbAnalysisResult;
import com.codevision.codevisionbackend.git.GitCloneService;
import com.codevision.codevisionbackend.project.Project;
import com.codevision.codevisionbackend.project.ProjectService;
import com.codevision.codevisionbackend.project.ProjectSnapshotService;
import com.codevision.codevisionbackend.project.api.ApiEndpoint;
import com.codevision.codevisionbackend.project.api.ApiEndpointRepository;
import com.codevision.codevisionbackend.project.asset.AssetImage;
import com.codevision.codevisionbackend.project.asset.AssetImageRepository;
import com.codevision.codevisionbackend.project.db.DaoOperation;
import com.codevision.codevisionbackend.project.db.DaoOperationRepository;
import com.codevision.codevisionbackend.project.db.DbEntity;
import com.codevision.codevisionbackend.project.db.DbEntityRepository;
import com.codevision.codevisionbackend.project.metadata.ClassMetadata;
import com.codevision.codevisionbackend.project.metadata.ClassMetadataRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
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
    private final ApiScanner apiScanner;
    private final AssetScanner assetScanner;
    private final JpaEntityScanner jpaEntityScanner;
    private final DaoAnalysisService daoAnalysisService;
    private final ProjectService projectService;
    private final ClassMetadataRepository classMetadataRepository;
    private final ApiEndpointRepository apiEndpointRepository;
    private final AssetImageRepository assetImageRepository;
    private final DbEntityRepository dbEntityRepository;
    private final DaoOperationRepository daoOperationRepository;
    private final ProjectSnapshotService projectSnapshotService;
    private final ObjectMapper objectMapper;

    public AnalysisService(
            GitCloneService gitCloneService,
            BuildMetadataExtractor buildMetadataExtractor,
            JavaSourceScanner javaSourceScanner,
            YamlScanner yamlScanner,
            ApiScanner apiScanner,
            AssetScanner assetScanner,
            JpaEntityScanner jpaEntityScanner,
            DaoAnalysisService daoAnalysisService,
            ProjectService projectService,
            ClassMetadataRepository classMetadataRepository,
            ApiEndpointRepository apiEndpointRepository,
            AssetImageRepository assetImageRepository,
            DbEntityRepository dbEntityRepository,
            DaoOperationRepository daoOperationRepository,
            ProjectSnapshotService projectSnapshotService,
            ObjectMapper objectMapper) {
        this.gitCloneService = gitCloneService;
        this.buildMetadataExtractor = buildMetadataExtractor;
        this.javaSourceScanner = javaSourceScanner;
        this.yamlScanner = yamlScanner;
        this.apiScanner = apiScanner;
        this.assetScanner = assetScanner;
        this.jpaEntityScanner = jpaEntityScanner;
        this.daoAnalysisService = daoAnalysisService;
        this.projectService = projectService;
        this.classMetadataRepository = classMetadataRepository;
        this.apiEndpointRepository = apiEndpointRepository;
        this.assetImageRepository = assetImageRepository;
        this.dbEntityRepository = dbEntityRepository;
        this.daoOperationRepository = daoOperationRepository;
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
            List<DbEntityRecord> entityRecords =
                    jpaEntityScanner.scan(cloneResult.directory(), buildMetadata.moduleRoots());
            DbAnalysisResult dbAnalysisResult =
                    daoAnalysisService.analyze(cloneResult.directory(), buildMetadata.moduleRoots(), entityRecords);
            log.info(
                    "Discovered {} JPA entities and {} repository operations for {}",
                    dbAnalysisResult.entities().size(),
                    dbAnalysisResult.operationsByClass().values().stream()
                            .mapToInt(list -> list == null ? 0 : list.size())
                            .sum(),
                    repoUrl);
            replaceDbEntities(persistedProject, dbAnalysisResult.entities());
            replaceDaoOperations(persistedProject, dbAnalysisResult.operationsByClass());
            MetadataDump metadataDump = yamlScanner.scan(cloneResult.directory());
            log.debug(
                    "Collected {} metadata artifacts for {}",
                    (metadataDump.openApiSpecs() != null ? metadataDump.openApiSpecs().size() : 0)
                            + (metadataDump.wsdlDocuments() != null ? metadataDump.wsdlDocuments().size() : 0)
                            + (metadataDump.xsdDocuments() != null ? metadataDump.xsdDocuments().size() : 0),
                    repoUrl);
            List<ApiEndpointRecord> apiEndpoints =
                    apiScanner.scan(cloneResult.directory(), buildMetadata.moduleRoots(), metadataDump);
            log.info("Discovered {} API endpoints for {}", apiEndpoints.size(), repoUrl);
            replaceApiEndpoints(persistedProject, apiEndpoints);
            List<ImageAssetRecord> imageAssets = assetScanner.scan(cloneResult.directory());
            log.info("Discovered {} image assets for {}", imageAssets.size(), repoUrl);
            replaceAssetImages(persistedProject, imageAssets);
            DbAnalysisSummary dbAnalysisSummary = toDbAnalysisSummary(dbAnalysisResult);
            ParsedDataResponse parsedData =
                    assembleParsedData(
                            persistedProject,
                            buildMetadata.buildInfo(),
                            classRecords,
                            metadataDump,
                            dbAnalysisSummary,
                            apiEndpoints,
                            imageAssets);
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
            List<ImageAssetRecord> imageAssets) {
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
                assetInventory);
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
            apiEndpointRepository.deleteByProject(existing);
            assetImageRepository.deleteByProject(existing);
            dbEntityRepository.deleteByProject(existing);
            daoOperationRepository.deleteByProject(existing);
            projectService.delete(existing);
        });
    }
}
