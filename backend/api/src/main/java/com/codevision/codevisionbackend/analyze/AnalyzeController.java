package com.codevision.codevisionbackend.analyze;

import com.codevision.codevisionbackend.analyze.job.AnalysisJob;
import com.codevision.codevisionbackend.analyze.job.AnalysisJobService;
import com.codevision.codevisionbackend.api.ApiModelMapper;
import com.codevision.codevisionbackend.api.generated.AnalysisApi;
import com.codevision.codevisionbackend.api.model.AnalyzeRequest;
import com.codevision.codevisionbackend.api.model.AnalyzeResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AnalyzeController implements AnalysisApi {

    private static final Logger log = LoggerFactory.getLogger(AnalyzeController.class);

    private final AnalysisJobService analysisJobService;
    private final ApiModelMapper apiModelMapper;

    public AnalyzeController(AnalysisJobService analysisJobService, ApiModelMapper apiModelMapper) {
        this.analysisJobService = analysisJobService;
        this.apiModelMapper = apiModelMapper;
    }

    @Override
    public ResponseEntity<AnalyzeResponse> analyzeRepository(@Valid @RequestBody AnalyzeRequest analyzeRequest) {
        String repoUrl = analyzeRequest.getRepoUrl() != null ? analyzeRequest.getRepoUrl().toString() : "n/a";
        log.info("Received analyze request for {}", repoUrl);
        try {
            AnalysisJob job = analysisJobService.enqueue(repoUrl);
            AnalyzeResponse response = apiModelMapper.toAnalyzeResponse(job);
            log.info("Enqueued analysis job {} for {}", job.getId(), repoUrl);
            return ResponseEntity.accepted().body(response);
        } catch (IllegalArgumentException ex) {
            log.warn("Rejecting analysis request for {}: {}", repoUrl, ex.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException ex) {
            log.warn("Unable to enqueue analysis job for {}", repoUrl, ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    @Override
    public ResponseEntity<AnalyzeResponse> getAnalysisJob(UUID jobId) {
        return analysisJobService
                .findJob(jobId)
                .map(job -> ResponseEntity.ok(apiModelMapper.toAnalyzeResponse(job)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
