package com.codevision.codevisionbackend.api.model;

import java.net.URI;
import java.util.Objects;
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
 * SoapPortSummary
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-07T01:12:28.278930-05:00[America/New_York]", comments = "Generator version: 7.5.0")
public class SoapPortSummary {

  private String portName;

  @Valid
  private List<String> operations = new ArrayList<>();

  public SoapPortSummary() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SoapPortSummary(String portName) {
    this.portName = portName;
  }

  public SoapPortSummary portName(String portName) {
    this.portName = portName;
    return this;
  }

  /**
   * Name of the SOAP port.
   * @return portName
  */
  @NotNull 
  @Schema(name = "portName", description = "Name of the SOAP port.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("portName")
  public String getPortName() {
    return portName;
  }

  public void setPortName(String portName) {
    this.portName = portName;
  }

  public SoapPortSummary operations(List<String> operations) {
    this.operations = operations;
    return this;
  }

  public SoapPortSummary addOperationsItem(String operationsItem) {
    if (this.operations == null) {
      this.operations = new ArrayList<>();
    }
    this.operations.add(operationsItem);
    return this;
  }

  /**
   * Operation names exposed by the port.
   * @return operations
  */
  
  @Schema(name = "operations", description = "Operation names exposed by the port.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("operations")
  public List<String> getOperations() {
    return operations;
  }

  public void setOperations(List<String> operations) {
    this.operations = operations;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SoapPortSummary soapPortSummary = (SoapPortSummary) o;
    return Objects.equals(this.portName, soapPortSummary.portName) &&
        Objects.equals(this.operations, soapPortSummary.operations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(portName, operations);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SoapPortSummary {\n");
    sb.append("    portName: ").append(toIndentedString(portName)).append("\n");
    sb.append("    operations: ").append(toIndentedString(operations)).append("\n");
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

