package com.codevision.codevisionbackend.project.diagram;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DiagramSvgRenderer {

    private static final Logger log = LoggerFactory.getLogger(DiagramSvgRenderer.class);

    public Optional<byte[]> render(String plantumlSource) {
        if (plantumlSource == null || plantumlSource.isBlank()) {
            return Optional.empty();
        }
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            SourceStringReader reader = new SourceStringReader(plantumlSource, StandardCharsets.UTF_8);
            var description = reader.outputImage(outputStream, new FileFormatOption(FileFormat.SVG));
            if (description == null) {
                return Optional.empty();
            }
            return Optional.of(outputStream.toByteArray());
        } catch (Exception e) {
            log.warn("Failed to render PlantUML diagram", e);
            return Optional.empty();
        }
    }
}
