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
 * UpdatePiiFindingRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-10T00:23:13.435459-05:00[America/New_York]", comments = "Generator version: 7.5.0")
public class UpdatePiiFindingRequest {

  private Boolean ignored;

  public UpdatePiiFindingRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public UpdatePiiFindingRequest(Boolean ignored) {
    this.ignored = ignored;
  }

  public UpdatePiiFindingRequest ignored(Boolean ignored) {
    this.ignored = ignored;
    return this;
  }

  /**
   * Indicates whether the finding should be suppressed in future responses.
   * @return ignored
  */
  @NotNull 
  @Schema(name = "ignored", description = "Indicates whether the finding should be suppressed in future responses.", requiredMode = Schema.RequiredMode.REQUIRED)
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
    UpdatePiiFindingRequest updatePiiFindingRequest = (UpdatePiiFindingRequest) o;
    return Objects.equals(this.ignored, updatePiiFindingRequest.ignored);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ignored);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdatePiiFindingRequest {\n");
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

