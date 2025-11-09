package com.codevision.codevisionbackend.project.diagram;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class DiagramSvgRendererTest {

    private final DiagramSvgRenderer renderer = new DiagramSvgRenderer();

    @Test
    void rendersSvgForValidPlantUmlSource() {
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
        assertThat(renderer.render(null)).isEmpty();
        assertThat(renderer.render("   ")).isEmpty();
    }
}
