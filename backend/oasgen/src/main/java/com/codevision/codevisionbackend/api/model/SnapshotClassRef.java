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
 * SnapshotClassRef
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-09T21:44:00.250675-05:00[America/New_York]", comments = "Generator version: 7.5.0")
public class SnapshotClassRef {

  private String fullyQualifiedName;

  private String stereotype;

  public SnapshotClassRef fullyQualifiedName(String fullyQualifiedName) {
    this.fullyQualifiedName = fullyQualifiedName;
    return this;
  }

  /**
   * Get fullyQualifiedName
   * @return fullyQualifiedName
  */
  
  @Schema(name = "fullyQualifiedName", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("fullyQualifiedName")
  public String getFullyQualifiedName() {
    return fullyQualifiedName;
  }

  public void setFullyQualifiedName(String fullyQualifiedName) {
    this.fullyQualifiedName = fullyQualifiedName;
  }

  public SnapshotClassRef stereotype(String stereotype) {
    this.stereotype = stereotype;
    return this;
  }

  /**
   * Get stereotype
   * @return stereotype
  */
  
  @Schema(name = "stereotype", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("stereotype")
  public String getStereotype() {
    return stereotype;
  }

  public void setStereotype(String stereotype) {
    this.stereotype = stereotype;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SnapshotClassRef snapshotClassRef = (SnapshotClassRef) o;
    return Objects.equals(this.fullyQualifiedName, snapshotClassRef.fullyQualifiedName) &&
        Objects.equals(this.stereotype, snapshotClassRef.stereotype);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fullyQualifiedName, stereotype);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SnapshotClassRef {\n");
    sb.append("    fullyQualifiedName: ").append(toIndentedString(fullyQualifiedName)).append("\n");
    sb.append("    stereotype: ").append(toIndentedString(stereotype)).append("\n");
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

