package com.codevision.codevisionbackend.project;

import com.codevision.codevisionbackend.api.ApiModelMapper;
import com.codevision.codevisionbackend.api.generated.SecurityApi;
import com.codevision.codevisionbackend.api.model.ProjectLoggerInsightsResponse;
import com.codevision.codevisionbackend.api.model.ProjectPiiPciResponse;
import com.codevision.codevisionbackend.project.security.SecurityExportService;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProjectSecurityController implements SecurityApi {

    private static final Logger log = LoggerFactory.getLogger(ProjectSecurityController.class);
    private static final MediaType TEXT_CSV = MediaType.parseMediaType("text/csv");

    private final ProjectSnapshotService projectSnapshotService;
    private final ApiModelMapper apiModelMapper;
    private final SecurityExportService securityExportService;

    public ProjectSecurityController(
            ProjectSnapshotService projectSnapshotService,
            ApiModelMapper apiModelMapper,
            SecurityExportService securityExportService) {
        this.projectSnapshotService = projectSnapshotService;
        this.apiModelMapper = apiModelMapper;
        this.securityExportService = securityExportService;
    }

    @Override
    public ResponseEntity<ProjectLoggerInsightsResponse> getProjectLoggerInsights(
            @PathVariable("projectId") Long projectId) {
        log.info("Fetching logger insights for project id={}", projectId);
        return projectSnapshotService.fetchSnapshot(projectId)
                .map(snapshot -> apiModelMapper.toLoggerInsightsResponse(projectId, snapshot.loggerInsights()))
                .map(response -> {
                    log.info("Returning {} logger insights for project id={}",
                            Optional.ofNullable(response.getLoggerInsights()).map(List::size).orElse(0),
                            projectId);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    log.warn("No logger insights found for project id={}", projectId);
                    return ResponseEntity.notFound().build();
                });
    }

    @Override
    public ResponseEntity<ProjectPiiPciResponse> getProjectPiiPciFindings(@PathVariable("projectId") Long projectId) {
        log.info("Fetching PCI/PII findings for project id={}", projectId);
        return projectSnapshotService.fetchSnapshot(projectId)
                .map(snapshot -> apiModelMapper.toPiiPciResponse(projectId, snapshot.piiPciScan()))
                .map(response -> {
                    log.info("Returning {} PCI/PII findings for project id={}",
                            Optional.ofNullable(response.getFindings()).map(List::size).orElse(0),
                            projectId);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    log.warn("No PCI/PII findings found for project id={}", projectId);
                    return ResponseEntity.notFound().build();
                });
    }

    @Override
    public ResponseEntity<Resource> exportProjectLogsCsv(@PathVariable("projectId") Long projectId) {
        return projectSnapshotService.fetchSnapshot(projectId)
                .map(snapshot -> buildFileResponse(
                        securityExportService.buildLoggerCsv(snapshot.loggerInsights()),
                        TEXT_CSV,
                        securityExportService.buildFileName("logs", "csv", projectId)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<Resource> exportProjectLogsPdf(@PathVariable("projectId") Long projectId) {
        return projectSnapshotService.fetchSnapshot(projectId)
                .map(snapshot -> buildFileResponse(
                        securityExportService.buildLoggerPdf(snapshot.projectName(), snapshot.loggerInsights()),
                        MediaType.APPLICATION_PDF,
                        securityExportService.buildFileName("logs", "pdf", projectId)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<Resource> exportProjectPiiCsv(@PathVariable("projectId") Long projectId) {
        return projectSnapshotService.fetchSnapshot(projectId)
                .map(snapshot -> buildFileResponse(
                        securityExportService.buildPiiCsv(snapshot.piiPciScan()),
                        TEXT_CSV,
                        securityExportService.buildFileName("pii", "csv", projectId)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<Resource> exportProjectPiiPdf(@PathVariable("projectId") Long projectId) {
        return projectSnapshotService.fetchSnapshot(projectId)
                .map(snapshot -> buildFileResponse(
                        securityExportService.buildPiiPdf(snapshot.projectName(), snapshot.piiPciScan()),
                        MediaType.APPLICATION_PDF,
                        securityExportService.buildFileName("pii", "pdf", projectId)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private ResponseEntity<Resource> buildFileResponse(byte[] data, MediaType mediaType, String fileName) {
        if (data == null || data.length == 0) {
            return ResponseEntity.noContent().build();
        }
        ByteArrayResource resource = new ByteArrayResource(data);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentLength(data.length)
                .body(resource);
    }
}
