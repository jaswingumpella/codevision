package com.codevision.codevisionbackend.analyze.job;

import static com.codevision.codevisionbackend.git.BranchUtils.normalize;

import com.codevision.codevisionbackend.analyze.AnalysisOutcome;
import com.codevision.codevisionbackend.analyze.AnalysisService;
import com.codevision.codevisionbackend.project.Project;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class AnalysisJobService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisJobService.class);

    private final AnalysisJobRepository jobRepository;
    private final AnalysisService analysisService;
    private final TaskExecutor analysisJobExecutor;

    public AnalysisJobService(
            AnalysisJobRepository jobRepository,
            AnalysisService analysisService,
            @Qualifier("analysisJobExecutor") TaskExecutor analysisJobExecutor) {
        this.jobRepository = jobRepository;
        this.analysisService = analysisService;
        this.analysisJobExecutor = analysisJobExecutor;
    }

    public AnalysisJob enqueue(String repoUrl, String branchName) {
        if (repoUrl == null || repoUrl.isBlank()) {
            throw new IllegalArgumentException("Repository URL must be provided");
        }
        String normalizedRepoUrl = repoUrl.trim();
        String normalizedBranch = normalize(branchName);
        OffsetDateTime now = OffsetDateTime.now();
        AnalysisJob job = new AnalysisJob();
        job.setRepoUrl(normalizedRepoUrl);
        job.setBranchName(normalizedBranch);
        job.setStatus(AnalysisJobStatus.QUEUED);
        job.setStatusMessage("Queued for analysis");
        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        AnalysisJob persisted = jobRepository.save(job);
        try {
            analysisJobExecutor.execute(() -> processJob(persisted.getId(), normalizedRepoUrl, normalizedBranch));
        } catch (RejectedExecutionException rex) {
            log.error("Unable to enqueue analysis job {}", persisted.getId(), rex);
            markFailed(persisted.getId(), "Worker queue is full", rex);
            throw new IllegalStateException("Analysis queue is saturated. Try again shortly.", rex);
        }
        return persisted;
    }

    public Optional<AnalysisJob> findJob(UUID jobId) {
        return jobRepository.findById(jobId);
    }

    private void processJob(UUID jobId, String repoUrl, String branchName) {
        log.info("Starting analysis job {} for {} ({})", jobId, repoUrl, branchName);
        updateJob(jobId, job -> {
            OffsetDateTime now = OffsetDateTime.now();
            job.setStatus(AnalysisJobStatus.RUNNING);
            job.setStartedAt(now);
            job.setUpdatedAt(now);
            job.setStatusMessage("Running analysis");
            job.setErrorMessage(null);
        });
        try {
            AnalysisOutcome outcome = analysisService.analyze(repoUrl, branchName);
            Project project = outcome.project();
            Long projectId = project != null ? project.getId() : null;
            updateJob(jobId, job -> {
                OffsetDateTime finished = OffsetDateTime.now();
                job.setStatus(AnalysisJobStatus.SUCCEEDED);
                job.setCompletedAt(finished);
                job.setUpdatedAt(finished);
                job.setProjectId(projectId);
                job.setCommitHash(outcome.commitHash());
                job.setSnapshotId(outcome.snapshotId());
                job.setStatusMessage("Snapshot saved");
                job.setErrorMessage(null);
            });
            log.info(
                    "Analysis job {} succeeded for repo {} ({}) -> project {} snapshot {}",
                    jobId,
                    repoUrl,
                    branchName,
                    projectId,
                    outcome.snapshotId());
        } catch (Exception ex) {
            log.error("Analysis job {} failed for repo {}", jobId, repoUrl, ex);
            markFailed(jobId, "Analysis failed", ex);
        }
    }

    private void markFailed(UUID jobId, String message, Exception ex) {
        updateJob(jobId, job -> {
            OffsetDateTime finished = OffsetDateTime.now();
            job.setStatus(AnalysisJobStatus.FAILED);
            job.setCompletedAt(finished);
            job.setUpdatedAt(finished);
            job.setStatusMessage(message);
            job.setErrorMessage(truncate(ex.getMessage()));
        });
    }

    private void updateJob(UUID jobId, java.util.function.Consumer<AnalysisJob> updater) {
        AnalysisJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }
        updater.accept(job);
        jobRepository.save(job);
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
