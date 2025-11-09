package com.codevision.codevisionbackend.api.model;

import java.net.URI;
import java.util.Objects;
import com.codevision.codevisionbackend.api.model.DiagramDescriptor;
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
 * ProjectDiagramsResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-09T15:05:41.709386-05:00[America/New_York]", comments = "Generator version: 7.5.0")
public class ProjectDiagramsResponse {

  private Long projectId;

  @Valid
  private List<@Valid DiagramDescriptor> diagrams = new ArrayList<>();

  public ProjectDiagramsResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ProjectDiagramsResponse(Long projectId, List<@Valid DiagramDescriptor> diagrams) {
    this.projectId = projectId;
    this.diagrams = diagrams;
  }

  public ProjectDiagramsResponse projectId(Long projectId) {
    this.projectId = projectId;
    return this;
  }

  /**
   * Get projectId
   * @return projectId
  */
  @NotNull 
  @Schema(name = "projectId", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("projectId")
  public Long getProjectId() {
    return projectId;
  }

  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }

  public ProjectDiagramsResponse diagrams(List<@Valid DiagramDescriptor> diagrams) {
    this.diagrams = diagrams;
    return this;
  }

  public ProjectDiagramsResponse addDiagramsItem(DiagramDescriptor diagramsItem) {
    if (this.diagrams == null) {
      this.diagrams = new ArrayList<>();
    }
    this.diagrams.add(diagramsItem);
    return this;
  }

  /**
   * Get diagrams
   * @return diagrams
  */
  @NotNull @Valid 
  @Schema(name = "diagrams", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("diagrams")
  public List<@Valid DiagramDescriptor> getDiagrams() {
    return diagrams;
  }

  public void setDiagrams(List<@Valid DiagramDescriptor> diagrams) {
    this.diagrams = diagrams;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProjectDiagramsResponse projectDiagramsResponse = (ProjectDiagramsResponse) o;
    return Objects.equals(this.projectId, projectDiagramsResponse.projectId) &&
        Objects.equals(this.diagrams, projectDiagramsResponse.diagrams);
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectId, diagrams);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProjectDiagramsResponse {\n");
    sb.append("    projectId: ").append(toIndentedString(projectId)).append("\n");
    sb.append("    diagrams: ").append(toIndentedString(diagrams)).append("\n");
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

