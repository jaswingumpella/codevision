package com.codevision.codevisionbackend.api.model;

import java.net.URI;
import java.util.Objects;
import com.codevision.codevisionbackend.api.model.ApiSpecArtifact;
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
 * ApiEndpoint
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-06T22:33:59.387668-05:00[America/New_York]", comments = "Generator version: 7.5.0")
public class ApiEndpoint {

  private String protocol;

  private String httpMethod;

  private String pathOrOperation;

  private String controllerClass;

  private String controllerMethod;

  @Valid
  private List<@Valid ApiSpecArtifact> specArtifacts = new ArrayList<>();

  public ApiEndpoint() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ApiEndpoint(String protocol, String pathOrOperation, String controllerClass) {
    this.protocol = protocol;
    this.pathOrOperation = pathOrOperation;
    this.controllerClass = controllerClass;
  }

  public ApiEndpoint protocol(String protocol) {
    this.protocol = protocol;
    return this;
  }

  /**
   * Protocol classification of the endpoint (REST, SOAP, SERVLET, JAXRS).
   * @return protocol
  */
  @NotNull 
  @Schema(name = "protocol", description = "Protocol classification of the endpoint (REST, SOAP, SERVLET, JAXRS).", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("protocol")
  public String getProtocol() {
    return protocol;
  }

  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  public ApiEndpoint httpMethod(String httpMethod) {
    this.httpMethod = httpMethod;
    return this;
  }

  /**
   * HTTP method, when applicable.
   * @return httpMethod
  */
  
  @Schema(name = "httpMethod", description = "HTTP method, when applicable.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("httpMethod")
  public String getHttpMethod() {
    return httpMethod;
  }

  public void setHttpMethod(String httpMethod) {
    this.httpMethod = httpMethod;
  }

  public ApiEndpoint pathOrOperation(String pathOrOperation) {
    this.pathOrOperation = pathOrOperation;
    return this;
  }

  /**
   * Resolved path or operation identifier exposed by the endpoint.
   * @return pathOrOperation
  */
  @NotNull 
  @Schema(name = "pathOrOperation", description = "Resolved path or operation identifier exposed by the endpoint.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("pathOrOperation")
  public String getPathOrOperation() {
    return pathOrOperation;
  }

  public void setPathOrOperation(String pathOrOperation) {
    this.pathOrOperation = pathOrOperation;
  }

  public ApiEndpoint controllerClass(String controllerClass) {
    this.controllerClass = controllerClass;
    return this;
  }

  /**
   * Fully qualified class that declares the endpoint.
   * @return controllerClass
  */
  @NotNull 
  @Schema(name = "controllerClass", description = "Fully qualified class that declares the endpoint.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("controllerClass")
  public String getControllerClass() {
    return controllerClass;
  }

  public void setControllerClass(String controllerClass) {
    this.controllerClass = controllerClass;
  }

  public ApiEndpoint controllerMethod(String controllerMethod) {
    this.controllerMethod = controllerMethod;
    return this;
  }

  /**
   * Method handling the endpoint invocation.
   * @return controllerMethod
  */
  
  @Schema(name = "controllerMethod", description = "Method handling the endpoint invocation.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("controllerMethod")
  public String getControllerMethod() {
    return controllerMethod;
  }

  public void setControllerMethod(String controllerMethod) {
    this.controllerMethod = controllerMethod;
  }

  public ApiEndpoint specArtifacts(List<@Valid ApiSpecArtifact> specArtifacts) {
    this.specArtifacts = specArtifacts;
    return this;
  }

  public ApiEndpoint addSpecArtifactsItem(ApiSpecArtifact specArtifactsItem) {
    if (this.specArtifacts == null) {
      this.specArtifacts = new ArrayList<>();
    }
    this.specArtifacts.add(specArtifactsItem);
    return this;
  }

  /**
   * Related specification artifacts such as OpenAPI or WSDL documents.
   * @return specArtifacts
  */
  @Valid 
  @Schema(name = "specArtifacts", description = "Related specification artifacts such as OpenAPI or WSDL documents.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("specArtifacts")
  public List<@Valid ApiSpecArtifact> getSpecArtifacts() {
    return specArtifacts;
  }

  public void setSpecArtifacts(List<@Valid ApiSpecArtifact> specArtifacts) {
    this.specArtifacts = specArtifacts;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiEndpoint apiEndpoint = (ApiEndpoint) o;
    return Objects.equals(this.protocol, apiEndpoint.protocol) &&
        Objects.equals(this.httpMethod, apiEndpoint.httpMethod) &&
        Objects.equals(this.pathOrOperation, apiEndpoint.pathOrOperation) &&
        Objects.equals(this.controllerClass, apiEndpoint.controllerClass) &&
        Objects.equals(this.controllerMethod, apiEndpoint.controllerMethod) &&
        Objects.equals(this.specArtifacts, apiEndpoint.specArtifacts);
  }

  @Override
  public int hashCode() {
    return Objects.hash(protocol, httpMethod, pathOrOperation, controllerClass, controllerMethod, specArtifacts);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiEndpoint {\n");
    sb.append("    protocol: ").append(toIndentedString(protocol)).append("\n");
    sb.append("    httpMethod: ").append(toIndentedString(httpMethod)).append("\n");
    sb.append("    pathOrOperation: ").append(toIndentedString(pathOrOperation)).append("\n");
    sb.append("    controllerClass: ").append(toIndentedString(controllerClass)).append("\n");
    sb.append("    controllerMethod: ").append(toIndentedString(controllerMethod)).append("\n");
    sb.append("    specArtifacts: ").append(toIndentedString(specArtifacts)).append("\n");
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

