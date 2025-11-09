package com.codevision.codevisionbackend.project;

import com.codevision.codevisionbackend.api.ApiModelMapper;
import com.codevision.codevisionbackend.api.generated.MetadataApi;
import com.codevision.codevisionbackend.api.model.ProjectMetadataResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProjectMetadataController implements MetadataApi {

    private static final Logger log = LoggerFactory.getLogger(ProjectMetadataController.class);

    private final ProjectSnapshotService projectSnapshotService;
    private final ApiModelMapper apiModelMapper;

    public ProjectMetadataController(ProjectSnapshotService projectSnapshotService, ApiModelMapper apiModelMapper) {
        this.projectSnapshotService = projectSnapshotService;
        this.apiModelMapper = apiModelMapper;
    }

    @Override
    public ResponseEntity<ProjectMetadataResponse> getProjectMetadata(@PathVariable("projectId") Long projectId) {
        log.info("Fetching metadata payload for project {}", projectId);
        return projectSnapshotService.fetchSnapshot(projectId)
                .map(snapshot -> apiModelMapper.toProjectMetadataResponse(projectId, snapshot))
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    log.warn("No metadata snapshot available for project {}", projectId);
                    return ResponseEntity.notFound().build();
                });
    }
}
