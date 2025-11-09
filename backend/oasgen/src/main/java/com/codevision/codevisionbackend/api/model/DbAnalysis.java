package com.codevision.codevisionbackend.api.model;

import java.net.URI;
import java.util.Objects;
import com.codevision.codevisionbackend.api.model.DaoOperationDetails;
import com.codevision.codevisionbackend.api.model.DbEntitySummary;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * DbAnalysis
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-09T00:07:15.283463-05:00[America/New_York]", comments = "Generator version: 7.5.0")
public class DbAnalysis {

  @Valid
  private List<@Valid DbEntitySummary> entities = new ArrayList<>();

  @Valid
  private Map<String, List<String>> classesByEntity = new HashMap<>();

  @Valid
  private Map<String, List<@Valid DaoOperationDetails>> operationsByClass = new HashMap<>();

  public DbAnalysis entities(List<@Valid DbEntitySummary> entities) {
    this.entities = entities;
    return this;
  }

  public DbAnalysis addEntitiesItem(DbEntitySummary entitiesItem) {
    if (this.entities == null) {
      this.entities = new ArrayList<>();
    }
    this.entities.add(entitiesItem);
    return this;
  }

  /**
   * Summaries of JPA entities and their fields.
   * @return entities
  */
  @Valid 
  @Schema(name = "entities", description = "Summaries of JPA entities and their fields.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("entities")
  public List<@Valid DbEntitySummary> getEntities() {
    return entities;
  }

  public void setEntities(List<@Valid DbEntitySummary> entities) {
    this.entities = entities;
  }

  public DbAnalysis classesByEntity(Map<String, List<String>> classesByEntity) {
    this.classesByEntity = classesByEntity;
    return this;
  }

  public DbAnalysis putClassesByEntityItem(String key, List<String> classesByEntityItem) {
    if (this.classesByEntity == null) {
      this.classesByEntity = new HashMap<>();
    }
    this.classesByEntity.put(key, classesByEntityItem);
    return this;
  }

  /**
   * Mapping of entity name to repository classes interacting with it.
   * @return classesByEntity
  */
  @Valid 
  @Schema(name = "classesByEntity", description = "Mapping of entity name to repository classes interacting with it.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("classesByEntity")
  public Map<String, List<String>> getClassesByEntity() {
    return classesByEntity;
  }

  public void setClassesByEntity(Map<String, List<String>> classesByEntity) {
    this.classesByEntity = classesByEntity;
  }

  public DbAnalysis operationsByClass(Map<String, List<@Valid DaoOperationDetails>> operationsByClass) {
    this.operationsByClass = operationsByClass;
    return this;
  }

  public DbAnalysis putOperationsByClassItem(String key, List<@Valid DaoOperationDetails> operationsByClassItem) {
    if (this.operationsByClass == null) {
      this.operationsByClass = new HashMap<>();
    }
    this.operationsByClass.put(key, operationsByClassItem);
    return this;
  }

  /**
   * Mapping of repository class to its detected CRUD operations.
   * @return operationsByClass
  */
  @Valid 
  @Schema(name = "operationsByClass", description = "Mapping of repository class to its detected CRUD operations.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("operationsByClass")
  public Map<String, List<@Valid DaoOperationDetails>> getOperationsByClass() {
    return operationsByClass;
  }

  public void setOperationsByClass(Map<String, List<@Valid DaoOperationDetails>> operationsByClass) {
    this.operationsByClass = operationsByClass;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DbAnalysis dbAnalysis = (DbAnalysis) o;
    return Objects.equals(this.entities, dbAnalysis.entities) &&
        Objects.equals(this.classesByEntity, dbAnalysis.classesByEntity) &&
        Objects.equals(this.operationsByClass, dbAnalysis.operationsByClass);
  }

  @Override
  public int hashCode() {
    return Objects.hash(entities, classesByEntity, operationsByClass);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DbAnalysis {\n");
    sb.append("    entities: ").append(toIndentedString(entities)).append("\n");
    sb.append("    classesByEntity: ").append(toIndentedString(classesByEntity)).append("\n");
    sb.append("    operationsByClass: ").append(toIndentedString(operationsByClass)).append("\n");
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

