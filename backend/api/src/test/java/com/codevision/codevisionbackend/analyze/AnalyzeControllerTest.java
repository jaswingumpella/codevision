package com.codevision.codevisionbackend.analyze;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.codevision.codevisionbackend.analyze.AssetInventory;
import com.codevision.codevisionbackend.analyze.DbAnalysisSummary;
import com.codevision.codevisionbackend.api.ApiModelMapper;
import com.codevision.codevisionbackend.api.model.AnalyzeRequest;
import com.codevision.codevisionbackend.api.model.AnalyzeResponse;
import com.codevision.codevisionbackend.project.Project;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AnalyzeControllerTest {

    private StubAnalysisService analysisService;
    private AnalyzeController controller;

    @BeforeEach
    void setUp() {
        analysisService = new StubAnalysisService();
        controller = new AnalyzeController(analysisService, new ApiModelMapper());
    }

    @Test
    void analyzeReturnsAnalyzedMetadataStatus() {
        AnalyzeRequest request = new AnalyzeRequest();
        request.setRepoUrl(URI.create("https://example.com/repo.git"));

        Project project = new Project(request.getRepoUrl().toString(), "demo", OffsetDateTime.now());
        project.setId(321L);
        ParsedDataResponse data = new ParsedDataResponse(
                project.getId(),
                "demo",
                project.getRepoUrl(),
                project.getLastAnalyzedAt(),
                new BuildInfo("com.barclays", "demo", "1.0.0", "21"),
                List.of(),
                MetadataDump.empty(),
                emptyDbAnalysis(),
                List.of(),
                AssetInventory.empty(),
                List.of(),
                List.of(),
                Map.of(),
                List.of());
        analysisService.setNextOutcome(new AnalysisOutcome(project, data));

        ResponseEntity<AnalyzeResponse> response = controller.analyzeRepository(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        AnalyzeResponse body = response.getBody();
        assertEquals(project.getId(), body.getProjectId());
        assertEquals(AnalyzeController.STATUS_ANALYZED_METADATA, body.getStatus());
    }

    private static class StubAnalysisService extends AnalysisService {

        private AnalysisOutcome nextOutcome;

        StubAnalysisService() {
            super(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    new ObjectMapper());
        }

        void setNextOutcome(AnalysisOutcome outcome) {
            this.nextOutcome = outcome;
        }

        @Override
        public AnalysisOutcome analyze(String repoUrl) {
            return nextOutcome;
        }
    }

    private DbAnalysisSummary emptyDbAnalysis() {
        return new DbAnalysisSummary(List.of(), Map.of(), Map.of());
    }
}
