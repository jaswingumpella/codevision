package com.codevision.codevisionbackend.graph.export;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for SVG export canvas and node dimensions.
 */
@ConfigurationProperties(prefix = "graph.export.svg")
public record SvgExportProperties(
        int canvasWidth,
        int canvasHeight,
        int nodeRadius,
        int rectWidth,
        int rectHeight
) {
    public SvgExportProperties() {
        this(1200, 900, 20, 80, 30);
    }
}
