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
 * DaoOperationDetails
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-09T15:30:27.709921-05:00[America/New_York]", comments = "Generator version: 7.5.0")
public class DaoOperationDetails {

  private String methodName;

  private String operationType;

  private String target;

  private String querySnippet;

  public DaoOperationDetails() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public DaoOperationDetails(String methodName, String operationType) {
    this.methodName = methodName;
    this.operationType = operationType;
  }

  public DaoOperationDetails methodName(String methodName) {
    this.methodName = methodName;
    return this;
  }

  /**
   * Repository method name.
   * @return methodName
  */
  @NotNull 
  @Schema(name = "methodName", description = "Repository method name.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("methodName")
  public String getMethodName() {
    return methodName;
  }

  public void setMethodName(String methodName) {
    this.methodName = methodName;
  }

  public DaoOperationDetails operationType(String operationType) {
    this.operationType = operationType;
    return this;
  }

  /**
   * Inferred CRUD intent (SELECT, INSERT, UPDATE, DELETE, etc.).
   * @return operationType
  */
  @NotNull 
  @Schema(name = "operationType", description = "Inferred CRUD intent (SELECT, INSERT, UPDATE, DELETE, etc.).", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("operationType")
  public String getOperationType() {
    return operationType;
  }

  public void setOperationType(String operationType) {
    this.operationType = operationType;
  }

  public DaoOperationDetails target(String target) {
    this.target = target;
    return this;
  }

  /**
   * Entity or table targeted by the method.
   * @return target
  */
  
  @Schema(name = "target", description = "Entity or table targeted by the method.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("target")
  public String getTarget() {
    return target;
  }

  public void setTarget(String target) {
    this.target = target;
  }

  public DaoOperationDetails querySnippet(String querySnippet) {
    this.querySnippet = querySnippet;
    return this;
  }

  /**
   * Inline query text when available.
   * @return querySnippet
  */
  
  @Schema(name = "querySnippet", description = "Inline query text when available.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("querySnippet")
  public String getQuerySnippet() {
    return querySnippet;
  }

  public void setQuerySnippet(String querySnippet) {
    this.querySnippet = querySnippet;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DaoOperationDetails daoOperationDetails = (DaoOperationDetails) o;
    return Objects.equals(this.methodName, daoOperationDetails.methodName) &&
        Objects.equals(this.operationType, daoOperationDetails.operationType) &&
        Objects.equals(this.target, daoOperationDetails.target) &&
        Objects.equals(this.querySnippet, daoOperationDetails.querySnippet);
  }

  @Override
  public int hashCode() {
    return Objects.hash(methodName, operationType, target, querySnippet);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DaoOperationDetails {\n");
    sb.append("    methodName: ").append(toIndentedString(methodName)).append("\n");
    sb.append("    operationType: ").append(toIndentedString(operationType)).append("\n");
    sb.append("    target: ").append(toIndentedString(target)).append("\n");
    sb.append("    querySnippet: ").append(toIndentedString(querySnippet)).append("\n");
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

