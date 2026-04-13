package com.codevision.codevisionbackend.graph.web;

import com.codevision.codevisionbackend.graph.*;
import com.codevision.codevisionbackend.graph.export.ExportException;
import com.codevision.codevisionbackend.graph.export.GraphExportService;
import com.codevision.codevisionbackend.graph.export.GraphExporter;
import com.codevision.codevisionbackend.config.SecurityProperties;
import com.codevision.codevisionbackend.security.ApiKeyFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = GraphExportController.class,
        properties = "security.apiKey=test-key")
@Import({ApiKeyFilter.class, GraphExportControllerAdvice.class})
@EnableConfigurationProperties(SecurityProperties.class)
class GraphExportControllerTest {

    private static final String API_KEY = "test-key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GraphExportService exportService;

    @Nested
    class Given_SupportedFormat {

        @Nested
        class When_ExportingGraph {

            @Test
            void Then_ReturnsExportedBytesWithCorrectHeaders() throws Exception {
                var exporter = mock(GraphExporter.class);
                when(exporter.export(any(KnowledgeGraph.class))).thenReturn("{\"nodes\":[]}".getBytes());
                when(exporter.contentType()).thenReturn("application/json");
                when(exporter.fileExtension()).thenReturn(".json");
                when(exportService.getExporter("json")).thenReturn(Optional.of(exporter));

                var graphJson = objectMapper.writeValueAsString(new KnowledgeGraph());

                mockMvc.perform(post("/api/v1/graph/export/json")
                                .header("X-API-KEY", API_KEY)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(graphJson))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType("application/json"))
                        .andExpect(header().string("Content-Disposition",
                                "attachment; filename=\"knowledge-graph.json\""))
                        .andExpect(content().string("{\"nodes\":[]}"));
            }
        }
    }

    @Nested
    class Given_CsvFormat {

        @Nested
        class When_ExportingGraph {

            @Test
            void Then_ReturnsCorrectContentType() throws Exception {
                var exporter = mock(GraphExporter.class);
                when(exporter.export(any(KnowledgeGraph.class))).thenReturn("id,name\n".getBytes());
                when(exporter.contentType()).thenReturn("text/csv");
                when(exporter.fileExtension()).thenReturn(".csv");
                when(exportService.getExporter("csv")).thenReturn(Optional.of(exporter));

                var graphJson = objectMapper.writeValueAsString(new KnowledgeGraph());

                mockMvc.perform(post("/api/v1/graph/export/csv")
                                .header("X-API-KEY", API_KEY)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(graphJson))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType("text/csv"));
            }
        }
    }

    @Nested
    class Given_UnsupportedFormat {

        @Nested
        class When_ExportingGraph {

            @Test
            void Then_Returns400BadRequest() throws Exception {
                when(exportService.getExporter("unknown")).thenReturn(Optional.empty());

                var graphJson = objectMapper.writeValueAsString(new KnowledgeGraph());

                mockMvc.perform(post("/api/v1/graph/export/unknown")
                                .header("X-API-KEY", API_KEY)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(graphJson))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.error").value("Unsupported export format: unknown"));
            }
        }
    }

    @Nested
    class Given_ExportFailure {

        @Nested
        class When_ExporterThrowsExportException {

            @Test
            void Then_Returns500InternalServerError() throws Exception {
                var exporter = mock(GraphExporter.class);
                when(exporter.export(any(KnowledgeGraph.class)))
                        .thenThrow(new ExportException("PDF generation failed"));
                when(exportService.getExporter("pdf")).thenReturn(Optional.of(exporter));

                var graphJson = objectMapper.writeValueAsString(new KnowledgeGraph());

                mockMvc.perform(post("/api/v1/graph/export/pdf")
                                .header("X-API-KEY", API_KEY)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(graphJson))
                        .andExpect(status().isInternalServerError())
                        .andExpect(jsonPath("$.error").value("Export failed: PDF generation failed"));
            }
        }
    }

    @Nested
    class Given_MissingApiKey {

        @Nested
        class When_PostingWithoutApiKey {

            @Test
            void Then_Returns401Unauthorized() throws Exception {
                var graphJson = objectMapper.writeValueAsString(new KnowledgeGraph());

                mockMvc.perform(post("/api/v1/graph/export/json")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(graphJson))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    @Nested
    class Given_FormatsEndpoint {

        @Nested
        class When_ListingFormats {

            @Test
            void Then_ReturnsAllSupportedFormats() throws Exception {
                when(exportService.supportedFormats())
                        .thenReturn(List.of("csv", "dot", "json", "mermaid"));

                mockMvc.perform(get("/api/v1/graph/export/formats"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.formats").isArray())
                        .andExpect(jsonPath("$.formats[0]").value("csv"))
                        .andExpect(jsonPath("$.formats[2]").value("json"));
            }
        }
    }
}
