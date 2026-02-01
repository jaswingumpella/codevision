package com.codevision.codevisionbackend.analyze;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.codevision.codevisionbackend.analyze.job.AnalysisJob;
import com.codevision.codevisionbackend.analyze.job.AnalysisJobService;
import com.codevision.codevisionbackend.analyze.job.AnalysisJobStatus;
import com.codevision.codevisionbackend.api.ApiModelMapper;
import com.codevision.codevisionbackend.api.model.AnalyzeRequest;
import com.codevision.codevisionbackend.api.model.AnalyzeResponse;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AnalyzeControllerTest {

    private AnalysisJobService jobService;
    private AnalyzeController controller;

    @BeforeEach
    void setUp() {
        jobService = Mockito.mock(AnalysisJobService.class);
        controller = new AnalyzeController(jobService, new ApiModelMapper());
    }

    @Test
    void analyzeReturnsAcceptedJobDescriptor() {
        AnalysisJob job = new AnalysisJob();
        job.setId(UUID.randomUUID());
        job.setRepoUrl("https://example.com/repo.git");
        job.setStatus(AnalysisJobStatus.QUEUED);
        job.setStatusMessage("Queued for analysis");
        job.setCreatedAt(OffsetDateTime.now());
        job.setUpdatedAt(job.getCreatedAt());
        when(jobService.enqueue(anyString(), anyString(), any())).thenReturn(job);

        AnalyzeRequest request = new AnalyzeRequest();
        request.setRepoUrl(URI.create(job.getRepoUrl()));
        request.setBranchName("main");

        ResponseEntity<AnalyzeResponse> response = controller.analyzeRepository(request);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        AnalyzeResponse body = response.getBody();
        assertEquals(job.getId(), body.getJobId());
        assertEquals(AnalyzeResponse.StatusEnum.QUEUED, body.getStatus());
        assertNull(body.getProjectId());
    }

    @Test
    void getAnalysisJobReturnsStatusWhenPresent() {
        AnalysisJob job = new AnalysisJob();
        job.setId(UUID.randomUUID());
        job.setRepoUrl("https://example.com/repo.git");
        job.setStatus(AnalysisJobStatus.SUCCEEDED);
        job.setStatusMessage("Snapshot saved");
        job.setProjectId(99L);
        job.setCreatedAt(OffsetDateTime.now().minusMinutes(1));
        job.setStartedAt(job.getCreatedAt());
        job.setCompletedAt(OffsetDateTime.now());
        job.setUpdatedAt(job.getCompletedAt());
        UUID jobId = job.getId();
        when(jobService.findJob(jobId)).thenReturn(Optional.of(job));

        ResponseEntity<AnalyzeResponse> response = controller.getAnalysisJob(jobId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        AnalyzeResponse body = response.getBody();
        assertEquals(jobId, body.getJobId());
        assertEquals(job.getProjectId(), body.getProjectId());
        assertEquals(AnalyzeResponse.StatusEnum.SUCCEEDED, body.getStatus());
    }

    @Test
    void getAnalysisJobReturnsNotFoundWhenMissing() {
        UUID jobId = UUID.randomUUID();
        when(jobService.findJob(jobId)).thenReturn(Optional.empty());

        ResponseEntity<AnalyzeResponse> response = controller.getAnalysisJob(jobId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void analyzeReturnsBadRequestWhenEnqueueFailsValidation() {
        AnalyzeRequest request = new AnalyzeRequest();
        request.setRepoUrl(URI.create("https://example.com/repo.git"));
        request.setBranchName("main");
        when(jobService.enqueue(anyString(), anyString(), any())).thenThrow(new IllegalArgumentException("bad"));

        ResponseEntity<AnalyzeResponse> response = controller.analyzeRepository(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
