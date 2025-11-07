package com.codevision.codevisionbackend.project.diagram;

import com.codevision.codevisionbackend.analyze.DiagramSummary;
import com.codevision.codevisionbackend.analyze.diagram.DiagramDefinition;
import com.codevision.codevisionbackend.project.Project;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DiagramService {

    private static final Logger log = LoggerFactory.getLogger(DiagramService.class);
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final DiagramRepository diagramRepository;
    private final DiagramStorageService storageService;
    private final DiagramSvgRenderer svgRenderer;
    private final ObjectMapper objectMapper;

    public DiagramService(
            DiagramRepository diagramRepository,
            DiagramStorageService storageService,
            DiagramSvgRenderer svgRenderer,
            ObjectMapper objectMapper) {
        this.diagramRepository = diagramRepository;
        this.storageService = storageService;
        this.svgRenderer = svgRenderer;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public List<Diagram> replaceProjectDiagrams(Project project, List<DiagramDefinition> definitions) {
        if (project == null || project.getId() == null) {
            throw new IllegalArgumentException("Project must be persisted before saving diagrams");
        }
        log.debug(
                "Replacing {} diagrams for projectId={}",
                definitions != null ? definitions.size() : 0,
                project.getId());
        purgeProjectDiagrams(project);
        if (definitions == null || definitions.isEmpty()) {
            return List.of();
        }
        List<Diagram> entities = new ArrayList<>(definitions.size());
        for (int index = 0; index < definitions.size(); index++) {
            DiagramDefinition definition = definitions.get(index);
            if (definition == null) {
                continue;
            }
            Diagram diagram = toEntity(project, definition, index);
            byte[] svg = svgRenderer.render(definition.plantumlSource()).orElse(null);
            if (svg != null) {
                String relativePath = storageService.storeSvg(project.getId(), definition.type(), index, svg);
                diagram.setSvgPath(relativePath);
            }
            entities.add(diagram);
        }
        List<Diagram> persisted = diagramRepository.saveAll(entities);
        log.info("Persisted {} diagrams for projectId={}", persisted.size(), project.getId());
        return persisted;
    }

    @Transactional
    public void purgeProjectDiagrams(Project project) {
        if (project == null || project.getId() == null) {
            return;
        }
        List<Diagram> existing = diagramRepository.findByProjectIdOrderByDiagramTypeAscSequenceOrderAscTitleAsc(
                project.getId());
        existing.forEach(diagram -> storageService.deleteSvg(diagram.getSvgPath()));
        diagramRepository.deleteByProject(project);
        storageService.purgeProject(project.getId());
    }

    @Transactional
    public List<Diagram> listProjectDiagrams(Long projectId) {
        if (projectId == null) {
            return List.of();
        }
        return diagramRepository.findByProjectIdOrderByDiagramTypeAscSequenceOrderAscTitleAsc(projectId);
    }

    @Transactional
    public Optional<Diagram> findDiagram(Long projectId, Long diagramId) {
        if (projectId == null || diagramId == null) {
            return Optional.empty();
        }
        return diagramRepository.findByProjectIdAndId(projectId, diagramId);
    }

    public byte[] loadSvg(Diagram diagram) {
        if (diagram == null) {
            return null;
        }
        return storageService.loadSvg(diagram.getSvgPath());
    }

    public Map<String, Object> readMetadata(Diagram diagram) {
        if (diagram == null || diagram.getMetadataJson() == null || diagram.getMetadataJson().isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(diagram.getMetadataJson(), MAP_TYPE);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize metadata for diagram {}", diagram.getId(), e);
            return Map.of();
        }
    }

    private Diagram toEntity(Project project, DiagramDefinition definition, int sequence) {
        Diagram diagram = new Diagram();
        diagram.setProject(project);
        diagram.setDiagramType(definition.type());
        diagram.setTitle(definition.title());
        diagram.setSequenceOrder(sequence);
        diagram.setPlantumlSource(definition.plantumlSource());
        diagram.setMermaidSource(definition.mermaidSource());
        diagram.setMetadataJson(writeMetadata(definition.metadata()));
        return diagram;
    }

    private String writeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize diagram metadata", e);
            return null;
        }
    }

    public Map<String, Object> parseMetadata(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse diagram metadata json", e);
            return Collections.emptyMap();
        }
    }

    public DiagramSummary toSummary(Diagram diagram) {
        if (diagram == null) {
            return null;
        }
        return new DiagramSummary(
                diagram.getId(),
                diagram.getDiagramType().name(),
                diagram.getTitle(),
                diagram.getPlantumlSource(),
                diagram.getMermaidSource(),
                diagram.getSvgPath(),
                readMetadata(diagram));
    }
}
