package com.codevision.codevisionbackend.api.model;

import java.net.URI;
import java.util.Objects;
import com.codevision.codevisionbackend.api.model.DbFieldSummary;
import com.codevision.codevisionbackend.api.model.DbRelationshipSummary;
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
 * DbEntitySummary
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-06T23:14:24.607561-05:00[America/New_York]", comments = "Generator version: 7.5.0")
public class DbEntitySummary {

  private String entityName;

  private String fullyQualifiedName;

  private String tableName;

  @Valid
  private List<String> primaryKeys = new ArrayList<>();

  @Valid
  private List<@Valid DbFieldSummary> fields = new ArrayList<>();

  @Valid
  private List<@Valid DbRelationshipSummary> relationships = new ArrayList<>();

  public DbEntitySummary() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public DbEntitySummary(String entityName) {
    this.entityName = entityName;
  }

  public DbEntitySummary entityName(String entityName) {
    this.entityName = entityName;
    return this;
  }

  /**
   * Simple name of the entity class.
   * @return entityName
  */
  @NotNull 
  @Schema(name = "entityName", description = "Simple name of the entity class.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("entityName")
  public String getEntityName() {
    return entityName;
  }

  public void setEntityName(String entityName) {
    this.entityName = entityName;
  }

  public DbEntitySummary fullyQualifiedName(String fullyQualifiedName) {
    this.fullyQualifiedName = fullyQualifiedName;
    return this;
  }

  /**
   * Fully qualified class name of the entity.
   * @return fullyQualifiedName
  */
  
  @Schema(name = "fullyQualifiedName", description = "Fully qualified class name of the entity.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("fullyQualifiedName")
  public String getFullyQualifiedName() {
    return fullyQualifiedName;
  }

  public void setFullyQualifiedName(String fullyQualifiedName) {
    this.fullyQualifiedName = fullyQualifiedName;
  }

  public DbEntitySummary tableName(String tableName) {
    this.tableName = tableName;
    return this;
  }

  /**
   * Database table name mapped to the entity.
   * @return tableName
  */
  
  @Schema(name = "tableName", description = "Database table name mapped to the entity.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("tableName")
  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public DbEntitySummary primaryKeys(List<String> primaryKeys) {
    this.primaryKeys = primaryKeys;
    return this;
  }

  public DbEntitySummary addPrimaryKeysItem(String primaryKeysItem) {
    if (this.primaryKeys == null) {
      this.primaryKeys = new ArrayList<>();
    }
    this.primaryKeys.add(primaryKeysItem);
    return this;
  }

  /**
   * Fields designated as primary keys.
   * @return primaryKeys
  */
  
  @Schema(name = "primaryKeys", description = "Fields designated as primary keys.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("primaryKeys")
  public List<String> getPrimaryKeys() {
    return primaryKeys;
  }

  public void setPrimaryKeys(List<String> primaryKeys) {
    this.primaryKeys = primaryKeys;
  }

  public DbEntitySummary fields(List<@Valid DbFieldSummary> fields) {
    this.fields = fields;
    return this;
  }

  public DbEntitySummary addFieldsItem(DbFieldSummary fieldsItem) {
    if (this.fields == null) {
      this.fields = new ArrayList<>();
    }
    this.fields.add(fieldsItem);
    return this;
  }

  /**
   * Declared fields and their column mappings.
   * @return fields
  */
  @Valid 
  @Schema(name = "fields", description = "Declared fields and their column mappings.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("fields")
  public List<@Valid DbFieldSummary> getFields() {
    return fields;
  }

  public void setFields(List<@Valid DbFieldSummary> fields) {
    this.fields = fields;
  }

  public DbEntitySummary relationships(List<@Valid DbRelationshipSummary> relationships) {
    this.relationships = relationships;
    return this;
  }

  public DbEntitySummary addRelationshipsItem(DbRelationshipSummary relationshipsItem) {
    if (this.relationships == null) {
      this.relationships = new ArrayList<>();
    }
    this.relationships.add(relationshipsItem);
    return this;
  }

  /**
   * Entity relationships detected via JPA annotations.
   * @return relationships
  */
  @Valid 
  @Schema(name = "relationships", description = "Entity relationships detected via JPA annotations.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("relationships")
  public List<@Valid DbRelationshipSummary> getRelationships() {
    return relationships;
  }

  public void setRelationships(List<@Valid DbRelationshipSummary> relationships) {
    this.relationships = relationships;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DbEntitySummary dbEntitySummary = (DbEntitySummary) o;
    return Objects.equals(this.entityName, dbEntitySummary.entityName) &&
        Objects.equals(this.fullyQualifiedName, dbEntitySummary.fullyQualifiedName) &&
        Objects.equals(this.tableName, dbEntitySummary.tableName) &&
        Objects.equals(this.primaryKeys, dbEntitySummary.primaryKeys) &&
        Objects.equals(this.fields, dbEntitySummary.fields) &&
        Objects.equals(this.relationships, dbEntitySummary.relationships);
  }

  @Override
  public int hashCode() {
    return Objects.hash(entityName, fullyQualifiedName, tableName, primaryKeys, fields, relationships);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DbEntitySummary {\n");
    sb.append("    entityName: ").append(toIndentedString(entityName)).append("\n");
    sb.append("    fullyQualifiedName: ").append(toIndentedString(fullyQualifiedName)).append("\n");
    sb.append("    tableName: ").append(toIndentedString(tableName)).append("\n");
    sb.append("    primaryKeys: ").append(toIndentedString(primaryKeys)).append("\n");
    sb.append("    fields: ").append(toIndentedString(fields)).append("\n");
    sb.append("    relationships: ").append(toIndentedString(relationships)).append("\n");
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

