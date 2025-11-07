package com.codevision.codevisionbackend.project.diagram;

import static org.assertj.core.api.Assertions.assertThat;

import com.codevision.codevisionbackend.analyze.DiagramSummary;
import com.codevision.codevisionbackend.project.Project;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DiagramServiceTest {

    private DiagramService diagramService;

    @BeforeEach
    void setUp() {
        diagramService = new DiagramService(
                Mockito.mock(DiagramRepository.class),
                Mockito.mock(DiagramStorageService.class),
                Mockito.mock(DiagramSvgRenderer.class),
                new ObjectMapper());
    }

    @Test
    void parseMetadataHandlesNullAndBlankInputs() {
        assertThat(diagramService.parseMetadata(null)).isEmpty();
        assertThat(diagramService.parseMetadata("   ")).isEmpty();
    }

    @Test
    void parseMetadataReturnsImmutableCopy() {
        Map<String, Object> metadata = diagramService.parseMetadata("{\"path\":\"/demo\",\"httpMethod\":\"GET\"}");
        assertThat(metadata).containsEntry("path", "/demo").containsEntry("httpMethod", "GET");
    }

    @Test
    void readMetadataReturnsEmptyMapOnInvalidJson() {
        Diagram diagram = new Diagram();
        diagram.setMetadataJson("{not-json");

        assertThat(diagramService.readMetadata(diagram)).isEmpty();
    }

    @Test
    void readMetadataDeserializesJson() {
        Diagram diagram = new Diagram();
        diagram.setMetadataJson("{\"operation\":\"/demo\"}");

        assertThat(diagramService.readMetadata(diagram)).containsEntry("operation", "/demo");
    }

    @Test
    void toSummaryIncludesMetadataAndPaths() {
        Diagram diagram = new Diagram();
        diagram.setId(15L);
        diagram.setProject(new Project());
        diagram.setDiagramType(DiagramType.SEQUENCE);
        diagram.setTitle("Sequence diagram");
        diagram.setPlantumlSource("@startuml@enduml");
        diagram.setMermaidSource("sequenceDiagram");
        diagram.setSvgPath("project-1/sequence/diagram.svg");
        diagram.setMetadataJson("{\"includeExternal\":true}");

        DiagramSummary summary = diagramService.toSummary(diagram);

        assertThat(summary.diagramId()).isEqualTo(15L);
        assertThat(summary.diagramType()).isEqualTo("SEQUENCE");
        assertThat(summary.metadata()).containsEntry("includeExternal", true);
        assertThat(summary.svgPath()).isEqualTo("project-1/sequence/diagram.svg");
    }
}
