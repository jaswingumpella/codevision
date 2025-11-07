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
 * PiiPciFinding
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-06T21:06:48.004750-05:00[America/New_York]", comments = "Generator version: 7.5.0")
public class PiiPciFinding {

  private String filePath;

  private Integer lineNumber;

  private String snippet;

  private String matchType;

  private String severity;

  private Boolean ignored;

  public PiiPciFinding() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PiiPciFinding(String filePath, String matchType, String severity) {
    this.filePath = filePath;
    this.matchType = matchType;
    this.severity = severity;
  }

  public PiiPciFinding filePath(String filePath) {
    this.filePath = filePath;
    return this;
  }

  /**
   * File containing the match.
   * @return filePath
  */
  @NotNull 
  @Schema(name = "filePath", description = "File containing the match.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("filePath")
  public String getFilePath() {
    return filePath;
  }

  public void setFilePath(String filePath) {
    this.filePath = filePath;
  }

  public PiiPciFinding lineNumber(Integer lineNumber) {
    this.lineNumber = lineNumber;
    return this;
  }

  /**
   * Line number of the match in the file.
   * @return lineNumber
  */
  
  @Schema(name = "lineNumber", description = "Line number of the match in the file.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lineNumber")
  public Integer getLineNumber() {
    return lineNumber;
  }

  public void setLineNumber(Integer lineNumber) {
    this.lineNumber = lineNumber;
  }

  public PiiPciFinding snippet(String snippet) {
    this.snippet = snippet;
    return this;
  }

  /**
   * Snippet that triggered the rule.
   * @return snippet
  */
  
  @Schema(name = "snippet", description = "Snippet that triggered the rule.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("snippet")
  public String getSnippet() {
    return snippet;
  }

  public void setSnippet(String snippet) {
    this.snippet = snippet;
  }

  public PiiPciFinding matchType(String matchType) {
    this.matchType = matchType;
    return this;
  }

  /**
   * Classification for the rule (PII or PCI).
   * @return matchType
  */
  @NotNull 
  @Schema(name = "matchType", description = "Classification for the rule (PII or PCI).", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("matchType")
  public String getMatchType() {
    return matchType;
  }

  public void setMatchType(String matchType) {
    this.matchType = matchType;
  }

  public PiiPciFinding severity(String severity) {
    this.severity = severity;
    return this;
  }

  /**
   * Severity assigned in configuration.
   * @return severity
  */
  @NotNull 
  @Schema(name = "severity", description = "Severity assigned in configuration.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("severity")
  public String getSeverity() {
    return severity;
  }

  public void setSeverity(String severity) {
    this.severity = severity;
  }

  public PiiPciFinding ignored(Boolean ignored) {
    this.ignored = ignored;
    return this;
  }

  /**
   * Indicates if the match was suppressed by an ignore pattern.
   * @return ignored
  */
  
  @Schema(name = "ignored", description = "Indicates if the match was suppressed by an ignore pattern.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("ignored")
  public Boolean getIgnored() {
    return ignored;
  }

  public void setIgnored(Boolean ignored) {
    this.ignored = ignored;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PiiPciFinding piiPciFinding = (PiiPciFinding) o;
    return Objects.equals(this.filePath, piiPciFinding.filePath) &&
        Objects.equals(this.lineNumber, piiPciFinding.lineNumber) &&
        Objects.equals(this.snippet, piiPciFinding.snippet) &&
        Objects.equals(this.matchType, piiPciFinding.matchType) &&
        Objects.equals(this.severity, piiPciFinding.severity) &&
        Objects.equals(this.ignored, piiPciFinding.ignored);
  }

  @Override
  public int hashCode() {
    return Objects.hash(filePath, lineNumber, snippet, matchType, severity, ignored);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PiiPciFinding {\n");
    sb.append("    filePath: ").append(toIndentedString(filePath)).append("\n");
    sb.append("    lineNumber: ").append(toIndentedString(lineNumber)).append("\n");
    sb.append("    snippet: ").append(toIndentedString(snippet)).append("\n");
    sb.append("    matchType: ").append(toIndentedString(matchType)).append("\n");
    sb.append("    severity: ").append(toIndentedString(severity)).append("\n");
    sb.append("    ignored: ").append(toIndentedString(ignored)).append("\n");
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

