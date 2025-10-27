package com.codevision.codevisionbackend.analyze;

import com.codevision.codevisionbackend.api.ApiModelMapper;
import com.codevision.codevisionbackend.api.generated.AnalysisApi;
import com.codevision.codevisionbackend.api.model.AnalyzeRequest;
import com.codevision.codevisionbackend.api.model.AnalyzeResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AnalyzeController implements AnalysisApi {

    public static final String STATUS_ANALYZED_METADATA = "ANALYZED_METADATA";

    private final AnalysisService analysisService;
    private final ApiModelMapper apiModelMapper;

    public AnalyzeController(AnalysisService analysisService, ApiModelMapper apiModelMapper) {
        this.analysisService = analysisService;
        this.apiModelMapper = apiModelMapper;
    }

    @Override
    public ResponseEntity<AnalyzeResponse> analyzeRepository(@Valid @RequestBody AnalyzeRequest analyzeRequest) {
        AnalysisOutcome outcome = analysisService.analyze(analyzeRequest.getRepoUrl().toString());
        AnalyzeResponse response =
                apiModelMapper.toAnalyzeResponse(outcome.project().getId(), STATUS_ANALYZED_METADATA);
        return ResponseEntity.ok(response);
    }
}
