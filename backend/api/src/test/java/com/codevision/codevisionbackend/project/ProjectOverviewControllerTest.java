package com.codevision.codevisionbackend.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codevision.codevisionbackend.analyze.MetadataDump;
import com.codevision.codevisionbackend.analyze.ParsedDataResponse;
import com.codevision.codevisionbackend.api.ApiModelMapper;
import com.codevision.codevisionbackend.analyze.BuildInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ProjectOverviewControllerTest {

    private StubProjectSnapshotService snapshotService;
    private ProjectOverviewController controller;

    @BeforeEach
    void setUp() {
        snapshotService = new StubProjectSnapshotService();
        controller = new ProjectOverviewController(snapshotService, new ApiModelMapper());
    }

    @Test
    void getOverviewReturnsSnapshotWhenPresent() {
        ParsedDataResponse response = new ParsedDataResponse(
                5L,
                "demo",
                "https://example.com/repo.git",
                OffsetDateTime.now(),
                new BuildInfo("com.barclays", "demo", "1.0.0", "21"),
                List.of(),
                MetadataDump.empty());
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

    private static class StubProjectSnapshotService extends ProjectSnapshotService {

        private Optional<ParsedDataResponse> snapshot = Optional.empty();

        StubProjectSnapshotService() {
            super(null, null, null, new ObjectMapper());
        }

        void setSnapshot(Optional<ParsedDataResponse> snapshot) {
            this.snapshot = snapshot;
        }

        @Override
        public Optional<ParsedDataResponse> fetchSnapshot(Long projectId) {
            return snapshot;
        }

        @Override
        public void saveSnapshot(Project project, ParsedDataResponse parsedData) {
            this.snapshot = Optional.of(parsedData);
        }
    }
}
