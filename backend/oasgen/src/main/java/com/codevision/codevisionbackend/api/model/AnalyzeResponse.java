package com.codevision.codevisionbackend.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-07T00:31:52.729797-05:00[America/New_York]", comments = "Generator version: 7.5.0")
public class AnalyzeResponse {

  private Long projectId;

  private String status;

  public AnalyzeResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AnalyzeResponse(Long projectId, String status) {
    this.projectId = projectId;
    this.status = status;
  }

  public AnalyzeResponse projectId(Long projectId) {
    this.projectId = projectId;
    return this;
  }

  /**
   * Identifier assigned to the analyzed project.
   * @return projectId
  */
  @NotNull 
  @Schema(name = "projectId", description = "Identifier assigned to the analyzed project.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("projectId")
  public Long getProjectId() {
    return projectId;
  }

  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }

  public AnalyzeResponse status(String status) {
    this.status = status;
    return this;
  }

  /**
   * Current status of the analysis workflow.
   * @return status
  */
  @NotNull 
  @Schema(name = "status", example = "ANALYZED_METADATA", description = "Current status of the analysis workflow.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("status")
  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
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
    return Objects.equals(this.projectId, analyzeResponse.projectId) &&
        Objects.equals(this.status, analyzeResponse.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectId, status);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AnalyzeResponse {\n");
    sb.append("    projectId: ").append(toIndentedString(projectId)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
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

