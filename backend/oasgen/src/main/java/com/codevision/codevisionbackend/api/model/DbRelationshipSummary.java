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
 * DbRelationshipSummary
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-09T15:19:53.093271-05:00[America/New_York]", comments = "Generator version: 7.5.0")
public class DbRelationshipSummary {

  private String fieldName;

  private String targetType;

  private String relationshipType;

  public DbRelationshipSummary() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public DbRelationshipSummary(String fieldName, String relationshipType) {
    this.fieldName = fieldName;
    this.relationshipType = relationshipType;
  }

  public DbRelationshipSummary fieldName(String fieldName) {
    this.fieldName = fieldName;
    return this;
  }

  /**
   * Field participating in the relationship.
   * @return fieldName
  */
  @NotNull 
  @Schema(name = "fieldName", description = "Field participating in the relationship.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("fieldName")
  public String getFieldName() {
    return fieldName;
  }

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }

  public DbRelationshipSummary targetType(String targetType) {
    this.targetType = targetType;
    return this;
  }

  /**
   * Target entity or collection element type.
   * @return targetType
  */
  
  @Schema(name = "targetType", description = "Target entity or collection element type.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("targetType")
  public String getTargetType() {
    return targetType;
  }

  public void setTargetType(String targetType) {
    this.targetType = targetType;
  }

  public DbRelationshipSummary relationshipType(String relationshipType) {
    this.relationshipType = relationshipType;
    return this;
  }

  /**
   * Relationship classification such as ONE_TO_MANY.
   * @return relationshipType
  */
  @NotNull 
  @Schema(name = "relationshipType", description = "Relationship classification such as ONE_TO_MANY.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("relationshipType")
  public String getRelationshipType() {
    return relationshipType;
  }

  public void setRelationshipType(String relationshipType) {
    this.relationshipType = relationshipType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DbRelationshipSummary dbRelationshipSummary = (DbRelationshipSummary) o;
    return Objects.equals(this.fieldName, dbRelationshipSummary.fieldName) &&
        Objects.equals(this.targetType, dbRelationshipSummary.targetType) &&
        Objects.equals(this.relationshipType, dbRelationshipSummary.relationshipType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fieldName, targetType, relationshipType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DbRelationshipSummary {\n");
    sb.append("    fieldName: ").append(toIndentedString(fieldName)).append("\n");
    sb.append("    targetType: ").append(toIndentedString(targetType)).append("\n");
    sb.append("    relationshipType: ").append(toIndentedString(relationshipType)).append("\n");
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

