package com.codevision.codevisionbackend.project;

import com.codevision.codevisionbackend.api.ApiModelMapper;
import com.codevision.codevisionbackend.api.generated.ProjectApi;
import com.codevision.codevisionbackend.api.model.ProjectSnapshotsResponse;
import com.codevision.codevisionbackend.api.model.SnapshotDiff;
import com.codevision.codevisionbackend.project.ProjectSnapshotService.ProjectSnapshotSummary;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProjectSnapshotController implements ProjectApi {

    private static final Logger log = LoggerFactory.getLogger(ProjectSnapshotController.class);

    private final ProjectSnapshotService projectSnapshotService;
    private final ApiModelMapper apiModelMapper;

    public ProjectSnapshotController(ProjectSnapshotService projectSnapshotService, ApiModelMapper apiModelMapper) {
        this.projectSnapshotService = projectSnapshotService;
        this.apiModelMapper = apiModelMapper;
    }

    @Override
    public ResponseEntity<ProjectSnapshotsResponse> listProjectSnapshots(@PathVariable("projectId") Long projectId) {
        List<ProjectSnapshotSummary> summaries = projectSnapshotService.listSnapshots(projectId);
        if (summaries == null || summaries.isEmpty()) {
            log.warn("No snapshots available for project {}", projectId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(apiModelMapper.toSnapshotList(projectId, summaries));
    }

    @Override
    public ResponseEntity<SnapshotDiff> diffProjectSnapshots(
            Long projectId, Long snapshotId, Long compareSnapshotId) {
        try {
            com.codevision.codevisionbackend.project.SnapshotDiff diff =
                    projectSnapshotService.diff(projectId, snapshotId, compareSnapshotId);
            return ResponseEntity.ok(apiModelMapper.toSnapshotDiff(diff));
        } catch (IllegalArgumentException ex) {
            log.warn("Unable to diff snapshots {} and {} for project {}: {}",
                    snapshotId, compareSnapshotId, projectId, ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
