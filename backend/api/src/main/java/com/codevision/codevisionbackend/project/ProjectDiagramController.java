package com.codevision.codevisionbackend.project;

import com.codevision.codevisionbackend.analyze.DiagramSummary;
import com.codevision.codevisionbackend.api.ApiModelMapper;
import com.codevision.codevisionbackend.api.generated.DiagramApi;
import com.codevision.codevisionbackend.api.model.ProjectDiagramsResponse;
import com.codevision.codevisionbackend.project.diagram.DiagramService;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProjectDiagramController implements DiagramApi {

    private static final Logger log = LoggerFactory.getLogger(ProjectDiagramController.class);

    private final DiagramService diagramService;
    private final ApiModelMapper apiModelMapper;

    public ProjectDiagramController(DiagramService diagramService, ApiModelMapper apiModelMapper) {
        this.diagramService = diagramService;
        this.apiModelMapper = apiModelMapper;
    }

    @Override
    public ResponseEntity<ProjectDiagramsResponse> getProjectDiagrams(@PathVariable("projectId") Long projectId) {
        log.info("Fetching diagrams for project id={}", projectId);
        List<DiagramSummary> diagrams = diagramService.listProjectDiagrams(projectId).stream()
                .map(diagramService::toSummary)
                .filter(Objects::nonNull)
                .toList();
        if (diagrams.isEmpty()) {
            log.warn("No diagrams found for project id={}", projectId);
            return ResponseEntity.notFound().build();
        }
        ProjectDiagramsResponse response = apiModelMapper.toProjectDiagramsResponse(projectId, diagrams);
        log.info("Returning {} diagrams for project id={}",
                response.getDiagrams() != null ? response.getDiagrams().size() : 0,
                projectId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Resource> getProjectDiagramSvg(
            @PathVariable("projectId") Long projectId, @PathVariable("diagramId") Long diagramId) {
        log.info("Fetching diagram svg for project id={} diagramId={}", projectId, diagramId);
        return diagramService.findDiagram(projectId, diagramId)
                .map(diagram -> {
                    byte[] svg = diagramService.loadSvg(diagram);
                    if (svg == null || svg.length == 0) {
                        log.warn("Diagram {} for project {} has no SVG content", diagramId, projectId);
                        return ResponseEntity.status(org.springframework.http.HttpStatus.NO_CONTENT).<Resource>build();
                    }
                    Resource resource = new ByteArrayResource(svg);
                    return ResponseEntity.ok()
                            .contentType(MediaType.valueOf("image/svg+xml"))
                            .contentLength(svg.length)
                            .body(resource);
                })
                .orElseGet(() -> {
                    log.warn("Diagram {} for project {} not found", diagramId, projectId);
                    return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).<Resource>build();
                });
    }
}
