package com.codevision.codevisionbackend.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * ProjectSnapshotSummary
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-09T19:10:45.782948-05:00[America/New_York]", comments = "Generator version: 7.5.0")
public class ProjectSnapshotSummary {

  private Long snapshotId;

  private String branchName;

  private String commitHash;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  public ProjectSnapshotSummary() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ProjectSnapshotSummary(Long snapshotId, String branchName, OffsetDateTime createdAt) {
    this.snapshotId = snapshotId;
    this.branchName = branchName;
    this.createdAt = createdAt;
  }

  public ProjectSnapshotSummary snapshotId(Long snapshotId) {
    this.snapshotId = snapshotId;
    return this;
  }

  /**
   * Get snapshotId
   * @return snapshotId
  */
  @NotNull 
  @Schema(name = "snapshotId", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("snapshotId")
  public Long getSnapshotId() {
    return snapshotId;
  }

  public void setSnapshotId(Long snapshotId) {
    this.snapshotId = snapshotId;
  }

  public ProjectSnapshotSummary branchName(String branchName) {
    this.branchName = branchName;
    return this;
  }

  /**
   * Get branchName
   * @return branchName
  */
  @NotNull 
  @Schema(name = "branchName", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("branchName")
  public String getBranchName() {
    return branchName;
  }

  public void setBranchName(String branchName) {
    this.branchName = branchName;
  }

  public ProjectSnapshotSummary commitHash(String commitHash) {
    this.commitHash = commitHash;
    return this;
  }

  /**
   * Get commitHash
   * @return commitHash
  */
  
  @Schema(name = "commitHash", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("commitHash")
  public String getCommitHash() {
    return commitHash;
  }

  public void setCommitHash(String commitHash) {
    this.commitHash = commitHash;
  }

  public ProjectSnapshotSummary createdAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  /**
   * Get createdAt
   * @return createdAt
  */
  @NotNull @Valid 
  @Schema(name = "createdAt", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("createdAt")
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProjectSnapshotSummary projectSnapshotSummary = (ProjectSnapshotSummary) o;
    return Objects.equals(this.snapshotId, projectSnapshotSummary.snapshotId) &&
        Objects.equals(this.branchName, projectSnapshotSummary.branchName) &&
        Objects.equals(this.commitHash, projectSnapshotSummary.commitHash) &&
        Objects.equals(this.createdAt, projectSnapshotSummary.createdAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(snapshotId, branchName, commitHash, createdAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProjectSnapshotSummary {\n");
    sb.append("    snapshotId: ").append(toIndentedString(snapshotId)).append("\n");
    sb.append("    branchName: ").append(toIndentedString(branchName)).append("\n");
    sb.append("    commitHash: ").append(toIndentedString(commitHash)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
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

