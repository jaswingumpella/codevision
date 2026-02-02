package com.codevision.codevisionbackend.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codevision.codevisionbackend.analyze.ApiEndpointSummary;
import com.codevision.codevisionbackend.analyze.AssetInventory;
import com.codevision.codevisionbackend.analyze.DbAnalysisSummary;
import com.codevision.codevisionbackend.analyze.MetadataDump;
import com.codevision.codevisionbackend.analyze.ParsedDataResponse;
import com.codevision.codevisionbackend.api.ApiModelMapper;
import com.codevision.codevisionbackend.analyze.BuildInfo;
import com.codevision.codevisionbackend.project.ProjectSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ProjectOverviewControllerTest {

    private StubProjectSnapshotService snapshotService;
    private ProjectSnapshotController controller;

    @BeforeEach
    void setUp() {
        snapshotService = new StubProjectSnapshotService();
        controller = new ProjectSnapshotController(snapshotService, new ApiModelMapper());
    }

    @Test
    void getOverviewReturnsSnapshotWhenPresent() {
        ParsedDataResponse response = new ParsedDataResponse(
                5L,
                "demo",
                "https://example.com/repo.git",
                OffsetDateTime.now(),
                new BuildInfo("com.example", "demo", "1.0.0", "21"),
                List.of(),
                MetadataDump.empty(),
                emptyDbAnalysis(),
                List.of(),
                AssetInventory.empty(),
                List.of(),
                List.of(),
                List.of(),
                Map.of(),
                List.of());
        snapshotService.setSnapshot(Optional.of(response));

        ResponseEntity<com.codevision.codevisionbackend.api.model.ParsedDataResponse> result =
                controller.getProjectOverview(5L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.hasBody());
        assertEquals("demo", result.getBody().getProjectName());
    }

    @Test
    void getOverviewReturnsNotFoundWhenMissing() {
        snapshotService.setSnapshot(Optional.empty());

        ResponseEntity<com.codevision.codevisionbackend.api.model.ParsedDataResponse> result =
                controller.getProjectOverview(99L);

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        assertNull(result.getBody());
    }

    @Test
    void getApiEndpointsReturnsCatalogWhenPresent() {
        ApiEndpointSummary endpoint = new ApiEndpointSummary(
                "REST", "GET", "/demo", "com.example.Controller", "getDemo", List.of());
        ParsedDataResponse response = new ParsedDataResponse(
                12L,
                "demo",
                "https://example.com/repo.git",
                OffsetDateTime.now(),
                BuildInfo.empty(),
                List.of(),
                MetadataDump.empty(),
                emptyDbAnalysis(),
                List.of(endpoint),
                AssetInventory.empty(),
                List.of(),
                List.of(),
                List.of(),
                Map.of(),
                List.of());
        snapshotService.setSnapshot(Optional.of(response));

        ResponseEntity<com.codevision.codevisionbackend.api.model.ProjectApiEndpointsResponse> result =
                controller.getProjectApiEndpoints(12L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.hasBody());
        assertEquals(1, result.getBody().getEndpoints().size());
    }

    @Test
    void getDbAnalysisReturnsPayloadWhenPresent() {
        DbAnalysisSummary summary = new DbAnalysisSummary(
                List.of(),
                Map.of("Customer", List.of("com.example.CustomerRepository")),
                Map.of("com.example.CustomerRepository",
                        List.of(new DbAnalysisSummary.DaoOperationDetails("findAll", "SELECT", "Customer", null))));
        ParsedDataResponse response = new ParsedDataResponse(
                12L,
                "demo",
                "https://example.com/repo.git",
                OffsetDateTime.now(),
                BuildInfo.empty(),
                List.of(),
                MetadataDump.empty(),
                summary,
                List.of(),
                AssetInventory.empty(),
                List.of(),
                List.of(),
                List.of(),
                Map.of(),
                List.of());
        snapshotService.setSnapshot(Optional.of(response));

        ResponseEntity<com.codevision.codevisionbackend.api.model.ProjectDbAnalysisResponse> result =
                controller.getProjectDbAnalysis(12L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.hasBody());
        assertEquals(1, result.getBody().getDbAnalysis().getClassesByEntity().size());
    }

    private static class StubProjectSnapshotService extends ProjectSnapshotService {

        private Optional<ParsedDataResponse> snapshot = Optional.empty();

        StubProjectSnapshotService() {
            super(null, null, null, null, null, new ObjectMapper());
        }

        void setSnapshot(Optional<ParsedDataResponse> snapshot) {
            this.snapshot = snapshot;
        }

        @Override
        public Optional<ParsedDataResponse> fetchSnapshot(Long projectId) {
            return snapshot;
        }

        @Override
        public ProjectSnapshot saveSnapshot(Project project, ParsedDataResponse parsedData, SnapshotMetadata metadata) {
            this.snapshot = Optional.of(parsedData);
            return null;
        }
    }

    private DbAnalysisSummary emptyDbAnalysis() {
        return new DbAnalysisSummary(List.of(), Map.of(), Map.of());
    }
}
