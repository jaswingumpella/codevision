package com.codevision.codevisionbackend.api.model;

import java.net.URI;
import java.util.Objects;
import com.codevision.codevisionbackend.api.model.PiiPciFinding;
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
 * ProjectPiiPciResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-07T00:36:01.617691-05:00[America/New_York]", comments = "Generator version: 7.5.0")
public class ProjectPiiPciResponse {

  private Long projectId;

  @Valid
  private List<@Valid PiiPciFinding> findings = new ArrayList<>();

  public ProjectPiiPciResponse projectId(Long projectId) {
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

  public ProjectPiiPciResponse findings(List<@Valid PiiPciFinding> findings) {
    this.findings = findings;
    return this;
  }

  public ProjectPiiPciResponse addFindingsItem(PiiPciFinding findingsItem) {
    if (this.findings == null) {
      this.findings = new ArrayList<>();
    }
    this.findings.add(findingsItem);
    return this;
  }

  /**
   * Get findings
   * @return findings
  */
  @Valid 
  @Schema(name = "findings", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("findings")
  public List<@Valid PiiPciFinding> getFindings() {
    return findings;
  }

  public void setFindings(List<@Valid PiiPciFinding> findings) {
    this.findings = findings;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProjectPiiPciResponse projectPiiPciResponse = (ProjectPiiPciResponse) o;
    return Objects.equals(this.projectId, projectPiiPciResponse.projectId) &&
        Objects.equals(this.findings, projectPiiPciResponse.findings);
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectId, findings);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProjectPiiPciResponse {\n");
    sb.append("    projectId: ").append(toIndentedString(projectId)).append("\n");
    sb.append("    findings: ").append(toIndentedString(findings)).append("\n");
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

