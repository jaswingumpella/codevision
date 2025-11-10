package com.codevision.codevisionbackend.analysis.web;

import com.codevision.codevisionbackend.analysis.CompiledAnalysisResult;
import com.codevision.codevisionbackend.analysis.CompiledAnalysisService;
import com.codevision.codevisionbackend.analysis.CompiledAnalysisService.CompiledAnalysisParameters;
import com.codevision.codevisionbackend.analysis.ExportedFile;
import com.codevision.codevisionbackend.analysis.persistence.AnalysisEntityRecord;
import com.codevision.codevisionbackend.analysis.persistence.AnalysisEntityRepository;
import com.codevision.codevisionbackend.analysis.persistence.CompiledEndpointRecord;
import com.codevision.codevisionbackend.analysis.persistence.CompiledEndpointRepository;
import com.codevision.codevisionbackend.analysis.persistence.SequenceRecord;
import com.codevision.codevisionbackend.analysis.persistence.SequenceRecordRepository;
import com.codevision.codevisionbackend.analysis.web.model.CompiledAnalysisRequest;
import com.codevision.codevisionbackend.analysis.web.model.CompiledAnalysisResponse;
import com.codevision.codevisionbackend.analysis.web.model.EndpointSummary;
import com.codevision.codevisionbackend.analysis.web.model.EntitySummary;
import com.codevision.codevisionbackend.analysis.web.model.ExportFileResponse;
import com.codevision.codevisionbackend.analysis.web.model.PageResponse;
import com.codevision.codevisionbackend.analysis.web.model.SequenceSummary;
import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api")
public class AnalysisController {

    private final CompiledAnalysisService compiledAnalysisService;
    private final AnalysisEntityRepository analysisEntityRepository;
    private final SequenceRecordRepository sequenceRecordRepository;
    private final CompiledEndpointRepository compiledEndpointRepository;

    public AnalysisController(
            CompiledAnalysisService compiledAnalysisService,
            AnalysisEntityRepository analysisEntityRepository,
            SequenceRecordRepository sequenceRecordRepository,
            CompiledEndpointRepository compiledEndpointRepository) {
        this.compiledAnalysisService = compiledAnalysisService;
        this.analysisEntityRepository = analysisEntityRepository;
        this.sequenceRecordRepository = sequenceRecordRepository;
        this.compiledEndpointRepository = compiledEndpointRepository;
    }

    @PostMapping("/analyze")
    public ResponseEntity<CompiledAnalysisResponse> runCompiledAnalysis(
            @Valid @RequestBody CompiledAnalysisRequest request) throws IOException {
        Path repoPath = Path.of(request.getRepoPath());
        CompiledAnalysisParameters parameters =
                new CompiledAnalysisParameters(repoPath, request.getAcceptPackages(), request.getIncludeDependencies());
        CompiledAnalysisResult result = compiledAnalysisService.analyze(parameters);
        CompiledAnalysisResponse response = toResponse(result);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/analyze/{id}/exports")
    public ResponseEntity<List<ExportFileResponse>> listExports(@PathVariable UUID id) throws IOException {
        List<ExportedFile> files = compiledAnalysisService.listExports(id);
        List<ExportFileResponse> responses = files.stream()
                .map(file -> new ExportFileResponse(file.name(), file.size(), buildDownloadUrl(id, file.name())))
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/analyze/{id}/exports/{fileName:.+}")
    public ResponseEntity<InputStreamResource> downloadExport(
            @PathVariable UUID id, @PathVariable String fileName) throws IOException {
        Path file = compiledAnalysisService.resolveExportFile(id, fileName);
        MediaType mediaType = determineMediaType(fileName);
        InputStreamResource resource = new InputStreamResource(Files.newInputStream(file));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + "\"")
                .contentType(mediaType)
                .body(resource);
    }

    @GetMapping("/entities")
    public ResponseEntity<PageResponse<EntitySummary>> listEntities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String packageFilter) {
        var pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("className"));
        var pageResult = (packageFilter == null || packageFilter.isBlank())
                ? analysisEntityRepository.findAll(pageable)
                : analysisEntityRepository.findByPackageNameStartingWithIgnoreCase(packageFilter, pageable);
        List<EntitySummary> summaries = pageResult.stream()
                .map(this::toEntitySummary)
                .toList();
        return ResponseEntity.ok(new PageResponse<>(summaries, pageResult.getTotalElements(), page, size));
    }

    @GetMapping("/sequences")
    public ResponseEntity<PageResponse<SequenceSummary>> listSequences(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) {
        var pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("generatorName"));
        var pageResult = sequenceRecordRepository.findAll(pageable);
        List<SequenceSummary> summaries = pageResult.stream()
                .map(this::toSequenceSummary)
                .toList();
        return ResponseEntity.ok(new PageResponse<>(summaries, pageResult.getTotalElements(), page, size));
    }

    @GetMapping("/endpoints")
    public ResponseEntity<PageResponse<EndpointSummary>> listEndpoints(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) {
        var pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("path"));
        var pageResult = compiledEndpointRepository.findAll(pageable);
        List<EndpointSummary> summaries = pageResult.stream()
                .map(this::toEndpointSummary)
                .toList();
        return ResponseEntity.ok(new PageResponse<>(summaries, pageResult.getTotalElements(), page, size));
    }

    private CompiledAnalysisResponse toResponse(CompiledAnalysisResult result) throws IOException {
        CompiledAnalysisResponse response = new CompiledAnalysisResponse();
        response.setAnalysisId(result.run().getId());
        response.setRepoPath(result.run().getRepoPath());
        response.setStartedAt(result.run().getStartedAt());
        response.setCompletedAt(result.run().getCompletedAt());
        response.setStatus(result.run().getStatus().name());
        response.setStatusMessage(result.run().getStatusMessage());
        response.setOutputDirectory(result.outputs().getRootDirectory().toString());
        response.setEntityCount(result.run().getEntityCount());
        response.setEndpointCount(result.run().getEndpointCount());
        response.setDependencyCount(result.run().getDependencyCount());
        response.setSequenceCount(result.run().getSequenceCount());
        response.setDurationMillis(result.run().getDurationMillis());
        List<ExportFileResponse> exports = result.outputs().allFiles().stream()
                .distinct()
                .map(path -> {
                    long size;
                    try {
                        size = Files.size(path);
                    } catch (IOException e) {
                        size = 0;
                    }
                    return new ExportFileResponse(
                            path.getFileName().toString(),
                            size,
                            buildDownloadUrl(result.run().getId(), path.getFileName().toString()));
                })
                .toList();
        response.setExports(exports);
        return response;
    }

    private EntitySummary toEntitySummary(AnalysisEntityRecord record) {
        EntitySummary summary = new EntitySummary();
        summary.setClassName(record.getClassName());
        summary.setPackageName(record.getPackageName());
        summary.setTableName(record.getTableName());
        summary.setOrigin(record.getOrigin().name());
        summary.setSccId(record.getSccId());
        summary.setInCycle(record.isInCycle());
        return summary;
    }

    private SequenceSummary toSequenceSummary(SequenceRecord record) {
        SequenceSummary summary = new SequenceSummary();
        summary.setGeneratorName(record.getGeneratorName());
        summary.setSequenceName(record.getSequenceName());
        summary.setAllocationSize(record.getAllocationSize());
        summary.setInitialValue(record.getInitialValue());
        return summary;
    }

    private EndpointSummary toEndpointSummary(CompiledEndpointRecord record) {
        EndpointSummary summary = new EndpointSummary();
        summary.setType(record.getType().name());
        summary.setHttpMethod(record.getHttpMethod());
        summary.setPath(record.getPath());
        summary.setControllerClass(record.getControllerClass());
        summary.setControllerMethod(record.getControllerMethod());
        summary.setFramework(record.getFramework());
        return summary;
    }

    private MediaType determineMediaType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".json")) {
            return MediaType.APPLICATION_JSON;
        }
        if (lower.endsWith(".csv")) {
            return new MediaType("text", "csv");
        }
        return MediaType.TEXT_PLAIN;
    }

    private String buildDownloadUrl(UUID runId, String fileName) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/analyze/{id}/exports/{file}")
                .buildAndExpand(runId, fileName)
                .toUriString();
    }
}
