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
 * SpecDocument
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-02-01T02:54:38.384001-05:00[America/New_York]", comments = "Generator version: 7.5.0")
public class SpecDocument {

  private String fileName;

  private String content;

  public SpecDocument() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SpecDocument(String fileName, String content) {
    this.fileName = fileName;
    this.content = content;
  }

  public SpecDocument fileName(String fileName) {
    this.fileName = fileName;
    return this;
  }

  /**
   * Source file name for the document.
   * @return fileName
  */
  @NotNull 
  @Schema(name = "fileName", description = "Source file name for the document.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("fileName")
  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public SpecDocument content(String content) {
    this.content = content;
    return this;
  }

  /**
   * Raw text content of the document.
   * @return content
  */
  @NotNull 
  @Schema(name = "content", description = "Raw text content of the document.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("content")
  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SpecDocument specDocument = (SpecDocument) o;
    return Objects.equals(this.fileName, specDocument.fileName) &&
        Objects.equals(this.content, specDocument.content);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fileName, content);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SpecDocument {\n");
    sb.append("    fileName: ").append(toIndentedString(fileName)).append("\n");
    sb.append("    content: ").append(toIndentedString(content)).append("\n");
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

