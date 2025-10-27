package com.codevision.codevisionbackend.analyze;

import com.codevision.codevisionbackend.api.ApiModelMapper;
import com.codevision.codevisionbackend.api.generated.AnalysisApi;
import com.codevision.codevisionbackend.api.model.AnalyzeRequest;
import com.codevision.codevisionbackend.api.model.AnalyzeResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AnalyzeController implements AnalysisApi {

    public static final String STATUS_ANALYZED_METADATA = "ANALYZED_METADATA";
    private static final Logger log = LoggerFactory.getLogger(AnalyzeController.class);

    private final AnalysisService analysisService;
    private final ApiModelMapper apiModelMapper;

    public AnalyzeController(AnalysisService analysisService, ApiModelMapper apiModelMapper) {
        this.analysisService = analysisService;
        this.apiModelMapper = apiModelMapper;
    }

    @Override
    public ResponseEntity<AnalyzeResponse> analyzeRepository(@Valid @RequestBody AnalyzeRequest analyzeRequest) {
        String repoUrl = analyzeRequest.getRepoUrl() != null ? analyzeRequest.getRepoUrl().toString() : "n/a";
        log.info("Received analyze request for {}", repoUrl);
        AnalysisOutcome outcome = analysisService.analyze(repoUrl);
        AnalyzeResponse response =
                apiModelMapper.toAnalyzeResponse(outcome.project().getId(), STATUS_ANALYZED_METADATA);
        log.info(
                "Completed analysis for {} -> projectId={}",
                repoUrl,
                outcome.project().getId());
        return ResponseEntity.ok(response);
    }
}
