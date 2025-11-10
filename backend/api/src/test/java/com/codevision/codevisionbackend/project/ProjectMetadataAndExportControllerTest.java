package com.codevision.codevisionbackend.project;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codevision.codevisionbackend.analyze.ParsedDataResponse;
import com.codevision.codevisionbackend.api.ApiModelMapper;
import com.codevision.codevisionbackend.api.model.ProjectMetadataResponse;
import com.codevision.codevisionbackend.project.export.ExportService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ProjectMetadataAndExportControllerTest {

    @Mock
    private ProjectSnapshotService projectSnapshotService;

    @Mock
    private ApiModelMapper apiModelMapper;

    @Mock
    private ExportService exportService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new ProjectMetadataController(projectSnapshotService, apiModelMapper),
                        new ProjectExportController(projectSnapshotService, apiModelMapper, exportService))
                .build();
    }

    @Test
    void metadataEndpointReturnsPayload() throws Exception {
        ParsedDataResponse snapshot = snapshot();
        ProjectMetadataResponse response = new ProjectMetadataResponse().projectId(7L).projectName("demo");
        when(projectSnapshotService.fetchSnapshot(7L)).thenReturn(Optional.of(snapshot));
        when(apiModelMapper.toProjectMetadataResponse(7L, snapshot)).thenReturn(response);

        mockMvc.perform(get("/project/{id}/metadata", 7))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectName").value("demo"));
    }

    @Test
    void metadataEndpointReturns404WhenMissing() throws Exception {
        when(projectSnapshotService.fetchSnapshot(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/project/{id}/metadata", 1))
                .andExpect(status().isNotFound());
    }

    @Test
    void exportHtmlReturnsAttachment() throws Exception {
        ParsedDataResponse snapshot = snapshot();
        when(projectSnapshotService.fetchSnapshot(5L)).thenReturn(Optional.of(snapshot));
        when(exportService.buildConfluenceHtml(snapshot)).thenReturn("<html>demo</html>");

        mockMvc.perform(get("/project/{id}/export/confluence.html", 5))
                .andExpect(status().isOk());
    }

    @Test
    void exportSnapshotReturnsJson() throws Exception {
        ParsedDataResponse snapshot = snapshot();
        com.codevision.codevisionbackend.api.model.ParsedDataResponse apiResponse =
                new com.codevision.codevisionbackend.api.model.ParsedDataResponse().projectName("demo");
        when(projectSnapshotService.fetchSnapshot(9L)).thenReturn(Optional.of(snapshot));
        when(apiModelMapper.toParsedDataResponse(snapshot)).thenReturn(apiResponse);

        mockMvc.perform(get("/project/{id}/export/snapshot", 9).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectName").value("demo"));
    }

    private ParsedDataResponse snapshot() {
        return new ParsedDataResponse(
                7L,
                "demo",
                "repo",
                OffsetDateTime.now(),
                null,
                List.of(),
                null,
                null,
                List.of(),
                null,
                List.of(),
                List.of(),
                List.of(),
                Map.of(),
                List.of());
    }
}
