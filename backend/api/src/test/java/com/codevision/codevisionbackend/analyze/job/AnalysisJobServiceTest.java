package com.codevision.codevisionbackend.analyze.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codevision.codevisionbackend.analyze.AnalysisOutcome;
import com.codevision.codevisionbackend.analyze.AnalysisService;
import com.codevision.codevisionbackend.project.Project;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.context.jdbc.Sql;

@DataJpaTest
@Sql(scripts = "classpath:schema-h2.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AnalysisJobServiceTest {

    @Autowired
    private AnalysisJobRepository jobRepository;

    private AnalysisService analysisService;
    private AnalysisJobService jobService;

    @BeforeEach
    void setUp() {
        analysisService = Mockito.mock(AnalysisService.class);
        TaskExecutor immediateExecutor = Runnable::run;
        jobService = new AnalysisJobService(jobRepository, analysisService, immediateExecutor);
    }

    @Test
    void enqueueRunsAnalysisAndMarksSuccess() {
        String repoUrl = "https://github.com/example/repo.git";
        Project project = new Project(repoUrl, "demo", "main", OffsetDateTime.now());
        project.setId(42L);
        AnalysisOutcome outcome = new AnalysisOutcome(project, null, "main", "abc123", 111L, false);
        when(analysisService.analyze(repoUrl, "main", true)).thenReturn(outcome);

        AnalysisJob job = jobService.enqueue(repoUrl, "main", true);

        verify(analysisService).analyze(repoUrl, "main", true);
        AnalysisJob persisted = jobRepository.findById(job.getId()).orElseThrow();
        assertEquals(AnalysisJobStatus.SUCCEEDED, persisted.getStatus());
        assertEquals(project.getId(), persisted.getProjectId());
        assertEquals("abc123", persisted.getCommitHash());
        assertEquals(111L, persisted.getSnapshotId());
        assertNotNull(persisted.getCompletedAt());
    }

    @Test
    void enqueueCapturesFailureDetails() {
        String repoUrl = "https://github.com/example/broken.git";
        when(analysisService.analyze(repoUrl, "main", true)).thenThrow(new IllegalStateException("boom"));

        AnalysisJob job = jobService.enqueue(repoUrl, "main", true);

        AnalysisJob persisted = jobRepository.findById(job.getId()).orElseThrow();
        assertEquals(AnalysisJobStatus.FAILED, persisted.getStatus());
        assertEquals("Analysis failed", persisted.getStatusMessage());
        assertNotNull(persisted.getErrorMessage());
    }
}
