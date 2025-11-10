package com.codevision.codevisionbackend.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * AnalyzeResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-09T21:12:14.696819-05:00[America/New_York]", comments = "Generator version: 7.5.0")
public class AnalyzeResponse {

  private UUID jobId;

  private Long projectId;

  private URI repoUrl;

  /**
   * Current status of the analysis workflow.
   */
  public enum StatusEnum {
    QUEUED("QUEUED"),
    
    RUNNING("RUNNING"),
    
    SUCCEEDED("SUCCEEDED"),
    
    FAILED("FAILED");

    private String value;

    StatusEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static StatusEnum fromValue(String value) {
      for (StatusEnum b : StatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private StatusEnum status;

  private String statusMessage;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime startedAt;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime completedAt;

  private String errorMessage;

  private String branchName;

  private String commitHash;

  private Long snapshotId;

  public AnalyzeResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AnalyzeResponse(UUID jobId, URI repoUrl, StatusEnum status, OffsetDateTime createdAt) {
    this.jobId = jobId;
    this.repoUrl = repoUrl;
    this.status = status;
    this.createdAt = createdAt;
  }

  public AnalyzeResponse jobId(UUID jobId) {
    this.jobId = jobId;
    return this;
  }

  /**
   * Unique identifier assigned to the analysis job.
   * @return jobId
  */
  @NotNull @Valid 
  @Schema(name = "jobId", description = "Unique identifier assigned to the analysis job.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("jobId")
  public UUID getJobId() {
    return jobId;
  }

  public void setJobId(UUID jobId) {
    this.jobId = jobId;
  }

  public AnalyzeResponse projectId(Long projectId) {
    this.projectId = projectId;
    return this;
  }

  /**
   * Identifier assigned to the analyzed project once the job succeeds.
   * @return projectId
  */
  
  @Schema(name = "projectId", description = "Identifier assigned to the analyzed project once the job succeeds.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("projectId")
  public Long getProjectId() {
    return projectId;
  }

  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }

  public AnalyzeResponse repoUrl(URI repoUrl) {
    this.repoUrl = repoUrl;
    return this;
  }

  /**
   * Repository URL associated with the analysis job.
   * @return repoUrl
  */
  @NotNull @Valid 
  @Schema(name = "repoUrl", description = "Repository URL associated with the analysis job.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("repoUrl")
  public URI getRepoUrl() {
    return repoUrl;
  }

  public void setRepoUrl(URI repoUrl) {
    this.repoUrl = repoUrl;
  }

  public AnalyzeResponse status(StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * Current status of the analysis workflow.
   * @return status
  */
  @NotNull 
  @Schema(name = "status", description = "Current status of the analysis workflow.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("status")
  public StatusEnum getStatus() {
    return status;
  }

  public void setStatus(StatusEnum status) {
    this.status = status;
  }

  public AnalyzeResponse statusMessage(String statusMessage) {
    this.statusMessage = statusMessage;
    return this;
  }

  /**
   * Friendly text describing the most recent job milestone.
   * @return statusMessage
  */
  
  @Schema(name = "statusMessage", description = "Friendly text describing the most recent job milestone.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("statusMessage")
  public String getStatusMessage() {
    return statusMessage;
  }

  public void setStatusMessage(String statusMessage) {
    this.statusMessage = statusMessage;
  }

  public AnalyzeResponse createdAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  /**
   * Timestamp when the job was created.
   * @return createdAt
  */
  @NotNull @Valid 
  @Schema(name = "createdAt", description = "Timestamp when the job was created.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("createdAt")
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public AnalyzeResponse startedAt(OffsetDateTime startedAt) {
    this.startedAt = startedAt;
    return this;
  }

  /**
   * Timestamp when the worker began executing the job.
   * @return startedAt
  */
  @Valid 
  @Schema(name = "startedAt", description = "Timestamp when the worker began executing the job.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("startedAt")
  public OffsetDateTime getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(OffsetDateTime startedAt) {
    this.startedAt = startedAt;
  }

  public AnalyzeResponse completedAt(OffsetDateTime completedAt) {
    this.completedAt = completedAt;
    return this;
  }

  /**
   * Timestamp when the job finished, regardless of success or failure.
   * @return completedAt
  */
  @Valid 
  @Schema(name = "completedAt", description = "Timestamp when the job finished, regardless of success or failure.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("completedAt")
  public OffsetDateTime getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(OffsetDateTime completedAt) {
    this.completedAt = completedAt;
  }

  public AnalyzeResponse errorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
    return this;
  }

  /**
   * Failure summary populated when the job ends in FAILED status.
   * @return errorMessage
  */
  
  @Schema(name = "errorMessage", description = "Failure summary populated when the job ends in FAILED status.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("errorMessage")
  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public AnalyzeResponse branchName(String branchName) {
    this.branchName = branchName;
    return this;
  }

  /**
   * Branch that was analyzed.
   * @return branchName
  */
  
  @Schema(name = "branchName", description = "Branch that was analyzed.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("branchName")
  public String getBranchName() {
    return branchName;
  }

  public void setBranchName(String branchName) {
    this.branchName = branchName;
  }

  public AnalyzeResponse commitHash(String commitHash) {
    this.commitHash = commitHash;
    return this;
  }

  /**
   * Commit hash associated with the snapshot, when known.
   * @return commitHash
  */
  
  @Schema(name = "commitHash", description = "Commit hash associated with the snapshot, when known.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("commitHash")
  public String getCommitHash() {
    return commitHash;
  }

  public void setCommitHash(String commitHash) {
    this.commitHash = commitHash;
  }

  public AnalyzeResponse snapshotId(Long snapshotId) {
    this.snapshotId = snapshotId;
    return this;
  }

  /**
   * Identifier of the persisted snapshot once analysis succeeds.
   * @return snapshotId
  */
  
  @Schema(name = "snapshotId", description = "Identifier of the persisted snapshot once analysis succeeds.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("snapshotId")
  public Long getSnapshotId() {
    return snapshotId;
  }

  public void setSnapshotId(Long snapshotId) {
    this.snapshotId = snapshotId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AnalyzeResponse analyzeResponse = (AnalyzeResponse) o;
    return Objects.equals(this.jobId, analyzeResponse.jobId) &&
        Objects.equals(this.projectId, analyzeResponse.projectId) &&
        Objects.equals(this.repoUrl, analyzeResponse.repoUrl) &&
        Objects.equals(this.status, analyzeResponse.status) &&
        Objects.equals(this.statusMessage, analyzeResponse.statusMessage) &&
        Objects.equals(this.createdAt, analyzeResponse.createdAt) &&
        Objects.equals(this.startedAt, analyzeResponse.startedAt) &&
        Objects.equals(this.completedAt, analyzeResponse.completedAt) &&
        Objects.equals(this.errorMessage, analyzeResponse.errorMessage) &&
        Objects.equals(this.branchName, analyzeResponse.branchName) &&
        Objects.equals(this.commitHash, analyzeResponse.commitHash) &&
        Objects.equals(this.snapshotId, analyzeResponse.snapshotId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(jobId, projectId, repoUrl, status, statusMessage, createdAt, startedAt, completedAt, errorMessage, branchName, commitHash, snapshotId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AnalyzeResponse {\n");
    sb.append("    jobId: ").append(toIndentedString(jobId)).append("\n");
    sb.append("    projectId: ").append(toIndentedString(projectId)).append("\n");
    sb.append("    repoUrl: ").append(toIndentedString(repoUrl)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    statusMessage: ").append(toIndentedString(statusMessage)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
    sb.append("    startedAt: ").append(toIndentedString(startedAt)).append("\n");
    sb.append("    completedAt: ").append(toIndentedString(completedAt)).append("\n");
    sb.append("    errorMessage: ").append(toIndentedString(errorMessage)).append("\n");
    sb.append("    branchName: ").append(toIndentedString(branchName)).append("\n");
    sb.append("    commitHash: ").append(toIndentedString(commitHash)).append("\n");
    sb.append("    snapshotId: ").append(toIndentedString(snapshotId)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

