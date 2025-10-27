package com.codevision.codevisionbackend.project;

import com.codevision.codevisionbackend.analyze.ParsedDataResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/project")
public class ProjectOverviewController {

    private final ProjectSnapshotService projectSnapshotService;

    public ProjectOverviewController(ProjectSnapshotService projectSnapshotService) {
        this.projectSnapshotService = projectSnapshotService;
    }

    @GetMapping("/{projectId}/overview")
    public ResponseEntity<ParsedDataResponse> getOverview(@PathVariable Long projectId) {
        return projectSnapshotService.fetchSnapshot(projectId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
