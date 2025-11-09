package com.codevision.codevisionbackend.api.model;

import java.net.URI;
import java.util.Objects;
import com.codevision.codevisionbackend.api.model.DbAnalysis;
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
 * ProjectDbAnalysisResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-09T15:19:53.093271-05:00[America/New_York]", comments = "Generator version: 7.5.0")
public class ProjectDbAnalysisResponse {

  private Long projectId;

  private DbAnalysis dbAnalysis;

  public ProjectDbAnalysisResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ProjectDbAnalysisResponse(Long projectId, DbAnalysis dbAnalysis) {
    this.projectId = projectId;
    this.dbAnalysis = dbAnalysis;
  }

  public ProjectDbAnalysisResponse projectId(Long projectId) {
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

  public ProjectDbAnalysisResponse dbAnalysis(DbAnalysis dbAnalysis) {
    this.dbAnalysis = dbAnalysis;
    return this;
  }

  /**
   * Get dbAnalysis
   * @return dbAnalysis
  */
  @NotNull @Valid 
  @Schema(name = "dbAnalysis", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("dbAnalysis")
  public DbAnalysis getDbAnalysis() {
    return dbAnalysis;
  }

  public void setDbAnalysis(DbAnalysis dbAnalysis) {
    this.dbAnalysis = dbAnalysis;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProjectDbAnalysisResponse projectDbAnalysisResponse = (ProjectDbAnalysisResponse) o;
    return Objects.equals(this.projectId, projectDbAnalysisResponse.projectId) &&
        Objects.equals(this.dbAnalysis, projectDbAnalysisResponse.dbAnalysis);
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectId, dbAnalysis);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProjectDbAnalysisResponse {\n");
    sb.append("    projectId: ").append(toIndentedString(projectId)).append("\n");
    sb.append("    dbAnalysis: ").append(toIndentedString(dbAnalysis)).append("\n");
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

