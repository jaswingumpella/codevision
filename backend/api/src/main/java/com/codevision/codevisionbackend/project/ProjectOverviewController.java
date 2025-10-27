package com.codevision.codevisionbackend.project;

import com.codevision.codevisionbackend.api.ApiModelMapper;
import com.codevision.codevisionbackend.api.generated.ProjectApi;
import com.codevision.codevisionbackend.api.model.ParsedDataResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProjectOverviewController implements ProjectApi {

    private static final Logger log = LoggerFactory.getLogger(ProjectOverviewController.class);

    private final ProjectSnapshotService projectSnapshotService;
    private final ApiModelMapper apiModelMapper;

    public ProjectOverviewController(ProjectSnapshotService projectSnapshotService, ApiModelMapper apiModelMapper) {
        this.projectSnapshotService = projectSnapshotService;
        this.apiModelMapper = apiModelMapper;
    }

    @Override
    public ResponseEntity<ParsedDataResponse> getProjectOverview(@PathVariable("projectId") Long projectId) {
        log.info("Fetching project overview for id={}", projectId);
        return projectSnapshotService.fetchSnapshot(projectId)
                .map(apiModelMapper::toParsedDataResponse)
                .map(response -> {
                    log.info("Found snapshot for project id={}", projectId);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    log.warn("No snapshot found for project id={}", projectId);
                    return ResponseEntity.notFound().build();
                });
    }

    @Override
    public ResponseEntity<com.codevision.codevisionbackend.api.model.ProjectApiEndpointsResponse> getProjectApiEndpoints(
            @PathVariable("projectId") Long projectId) {
        log.info("Fetching API endpoints for project id={}", projectId);
        return projectSnapshotService.fetchSnapshot(projectId)
                .map(snapshot -> apiModelMapper.toApiEndpointsResponse(projectId, snapshot.apiEndpoints()))
                .map(response -> {
                    log.info("Found {} endpoints for project id={}",
                            response.getEndpoints() != null ? response.getEndpoints().size() : 0,
                            projectId);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    log.warn("No endpoint catalog found for project id={}", projectId);
                    return ResponseEntity.notFound().build();
                });
    }
}
