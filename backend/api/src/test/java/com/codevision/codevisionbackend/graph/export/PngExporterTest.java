package com.codevision.codevisionbackend.graph.export;

import com.codevision.codevisionbackend.graph.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

class PngExporterTest {

    private final SvgExporter svgExporter = new SvgExporter(new SvgExportProperties());
    private final PngExporter exporter = new PngExporter(svgExporter);

    private static KgNode node(String id, KgNodeType type, String name) {
        return new KgNode(id, type, name, "com.app." + name,
                new NodeMetadata("public", Set.of(), emptyList(), emptyList(), "void",
                        emptyList(), emptyList(), null, 0, 0, 0, null, null, 0, 0, null),
                null, "SOURCE", new Provenance("test", "Test.java", 1, ConfidenceLevel.EXTRACTED));
    }

    private static KgEdge edge(String id, KgEdgeType type, String src, String tgt) {
        return new KgEdge(id, type, src, tgt, type.name(),
                ConfidenceLevel.EXTRACTED, new Provenance("test", "Test.java", 1, ConfidenceLevel.EXTRACTED), Map.of());
    }

    @Nested
    class Given_Metadata {

        @Nested
        class When_QueryingFormat {

            @Test
            void Then_ReturnsPng() {
                assertThat(exporter.formatName()).isEqualTo("png");
                assertThat(exporter.fileExtension()).isEqualTo(".png");
                assertThat(exporter.contentType()).isEqualTo("image/png");
            }
        }
    }

    @Nested
    class Given_GraphWithNodes {

        @Nested
        class When_Exporting {

            @Test
            void Then_ProducesPngWithMagicBytes() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("n1", KgNodeType.CLASS, "Foo"));
                graph.addNode(node("n2", KgNodeType.METHOD, "bar"));
                graph.addEdge(edge("e1", KgEdgeType.DECLARES, "n1", "n2"));

                var result = exporter.export(graph);

                // PNG magic bytes: 0x89 P N G \r \n 0x1A \n
                assertThat(result).hasSizeGreaterThan(8);
                assertThat(result[0]).isEqualTo((byte) 0x89);
                assertThat(result[1]).isEqualTo((byte) 'P');
                assertThat(result[2]).isEqualTo((byte) 'N');
                assertThat(result[3]).isEqualTo((byte) 'G');
                assertThat(result[4]).isEqualTo((byte) '\r');
                assertThat(result[5]).isEqualTo((byte) '\n');
                assertThat(result[6]).isEqualTo((byte) 0x1A);
                assertThat(result[7]).isEqualTo((byte) '\n');
            }

            @Test
            void Then_ProducesNonEmptyOutput() {
                var graph = new KnowledgeGraph();
                graph.addNode(node("n1", KgNodeType.CLASS, "Foo"));
                graph.addNode(node("n2", KgNodeType.METHOD, "bar"));
                graph.addEdge(edge("e1", KgEdgeType.DECLARES, "n1", "n2"));

                var result = exporter.export(graph);

                assertThat(result).isNotEmpty();
                assertThat(result.length).isGreaterThan(100);
            }
        }
    }

    @Nested
    class Given_EmptyGraph {

        @Nested
        class When_Exporting {

            @Test
            void Then_StillProducesPng() {
                var graph = new KnowledgeGraph();

                var result = exporter.export(graph);

                assertThat(result).isNotEmpty();
                // Still has PNG magic bytes
                assertThat(result[0]).isEqualTo((byte) 0x89);
                assertThat(result[1]).isEqualTo((byte) 'P');
                assertThat(result[2]).isEqualTo((byte) 'N');
                assertThat(result[3]).isEqualTo((byte) 'G');
            }
        }
    }
}
