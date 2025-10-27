package com.codevision.codevisionbackend.analyze;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analyze")
public class AnalyzeController {

    public static final String STATUS_ANALYZED_METADATA = "ANALYZED_METADATA";

    private final AnalysisService analysisService;

    public AnalyzeController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping
    public ResponseEntity<AnalyzeResponse> analyze(@Valid @RequestBody AnalyzeRequest request) {
        AnalysisOutcome outcome = analysisService.analyze(request.getRepoUrl());
        return ResponseEntity.ok(new AnalyzeResponse(outcome.project().getId(), STATUS_ANALYZED_METADATA));
    }
}
