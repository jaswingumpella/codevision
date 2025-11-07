package com.codevision.codevisionbackend.api.model;

import java.net.URI;
import java.util.Objects;
import com.codevision.codevisionbackend.api.model.LoggerInsight;
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
 * ProjectLoggerInsightsResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-06T22:33:59.387668-05:00[America/New_York]", comments = "Generator version: 7.5.0")
public class ProjectLoggerInsightsResponse {

  private Long projectId;

  @Valid
  private List<@Valid LoggerInsight> loggerInsights = new ArrayList<>();

  public ProjectLoggerInsightsResponse projectId(Long projectId) {
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

  public ProjectLoggerInsightsResponse loggerInsights(List<@Valid LoggerInsight> loggerInsights) {
    this.loggerInsights = loggerInsights;
    return this;
  }

  public ProjectLoggerInsightsResponse addLoggerInsightsItem(LoggerInsight loggerInsightsItem) {
    if (this.loggerInsights == null) {
      this.loggerInsights = new ArrayList<>();
    }
    this.loggerInsights.add(loggerInsightsItem);
    return this;
  }

  /**
   * Get loggerInsights
   * @return loggerInsights
  */
  @Valid 
  @Schema(name = "loggerInsights", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("loggerInsights")
  public List<@Valid LoggerInsight> getLoggerInsights() {
    return loggerInsights;
  }

  public void setLoggerInsights(List<@Valid LoggerInsight> loggerInsights) {
    this.loggerInsights = loggerInsights;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProjectLoggerInsightsResponse projectLoggerInsightsResponse = (ProjectLoggerInsightsResponse) o;
    return Objects.equals(this.projectId, projectLoggerInsightsResponse.projectId) &&
        Objects.equals(this.loggerInsights, projectLoggerInsightsResponse.loggerInsights);
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectId, loggerInsights);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProjectLoggerInsightsResponse {\n");
    sb.append("    projectId: ").append(toIndentedString(projectId)).append("\n");
    sb.append("    loggerInsights: ").append(toIndentedString(loggerInsights)).append("\n");
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

