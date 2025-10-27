package com.codevision.codevisionbackend.analyze;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.codevision.codevisionbackend.analyze.BuildInfo;
import com.codevision.codevisionbackend.analyze.MetadataDump;
import com.codevision.codevisionbackend.analyze.ParsedDataResponse;
import com.codevision.codevisionbackend.project.Project;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
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
        controller = new AnalyzeController(analysisService);
    }

    @Test
    void analyzeReturnsAnalyzedMetadataStatus() {
        AnalyzeRequest request = new AnalyzeRequest();
        request.setRepoUrl("https://example.com/repo.git");

        Project project = new Project(request.getRepoUrl(), "demo", OffsetDateTime.now());
        project.setId(321L);
        ParsedDataResponse data = new ParsedDataResponse(
                project.getId(),
                "demo",
                project.getRepoUrl(),
                project.getLastAnalyzedAt(),
                new BuildInfo("com.barclays", "demo", "1.0.0", "21"),
                List.of(),
                MetadataDump.empty());
        analysisService.setNextOutcome(new AnalysisOutcome(project, data));

        ResponseEntity<AnalyzeResponse> response = controller.analyze(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        AnalyzeResponse body = response.getBody();
        assertEquals(project.getId(), body.getProjectId());
        assertEquals(AnalyzeController.STATUS_ANALYZED_METADATA, body.getStatus());
    }

    private static class StubAnalysisService extends AnalysisService {

        private AnalysisOutcome nextOutcome;

        StubAnalysisService() {
            super(null, null, null, null, null, null, null, new ObjectMapper());
        }

        void setNextOutcome(AnalysisOutcome outcome) {
            this.nextOutcome = outcome;
        }

        @Override
        public AnalysisOutcome analyze(String repoUrl) {
            return nextOutcome;
        }
    }
}
