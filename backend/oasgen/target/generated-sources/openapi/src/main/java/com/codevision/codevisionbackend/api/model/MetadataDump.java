package com.codevision.codevisionbackend.api.model;

import java.net.URI;
import java.util.Objects;
import com.codevision.codevisionbackend.api.model.OpenApiSpec;
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
 * MetadataDump
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-10-27T02:19:03.945888-04:00[America/New_York]", comments = "Generator version: 7.5.0")
public class MetadataDump {

  @Valid
  private List<@Valid OpenApiSpec> openApiSpecs = new ArrayList<>();

  public MetadataDump openApiSpecs(List<@Valid OpenApiSpec> openApiSpecs) {
    this.openApiSpecs = openApiSpecs;
    return this;
  }

  public MetadataDump addOpenApiSpecsItem(OpenApiSpec openApiSpecsItem) {
    if (this.openApiSpecs == null) {
      this.openApiSpecs = new ArrayList<>();
    }
    this.openApiSpecs.add(openApiSpecsItem);
    return this;
  }

  /**
   * Collection of OpenAPI specification files detected within the repository.
   * @return openApiSpecs
  */
  @Valid 
  @Schema(name = "openApiSpecs", description = "Collection of OpenAPI specification files detected within the repository.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("openApiSpecs")
  public List<@Valid OpenApiSpec> getOpenApiSpecs() {
    return openApiSpecs;
  }

  public void setOpenApiSpecs(List<@Valid OpenApiSpec> openApiSpecs) {
    this.openApiSpecs = openApiSpecs;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MetadataDump metadataDump = (MetadataDump) o;
    return Objects.equals(this.openApiSpecs, metadataDump.openApiSpecs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(openApiSpecs);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MetadataDump {\n");
    sb.append("    openApiSpecs: ").append(toIndentedString(openApiSpecs)).append("\n");
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

