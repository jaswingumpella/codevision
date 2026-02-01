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
 * ApiSpecArtifact
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-02-01T02:54:38.384001-05:00[America/New_York]", comments = "Generator version: 7.5.0")
public class ApiSpecArtifact {

  private String type;

  private String name;

  private String reference;

  public ApiSpecArtifact() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ApiSpecArtifact(String type, String name) {
    this.type = type;
    this.name = name;
  }

  public ApiSpecArtifact type(String type) {
    this.type = type;
    return this;
  }

  /**
   * Artifact type, e.g. OPENAPI, WSDL, XSD, SERVLET_MAPPING.
   * @return type
  */
  @NotNull 
  @Schema(name = "type", description = "Artifact type, e.g. OPENAPI, WSDL, XSD, SERVLET_MAPPING.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("type")
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public ApiSpecArtifact name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Human friendly label for the artifact.
   * @return name
  */
  @NotNull 
  @Schema(name = "name", description = "Human friendly label for the artifact.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiSpecArtifact reference(String reference) {
    this.reference = reference;
    return this;
  }

  /**
   * Identifier used to resolve the artifact content.
   * @return reference
  */
  
  @Schema(name = "reference", description = "Identifier used to resolve the artifact content.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("reference")
  public String getReference() {
    return reference;
  }

  public void setReference(String reference) {
    this.reference = reference;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiSpecArtifact apiSpecArtifact = (ApiSpecArtifact) o;
    return Objects.equals(this.type, apiSpecArtifact.type) &&
        Objects.equals(this.name, apiSpecArtifact.name) &&
        Objects.equals(this.reference, apiSpecArtifact.reference);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, name, reference);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiSpecArtifact {\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    reference: ").append(toIndentedString(reference)).append("\n");
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

