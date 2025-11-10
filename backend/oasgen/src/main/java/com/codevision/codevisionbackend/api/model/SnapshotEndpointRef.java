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
 * SnapshotEndpointRef
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-09T21:44:00.250675-05:00[America/New_York]", comments = "Generator version: 7.5.0")
public class SnapshotEndpointRef {

  private String protocol;

  private String httpMethod;

  private String pathOrOperation;

  public SnapshotEndpointRef protocol(String protocol) {
    this.protocol = protocol;
    return this;
  }

  /**
   * Get protocol
   * @return protocol
  */
  
  @Schema(name = "protocol", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("protocol")
  public String getProtocol() {
    return protocol;
  }

  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  public SnapshotEndpointRef httpMethod(String httpMethod) {
    this.httpMethod = httpMethod;
    return this;
  }

  /**
   * Get httpMethod
   * @return httpMethod
  */
  
  @Schema(name = "httpMethod", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("httpMethod")
  public String getHttpMethod() {
    return httpMethod;
  }

  public void setHttpMethod(String httpMethod) {
    this.httpMethod = httpMethod;
  }

  public SnapshotEndpointRef pathOrOperation(String pathOrOperation) {
    this.pathOrOperation = pathOrOperation;
    return this;
  }

  /**
   * Get pathOrOperation
   * @return pathOrOperation
  */
  
  @Schema(name = "pathOrOperation", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("pathOrOperation")
  public String getPathOrOperation() {
    return pathOrOperation;
  }

  public void setPathOrOperation(String pathOrOperation) {
    this.pathOrOperation = pathOrOperation;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SnapshotEndpointRef snapshotEndpointRef = (SnapshotEndpointRef) o;
    return Objects.equals(this.protocol, snapshotEndpointRef.protocol) &&
        Objects.equals(this.httpMethod, snapshotEndpointRef.httpMethod) &&
        Objects.equals(this.pathOrOperation, snapshotEndpointRef.pathOrOperation);
  }

  @Override
  public int hashCode() {
    return Objects.hash(protocol, httpMethod, pathOrOperation);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SnapshotEndpointRef {\n");
    sb.append("    protocol: ").append(toIndentedString(protocol)).append("\n");
    sb.append("    httpMethod: ").append(toIndentedString(httpMethod)).append("\n");
    sb.append("    pathOrOperation: ").append(toIndentedString(pathOrOperation)).append("\n");
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

