package com.codevision.codevisionbackend.graph.export;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for PDF export column widths and layout.
 */
@ConfigurationProperties(prefix = "graph.export.pdf")
public record PdfExportProperties(
        int nodeNameWidth,
        int nodeTypeWidth,
        int nodeQualifiedNameWidth,
        int edgeTypeWidth,
        int edgeSourceWidth,
        int edgeTargetWidth,
        int edgeLabelWidth
) {
    public PdfExportProperties() {
        this(28, 18, 38, 18, 23, 23, 18);
    }
}
