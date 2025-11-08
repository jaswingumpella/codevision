package com.codevision.codevisionbackend.project;

import com.codevision.codevisionbackend.api.ApiModelMapper;
import com.codevision.codevisionbackend.api.generated.ExportApi;
import com.codevision.codevisionbackend.project.export.ExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProjectExportController implements ExportApi {

    private static final Logger log = LoggerFactory.getLogger(ProjectExportController.class);

    private final ProjectSnapshotService projectSnapshotService;
    private final ApiModelMapper apiModelMapper;
    private final ExportService exportService;

    public ProjectExportController(
            ProjectSnapshotService projectSnapshotService,
            ApiModelMapper apiModelMapper,
            ExportService exportService) {
        this.projectSnapshotService = projectSnapshotService;
        this.apiModelMapper = apiModelMapper;
        this.exportService = exportService;
    }

    @Override
    public ResponseEntity<String> exportProjectHtml(@PathVariable("projectId") Long projectId) {
        log.info("Generating HTML export for project {}", projectId);
        var snapshot = projectSnapshotService.fetchSnapshot(projectId);
        if (snapshot.isEmpty()) {
            log.warn("No snapshot found for project {} when exporting HTML", projectId);
            return ResponseEntity.<String>notFound().build();
        }
        String html = exportService.buildConfluenceHtml(snapshot.get());
        if (html == null || html.isBlank()) {
            log.warn("Snapshot exists for project {} but HTML export is empty", projectId);
            return ResponseEntity.<String>noContent().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE + "; charset=UTF-8")
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"project-%d.html\"".formatted(projectId))
                .body(html);
    }

    @Override
    public ResponseEntity<com.codevision.codevisionbackend.api.model.ParsedDataResponse> exportProjectSnapshot(
            @PathVariable("projectId") Long projectId) {
        log.info("Downloading snapshot JSON for project {}", projectId);
        return projectSnapshotService.fetchSnapshot(projectId)
                .map(apiModelMapper::toParsedDataResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    log.warn("No snapshot found for project {}", projectId);
                    return ResponseEntity.<com.codevision.codevisionbackend.api.model.ParsedDataResponse>notFound().build();
                });
    }
}
