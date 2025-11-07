package com.codevision.codevisionbackend.api.model;

import java.net.URI;
import java.util.Objects;
import com.codevision.codevisionbackend.api.model.ApiEndpoint;
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
 * ProjectApiEndpointsResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-06T22:23:43.718706-05:00[America/New_York]", comments = "Generator version: 7.5.0")
public class ProjectApiEndpointsResponse {

  private Long projectId;

  @Valid
  private List<@Valid ApiEndpoint> endpoints = new ArrayList<>();

  public ProjectApiEndpointsResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ProjectApiEndpointsResponse(Long projectId, List<@Valid ApiEndpoint> endpoints) {
    this.projectId = projectId;
    this.endpoints = endpoints;
  }

  public ProjectApiEndpointsResponse projectId(Long projectId) {
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

  public ProjectApiEndpointsResponse endpoints(List<@Valid ApiEndpoint> endpoints) {
    this.endpoints = endpoints;
    return this;
  }

  public ProjectApiEndpointsResponse addEndpointsItem(ApiEndpoint endpointsItem) {
    if (this.endpoints == null) {
      this.endpoints = new ArrayList<>();
    }
    this.endpoints.add(endpointsItem);
    return this;
  }

  /**
   * Endpoint catalog for the project.
   * @return endpoints
  */
  @NotNull @Valid 
  @Schema(name = "endpoints", description = "Endpoint catalog for the project.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("endpoints")
  public List<@Valid ApiEndpoint> getEndpoints() {
    return endpoints;
  }

  public void setEndpoints(List<@Valid ApiEndpoint> endpoints) {
    this.endpoints = endpoints;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProjectApiEndpointsResponse projectApiEndpointsResponse = (ProjectApiEndpointsResponse) o;
    return Objects.equals(this.projectId, projectApiEndpointsResponse.projectId) &&
        Objects.equals(this.endpoints, projectApiEndpointsResponse.endpoints);
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectId, endpoints);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProjectApiEndpointsResponse {\n");
    sb.append("    projectId: ").append(toIndentedString(projectId)).append("\n");
    sb.append("    endpoints: ").append(toIndentedString(endpoints)).append("\n");
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

