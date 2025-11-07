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
 * DbFieldSummary
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-07T01:12:28.278930-05:00[America/New_York]", comments = "Generator version: 7.5.0")
public class DbFieldSummary {

  private String name;

  private String type;

  private String columnName;

  public DbFieldSummary() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public DbFieldSummary(String name, String type) {
    this.name = name;
    this.type = type;
  }

  public DbFieldSummary name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Field name as declared in the entity.
   * @return name
  */
  @NotNull 
  @Schema(name = "name", description = "Field name as declared in the entity.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public DbFieldSummary type(String type) {
    this.type = type;
    return this;
  }

  /**
   * Java type of the field.
   * @return type
  */
  @NotNull 
  @Schema(name = "type", description = "Java type of the field.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("type")
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public DbFieldSummary columnName(String columnName) {
    this.columnName = columnName;
    return this;
  }

  /**
   * Database column mapped to the field when provided.
   * @return columnName
  */
  
  @Schema(name = "columnName", description = "Database column mapped to the field when provided.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("columnName")
  public String getColumnName() {
    return columnName;
  }

  public void setColumnName(String columnName) {
    this.columnName = columnName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DbFieldSummary dbFieldSummary = (DbFieldSummary) o;
    return Objects.equals(this.name, dbFieldSummary.name) &&
        Objects.equals(this.type, dbFieldSummary.type) &&
        Objects.equals(this.columnName, dbFieldSummary.columnName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type, columnName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DbFieldSummary {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    columnName: ").append(toIndentedString(columnName)).append("\n");
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

