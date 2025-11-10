package com.codevision.codevisionbackend.api.model;

import java.net.URI;
import java.util.Objects;
import com.codevision.codevisionbackend.api.model.ProjectSnapshotSummary;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * ProjectSnapshotsResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-09T19:10:45.782948-05:00[America/New_York]", comments = "Generator version: 7.5.0")
public class ProjectSnapshotsResponse {

  private Long projectId;

  @Valid
  private List<@Valid ProjectSnapshotSummary> snapshots = new ArrayList<>();

  public ProjectSnapshotsResponse projectId(Long projectId) {
    this.projectId = projectId;
    return this;
  }

  /**
   * Get projectId
   * @return projectId
  */
  
  @Schema(name = "projectId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("projectId")
  public Long getProjectId() {
    return projectId;
  }

  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }

  public ProjectSnapshotsResponse snapshots(List<@Valid ProjectSnapshotSummary> snapshots) {
    this.snapshots = snapshots;
    return this;
  }

  public ProjectSnapshotsResponse addSnapshotsItem(ProjectSnapshotSummary snapshotsItem) {
    if (this.snapshots == null) {
      this.snapshots = new ArrayList<>();
    }
    this.snapshots.add(snapshotsItem);
    return this;
  }

  /**
   * Get snapshots
   * @return snapshots
  */
  @Valid 
  @Schema(name = "snapshots", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("snapshots")
  public List<@Valid ProjectSnapshotSummary> getSnapshots() {
    return snapshots;
  }

  public void setSnapshots(List<@Valid ProjectSnapshotSummary> snapshots) {
    this.snapshots = snapshots;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProjectSnapshotsResponse projectSnapshotsResponse = (ProjectSnapshotsResponse) o;
    return Objects.equals(this.projectId, projectSnapshotsResponse.projectId) &&
        Objects.equals(this.snapshots, projectSnapshotsResponse.snapshots);
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectId, snapshots);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProjectSnapshotsResponse {\n");
    sb.append("    projectId: ").append(toIndentedString(projectId)).append("\n");
    sb.append("    snapshots: ").append(toIndentedString(snapshots)).append("\n");
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

