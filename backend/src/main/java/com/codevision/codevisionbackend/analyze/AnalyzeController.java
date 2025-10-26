package com.codevision.codevisionbackend.analyze;

import com.codevision.codevisionbackend.git.GitCloneService;
import com.codevision.codevisionbackend.project.Project;
import com.codevision.codevisionbackend.project.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analyze")
public class AnalyzeController {

    private final GitCloneService gitCloneService;
    private final ProjectService projectService;

    public AnalyzeController(GitCloneService gitCloneService, ProjectService projectService) {
        this.gitCloneService = gitCloneService;
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<AnalyzeResponse> analyze(@Valid @RequestBody AnalyzeRequest request) {
        var cloneResult = gitCloneService.cloneRepository(request.getRepoUrl());
        Project project = projectService.overwriteProject(request.getRepoUrl(), cloneResult.projectName());
        return ResponseEntity.ok(new AnalyzeResponse(project.getId(), "ANALYZED_BASE"));
    }
}
