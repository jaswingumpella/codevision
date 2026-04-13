package com.codevision.codevisionbackend.graph.export;

import com.codevision.codevisionbackend.graph.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GraphExportService")
class GraphExportServiceTest {

    private KnowledgeGraph buildTestGraph() {
        var graph = new KnowledgeGraph();
        graph.addNode(new KgNode("c1", KgNodeType.CLASS, "UserService", "com.app.UserService",
                new NodeMetadata("public", Set.of(), emptyList(), emptyList(), "void",
                        emptyList(), emptyList(), null, 0, 0, 0, null, null, 0, 0, null),
                null, "SOURCE", new Provenance("test", "Test.java", 1, ConfidenceLevel.EXTRACTED)));
        graph.addNode(new KgNode("m1", KgNodeType.METHOD, "findUser", "com.app.UserService.findUser",
                new NodeMetadata("public", Set.of(), emptyList(), emptyList(), "User",
                        emptyList(), emptyList(), null, 0, 0, 0, null, null, 0, 0, null),
                null, "SOURCE", new Provenance("test", "Test.java", 1, ConfidenceLevel.EXTRACTED)));
        graph.addEdge(new KgEdge("e1", KgEdgeType.DECLARES, "c1", "m1", "declares",
                ConfidenceLevel.EXTRACTED, new Provenance("test", "Test.java", 1, ConfidenceLevel.EXTRACTED), Map.of()));
        return graph;
    }

    private GraphExporter stubExporter(String name, String ext, byte[] output) {
        return new GraphExporter() {
            @Override public String formatName() { return name; }
            @Override public String fileExtension() { return ext; }
            @Override public byte[] export(KnowledgeGraph graph) { return output; }
        };
    }

    @Nested
    @DisplayName("Given registered exporters")
    class Given_RegisteredExporters {

        private final GraphExporter jsonExporter = stubExporter("json", ".json", "{\"nodes\":[]}".getBytes());
        private final GraphExporter csvExporter = stubExporter("csv", ".csv", "id,name\n".getBytes());
        private final GraphExportService service = new GraphExportService(List.of(jsonExporter, csvExporter));

        @Nested
        @DisplayName("When exporting by format")
        class When_ExportingByFormat {

            @Test
            @DisplayName("Then correct exporter called for json")
            void Then_CorrectExporterCalledForJson() {
                var graph = buildTestGraph();
                var result = service.export(graph, "json");

                assertThat(new String(result)).isEqualTo("{\"nodes\":[]}");
            }

            @Test
            @DisplayName("Then correct exporter called for csv")
            void Then_CorrectExporterCalledForCsv() {
                var graph = buildTestGraph();
                var result = service.export(graph, "csv");

                assertThat(new String(result)).isEqualTo("id,name\n");
            }
        }

        @Nested
        @DisplayName("When listing formats")
        class When_ListingFormats {

            @Test
            @DisplayName("Then all formats returned")
            void Then_AllFormatsReturned() {
                var formats = service.supportedFormats();

                assertThat(formats).containsExactlyInAnyOrder("csv", "json");
            }
        }
    }

    @Nested
    @DisplayName("Given case-insensitive format lookup")
    class Given_CaseInsensitiveFormatLookup {

        private final GraphExportService service = new GraphExportService(
                List.of(stubExporter("json", ".json", "{}".getBytes())));

        @Nested
        @DisplayName("When querying with different cases")
        class When_QueryingWithDifferentCases {

            @Test
            @DisplayName("Then uppercase format resolves correctly")
            void Then_UppercaseFormatResolvesCorrectly() {
                assertThat(service.getExporter("JSON")).isPresent();
            }

            @Test
            @DisplayName("Then mixed case format resolves correctly")
            void Then_MixedCaseFormatResolvesCorrectly() {
                assertThat(service.getExporter("Json")).isPresent();
            }
        }
    }

    @Nested
    @DisplayName("Given unknown format")
    class Given_UnknownFormat {

        private final GraphExportService service = new GraphExportService(
                List.of(stubExporter("json", ".json", "{}".getBytes())));

        @Nested
        @DisplayName("When exporting")
        class When_Exporting {

            @Test
            @DisplayName("Then throws IllegalArgumentException")
            void Then_ThrowsIllegalArgumentException() {
                var graph = buildTestGraph();

                assertThatThrownBy(() -> service.export(graph, "unknown"))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Unsupported export format");
            }
        }
    }

    @Nested
    @DisplayName("Given getExporter")
    class Given_GetExporter {

        private final GraphExportService service = new GraphExportService(
                List.of(stubExporter("json", ".json", "{}".getBytes())));

        @Nested
        @DisplayName("When format exists")
        class When_FormatExists {

            @Test
            @DisplayName("Then returns exporter")
            void Then_ReturnsExporter() {
                assertThat(service.getExporter("json")).isPresent();
            }
        }

        @Nested
        @DisplayName("When format is null")
        class When_FormatIsNull {

            @Test
            @DisplayName("Then returns empty")
            void Then_ReturnsEmpty() {
                assertThat(service.getExporter(null)).isEmpty();
            }
        }
    }
}
