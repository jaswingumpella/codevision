package com.codevision.codevisionbackend.project;

import com.codevision.codevisionbackend.api.ApiModelMapper;
import com.codevision.codevisionbackend.api.generated.ProjectApi;
import com.codevision.codevisionbackend.api.model.ParsedDataResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProjectOverviewController implements ProjectApi {

    private final ProjectSnapshotService projectSnapshotService;
    private final ApiModelMapper apiModelMapper;

    public ProjectOverviewController(ProjectSnapshotService projectSnapshotService, ApiModelMapper apiModelMapper) {
        this.projectSnapshotService = projectSnapshotService;
        this.apiModelMapper = apiModelMapper;
    }

    @Override
    public ResponseEntity<ParsedDataResponse> getProjectOverview(@PathVariable("projectId") Long projectId) {
        return projectSnapshotService.fetchSnapshot(projectId)
                .map(apiModelMapper::toParsedDataResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
