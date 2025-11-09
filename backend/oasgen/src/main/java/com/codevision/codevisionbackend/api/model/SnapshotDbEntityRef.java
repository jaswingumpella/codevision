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
 * SnapshotDbEntityRef
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-09T15:30:27.709921-05:00[America/New_York]", comments = "Generator version: 7.5.0")
public class SnapshotDbEntityRef {

  private String entityName;

  private String tableName;

  public SnapshotDbEntityRef entityName(String entityName) {
    this.entityName = entityName;
    return this;
  }

  /**
   * Get entityName
   * @return entityName
  */
  
  @Schema(name = "entityName", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("entityName")
  public String getEntityName() {
    return entityName;
  }

  public void setEntityName(String entityName) {
    this.entityName = entityName;
  }

  public SnapshotDbEntityRef tableName(String tableName) {
    this.tableName = tableName;
    return this;
  }

  /**
   * Get tableName
   * @return tableName
  */
  
  @Schema(name = "tableName", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("tableName")
  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SnapshotDbEntityRef snapshotDbEntityRef = (SnapshotDbEntityRef) o;
    return Objects.equals(this.entityName, snapshotDbEntityRef.entityName) &&
        Objects.equals(this.tableName, snapshotDbEntityRef.tableName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(entityName, tableName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SnapshotDbEntityRef {\n");
    sb.append("    entityName: ").append(toIndentedString(entityName)).append("\n");
    sb.append("    tableName: ").append(toIndentedString(tableName)).append("\n");
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

