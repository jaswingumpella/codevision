package com.codevision.codevisionbackend.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.net.URI;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * AnalyzeRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-10-27T08:11:19.051018-04:00[America/New_York]", comments = "Generator version: 7.5.0")
public class AnalyzeRequest {

  private URI repoUrl;

  public AnalyzeRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AnalyzeRequest(URI repoUrl) {
    this.repoUrl = repoUrl;
  }

  public AnalyzeRequest repoUrl(URI repoUrl) {
    this.repoUrl = repoUrl;
    return this;
  }

  /**
   * Git repository URL that should be analyzed.
   * @return repoUrl
  */
  @NotNull @Valid 
  @Schema(name = "repoUrl", description = "Git repository URL that should be analyzed.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("repoUrl")
  public URI getRepoUrl() {
    return repoUrl;
  }

  public void setRepoUrl(URI repoUrl) {
    this.repoUrl = repoUrl;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AnalyzeRequest analyzeRequest = (AnalyzeRequest) o;
    return Objects.equals(this.repoUrl, analyzeRequest.repoUrl);
  }

  @Override
  public int hashCode() {
    return Objects.hash(repoUrl);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AnalyzeRequest {\n");
    sb.append("    repoUrl: ").append(toIndentedString(repoUrl)).append("\n");
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

