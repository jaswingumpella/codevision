package com.codevision.codevisionbackend.api.model;

import java.net.URI;
import java.util.Objects;
import com.codevision.codevisionbackend.api.model.BuildInfo;
import com.codevision.codevisionbackend.api.model.MetadataDump;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.net.URI;
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
 * ProjectMetadataResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-09T00:07:15.283463-05:00[America/New_York]", comments = "Generator version: 7.5.0")
public class ProjectMetadataResponse {

  private Long projectId;

  private String projectName;

  private URI repoUrl;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime analyzedAt;

  private BuildInfo buildInfo;

  private MetadataDump metadataDump;

  private String snapshotDownloadUrl;

  public ProjectMetadataResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ProjectMetadataResponse(Long projectId, MetadataDump metadataDump) {
    this.projectId = projectId;
    this.metadataDump = metadataDump;
  }

  public ProjectMetadataResponse projectId(Long projectId) {
    this.projectId = projectId;
    return this;
  }

  /**
   * Identifier of the associated project.
   * @return projectId
  */
  @NotNull 
  @Schema(name = "projectId", description = "Identifier of the associated project.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("projectId")
  public Long getProjectId() {
    return projectId;
  }

  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }

  public ProjectMetadataResponse projectName(String projectName) {
    this.projectName = projectName;
    return this;
  }

  /**
   * Project name resolved during analysis.
   * @return projectName
  */
  
  @Schema(name = "projectName", description = "Project name resolved during analysis.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("projectName")
  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public ProjectMetadataResponse repoUrl(URI repoUrl) {
    this.repoUrl = repoUrl;
    return this;
  }

  /**
   * Repository URL associated with the project.
   * @return repoUrl
  */
  @Valid 
  @Schema(name = "repoUrl", description = "Repository URL associated with the project.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("repoUrl")
  public URI getRepoUrl() {
    return repoUrl;
  }

  public void setRepoUrl(URI repoUrl) {
    this.repoUrl = repoUrl;
  }

  public ProjectMetadataResponse analyzedAt(OffsetDateTime analyzedAt) {
    this.analyzedAt = analyzedAt;
    return this;
  }

  /**
   * Timestamp of the last completed analysis.
   * @return analyzedAt
  */
  @Valid 
  @Schema(name = "analyzedAt", description = "Timestamp of the last completed analysis.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("analyzedAt")
  public OffsetDateTime getAnalyzedAt() {
    return analyzedAt;
  }

  public void setAnalyzedAt(OffsetDateTime analyzedAt) {
    this.analyzedAt = analyzedAt;
  }

  public ProjectMetadataResponse buildInfo(BuildInfo buildInfo) {
    this.buildInfo = buildInfo;
    return this;
  }

  /**
   * Get buildInfo
   * @return buildInfo
  */
  @Valid 
  @Schema(name = "buildInfo", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("buildInfo")
  public BuildInfo getBuildInfo() {
    return buildInfo;
  }

  public void setBuildInfo(BuildInfo buildInfo) {
    this.buildInfo = buildInfo;
  }

  public ProjectMetadataResponse metadataDump(MetadataDump metadataDump) {
    this.metadataDump = metadataDump;
    return this;
  }

  /**
   * Get metadataDump
   * @return metadataDump
  */
  @NotNull @Valid 
  @Schema(name = "metadataDump", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("metadataDump")
  public MetadataDump getMetadataDump() {
    return metadataDump;
  }

  public void setMetadataDump(MetadataDump metadataDump) {
    this.metadataDump = metadataDump;
  }

  public ProjectMetadataResponse snapshotDownloadUrl(String snapshotDownloadUrl) {
    this.snapshotDownloadUrl = snapshotDownloadUrl;
    return this;
  }

  /**
   * Relative link to the `/project/{id}/export/snapshot` endpoint for convenience.
   * @return snapshotDownloadUrl
  */
  
  @Schema(name = "snapshotDownloadUrl", description = "Relative link to the `/project/{id}/export/snapshot` endpoint for convenience.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("snapshotDownloadUrl")
  public String getSnapshotDownloadUrl() {
    return snapshotDownloadUrl;
  }

  public void setSnapshotDownloadUrl(String snapshotDownloadUrl) {
    this.snapshotDownloadUrl = snapshotDownloadUrl;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProjectMetadataResponse projectMetadataResponse = (ProjectMetadataResponse) o;
    return Objects.equals(this.projectId, projectMetadataResponse.projectId) &&
        Objects.equals(this.projectName, projectMetadataResponse.projectName) &&
        Objects.equals(this.repoUrl, projectMetadataResponse.repoUrl) &&
        Objects.equals(this.analyzedAt, projectMetadataResponse.analyzedAt) &&
        Objects.equals(this.buildInfo, projectMetadataResponse.buildInfo) &&
        Objects.equals(this.metadataDump, projectMetadataResponse.metadataDump) &&
        Objects.equals(this.snapshotDownloadUrl, projectMetadataResponse.snapshotDownloadUrl);
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectId, projectName, repoUrl, analyzedAt, buildInfo, metadataDump, snapshotDownloadUrl);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProjectMetadataResponse {\n");
    sb.append("    projectId: ").append(toIndentedString(projectId)).append("\n");
    sb.append("    projectName: ").append(toIndentedString(projectName)).append("\n");
    sb.append("    repoUrl: ").append(toIndentedString(repoUrl)).append("\n");
    sb.append("    analyzedAt: ").append(toIndentedString(analyzedAt)).append("\n");
    sb.append("    buildInfo: ").append(toIndentedString(buildInfo)).append("\n");
    sb.append("    metadataDump: ").append(toIndentedString(metadataDump)).append("\n");
    sb.append("    snapshotDownloadUrl: ").append(toIndentedString(snapshotDownloadUrl)).append("\n");
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

