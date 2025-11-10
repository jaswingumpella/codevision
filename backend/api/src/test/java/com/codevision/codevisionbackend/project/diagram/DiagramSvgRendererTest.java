package com.codevision.codevisionbackend.project.diagram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class DiagramSvgRendererTest {

    private static final String ENABLE_RENDER_TESTS = "codevision.enableDiagramRendererTest";

    @Test
    void rendersSvgForValidPlantUmlSource() {
        assumeTrue(Boolean.getBoolean(ENABLE_RENDER_TESTS), "PlantUML rendering disabled for tests");
        DiagramSvgRenderer renderer = new DiagramSvgRenderer(true);

        String plantUml = """
                @startuml
                Alice -> Bob: Hello
                @enduml
                """;

        byte[] svgBytes = renderer.render(plantUml).orElseThrow();

        String svg = new String(svgBytes, StandardCharsets.UTF_8);
        assertThat(svg).contains("<svg").contains("Alice").contains("Bob");
    }

    @Test
    void returnsEmptyForBlankSource() {
        DiagramSvgRenderer renderer = new DiagramSvgRenderer(false);
        assertThat(renderer.render(null)).isEmpty();
        assertThat(renderer.render("   ")).isEmpty();
    }
}
