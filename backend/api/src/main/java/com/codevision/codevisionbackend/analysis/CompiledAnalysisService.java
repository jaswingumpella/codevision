package com.codevision.codevisionbackend.analysis;

import com.codevision.codevisionbackend.analysis.GraphModel.ClassNode;
import com.codevision.codevisionbackend.analysis.GraphModel.DependencyEdge;
import com.codevision.codevisionbackend.analysis.GraphModel.DependencyKind;
import com.codevision.codevisionbackend.analysis.GraphModel.Origin;
import com.codevision.codevisionbackend.analysis.config.CompiledAnalysisProperties;
import com.codevision.codevisionbackend.analysis.persistence.CompiledAnalysisRun;
import com.codevision.codevisionbackend.analysis.persistence.CompiledAnalysisRunRepository;
import com.codevision.codevisionbackend.analysis.persistence.CompiledAnalysisRunStatus;
import com.codevision.codevisionbackend.analyze.scanner.BuildMetadataExtractor;
import com.codevision.codevisionbackend.analyze.scanner.ClassMetadataRecord;
import com.codevision.codevisionbackend.analyze.scanner.JavaSourceScanner;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Orchestrates the compiled analysis workflow end-to-end.
 */
@Service
public class CompiledAnalysisService {

    private final ClasspathBuilder classpathBuilder;
    private final BytecodeEntityScanner bytecodeEntityScanner;
    private final BytecodeCallGraphScanner callGraphScanner;
    private final TarjanScc tarjanScc;
    private final GraphMerger graphMerger;
    private final DiagramWriter diagramWriter;
    private final ExportWriter exportWriter;
    private final PersistService persistService;
    private final JavaSourceScanner javaSourceScanner;
    private final BuildMetadataExtractor buildMetadataExtractor;
    private final CompiledAnalysisProperties properties;
    private final CompiledAnalysisRunRepository runRepository;
    private final Path outputRoot;

    public CompiledAnalysisService(
            ClasspathBuilder classpathBuilder,
            BytecodeEntityScanner bytecodeEntityScanner,
            BytecodeCallGraphScanner callGraphScanner,
            TarjanScc tarjanScc,
            GraphMerger graphMerger,
            DiagramWriter diagramWriter,
            ExportWriter exportWriter,
            PersistService persistService,
            JavaSourceScanner javaSourceScanner,
            BuildMetadataExtractor buildMetadataExtractor,
            CompiledAnalysisProperties properties,
            CompiledAnalysisRunRepository runRepository) {
        this.classpathBuilder = classpathBuilder;
        this.bytecodeEntityScanner = bytecodeEntityScanner;
        this.callGraphScanner = callGraphScanner;
        this.tarjanScc = tarjanScc;
        this.graphMerger = graphMerger;
        this.diagramWriter = diagramWriter;
        this.exportWriter = exportWriter;
        this.persistService = persistService;
        this.javaSourceScanner = javaSourceScanner;
        this.buildMetadataExtractor = buildMetadataExtractor;
        this.properties = properties;
        this.runRepository = runRepository;
        this.outputRoot = Path.of(properties.getOutput().getRoot()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.outputRoot);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Unable to create compiled analysis output root at " + this.outputRoot, e);
        }
    }

    public CompiledAnalysisResult analyze(CompiledAnalysisParameters parameters) throws IOException {
        Path repoPath = parameters.repoPath().toAbsolutePath().normalize();
        if (!Files.isDirectory(repoPath)) {
            throw new IllegalArgumentException("Repository path does not exist: " + repoPath);
        }
        List<String> acceptPackages = effectivePackages(parameters.acceptPackages());
        boolean includeDependencies =
                parameters.includeDependencies() != null ? parameters.includeDependencies() : properties.isIncludeDependencies();

        CompiledAnalysisRun run = new CompiledAnalysisRun();
        run.setId(UUID.randomUUID());
        run.setRepoPath(repoPath.toString());
        run.setProjectId(parameters.projectId());
        run.setStartedAt(Instant.now());
        run.setStatus(CompiledAnalysisRunStatus.RUNNING);
        run.setStatusMessage("Bytecode analysis in progress");
        run.setAcceptPackages(String.join(",", acceptPackages));
        runRepository.save(run);

        Instant start = Instant.now();
        try {
            ClasspathBuilder.ClasspathDescriptor classpath =
                    classpathBuilder.build(repoPath, includeDependencies);
            GraphModel sourceGraph = buildSourceGraph(repoPath, acceptPackages);
            GraphModel bytecodeModel = bytecodeEntityScanner.scan(classpath, acceptPackages);
            GraphModel graphModel = graphMerger.merge(sourceGraph, bytecodeModel);

            BytecodeCallGraphScanner.CallGraphResult callGraphResult =
                    callGraphScanner.scan(classpath, acceptPackages);
            callGraphResult.methodEdges().forEach(graphModel::addMethodCallEdge);
            callGraphResult.classAdjacency().forEach((caller, callees) -> callees.forEach(callee -> graphModel.addDependency(
                    new DependencyEdge(DependencyKind.CALL, caller, callee, "call"))));

            TarjanScc.Result sccResult = tarjanScc.compute(graphModel.buildAdjacencyMap());
            graphModel.getClasses().values().forEach(node -> {
                Long scc = sccResult.componentIds().get(node.getName());
                node.setSccId(scc);
                node.setInCycle(scc != null && sccResult.cyclicComponents().contains(scc));
            });

            Path outputDir = outputRoot.resolve(run.getId().toString());
            Files.createDirectories(outputDir);

            DiagramWriter.DiagramArtifacts diagramArtifacts =
                    diagramWriter.writeDiagrams(graphModel, outputDir, properties);
            AnalysisOutputPaths outputs = exportWriter.writeAll(graphModel, outputDir, diagramArtifacts);
            persistService.persist(graphModel);

            run.setCompletedAt(Instant.now());
            run.setStatus(CompiledAnalysisRunStatus.SUCCEEDED);
            run.setStatusMessage("Analysis completed");
            run.setOutputDirectory(outputDir.toString());
            run.setEntityCount((long) graphModel.getClasses().size());
            run.setEndpointCount((long) graphModel.getEndpoints().size());
            run.setDependencyCount((long) graphModel.getDependencyEdges().size());
            run.setSequenceCount((long) graphModel.getSequences().size());
            run.setDurationMillis(Duration.between(start, run.getCompletedAt()).toMillis());
            run.setClasspath(classpath.getClasspathString());
            runRepository.save(run);

            return new CompiledAnalysisResult(run, outputs, graphModel);
        } catch (Exception ex) {
            run.setCompletedAt(Instant.now());
            run.setStatus(CompiledAnalysisRunStatus.FAILED);
            run.setStatusMessage(ex.getMessage());
            runRepository.save(run);
            if (ex instanceof IOException ioException) {
                throw ioException;
            }
            if (ex instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(ex);
        }
    }

    public CompiledAnalysisRun getRun(UUID id) {
        return runRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Analysis run not found for id " + id));
    }

    public List<ExportedFile> listExports(UUID runId) throws IOException {
        CompiledAnalysisRun run = getRun(runId);
        if (run.getOutputDirectory() == null) {
            return List.of();
        }
        Path outputDir = Path.of(run.getOutputDirectory());
        if (!Files.isDirectory(outputDir)) {
            return List.of();
        }
        try (var stream = Files.list(outputDir)) {
            return stream.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(Path::getFileName))
                    .map(path -> {
                        try {
                            return new ExportedFile(
                                    path.getFileName().toString(),
                                    Files.size(path),
                                    path);
                        } catch (IOException e) {
                            return new ExportedFile(path.getFileName().toString(), 0, path);
                        }
                    })
                    .collect(Collectors.toList());
        }
    }

    public Path resolveExportFile(UUID runId, String fileName) {
        CompiledAnalysisRun run = getRun(runId);
        if (run.getOutputDirectory() == null) {
            throw new IllegalArgumentException("Run has no output directory");
        }
        Path outputDir = Path.of(run.getOutputDirectory()).toAbsolutePath().normalize();
        Path resolved = outputDir.resolve(fileName).normalize();
        if (!resolved.startsWith(outputDir)) {
            throw new IllegalArgumentException("Invalid file name");
        }
        if (!Files.exists(resolved)) {
            throw new IllegalArgumentException("File not found: " + fileName);
        }
        return resolved;
    }

    public java.util.Optional<CompiledAnalysisResult> findLatestByProject(Long projectId) {
        if (projectId == null) {
            return java.util.Optional.empty();
        }
        return runRepository.findTopByProjectIdOrderByStartedAtDesc(projectId)
                .map(run -> new CompiledAnalysisResult(run, null, null));
    }

    private GraphModel buildSourceGraph(Path repoPath, List<String> acceptPackages) {
        BuildMetadataExtractor.BuildMetadata metadata = buildMetadataExtractor.extract(repoPath);
        List<Path> moduleRoots = metadata.moduleRoots();
        List<ClassMetadataRecord> classes = javaSourceScanner.scan(repoPath, moduleRoots);
        GraphModel model = GraphModel.empty();
        for (ClassMetadataRecord record : classes) {
            if (!GraphModel.isUserPackage(record.fullyQualifiedName(), acceptPackages)) {
                continue;
            }
            ClassNode node = new ClassNode();
            node.setName(record.fullyQualifiedName());
            node.setPackageName(record.packageName());
            node.setSimpleName(record.className());
            if (record.stereotype() != null) {
                node.setStereotypes(Collections.singletonList(record.stereotype()));
            }
            node.setOrigin(Origin.SOURCE);
            model.addClass(node);
        }
        return model;
    }

    private List<String> effectivePackages(List<String> override) {
        if (override == null || override.isEmpty()) {
            return new ArrayList<>(properties.getAcceptPackages());
        }
        return override;
    }

    public record CompiledAnalysisParameters(
            Path repoPath, List<String> acceptPackages, Boolean includeDependencies, Long projectId) {}
}
