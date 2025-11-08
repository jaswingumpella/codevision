package com.codevision.codevisionbackend.api.model;

import java.net.URI;
import java.util.Objects;
import com.codevision.codevisionbackend.api.model.SoapPortSummary;
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
 * SoapServiceSummary
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-08T10:09:44.785384-05:00[America/New_York]", comments = "Generator version: 7.5.0")
public class SoapServiceSummary {

  private String fileName;

  private String serviceName;

  @Valid
  private List<@Valid SoapPortSummary> ports = new ArrayList<>();

  public SoapServiceSummary() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SoapServiceSummary(String fileName, String serviceName) {
    this.fileName = fileName;
    this.serviceName = serviceName;
  }

  public SoapServiceSummary fileName(String fileName) {
    this.fileName = fileName;
    return this;
  }

  /**
   * WSDL document file name from which this summary was derived.
   * @return fileName
  */
  @NotNull 
  @Schema(name = "fileName", description = "WSDL document file name from which this summary was derived.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("fileName")
  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public SoapServiceSummary serviceName(String serviceName) {
    this.serviceName = serviceName;
    return this;
  }

  /**
   * Name of the SOAP service.
   * @return serviceName
  */
  @NotNull 
  @Schema(name = "serviceName", description = "Name of the SOAP service.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("serviceName")
  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public SoapServiceSummary ports(List<@Valid SoapPortSummary> ports) {
    this.ports = ports;
    return this;
  }

  public SoapServiceSummary addPortsItem(SoapPortSummary portsItem) {
    if (this.ports == null) {
      this.ports = new ArrayList<>();
    }
    this.ports.add(portsItem);
    return this;
  }

  /**
   * Ports exposed by the SOAP service.
   * @return ports
  */
  @Valid 
  @Schema(name = "ports", description = "Ports exposed by the SOAP service.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("ports")
  public List<@Valid SoapPortSummary> getPorts() {
    return ports;
  }

  public void setPorts(List<@Valid SoapPortSummary> ports) {
    this.ports = ports;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SoapServiceSummary soapServiceSummary = (SoapServiceSummary) o;
    return Objects.equals(this.fileName, soapServiceSummary.fileName) &&
        Objects.equals(this.serviceName, soapServiceSummary.serviceName) &&
        Objects.equals(this.ports, soapServiceSummary.ports);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fileName, serviceName, ports);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SoapServiceSummary {\n");
    sb.append("    fileName: ").append(toIndentedString(fileName)).append("\n");
    sb.append("    serviceName: ").append(toIndentedString(serviceName)).append("\n");
    sb.append("    ports: ").append(toIndentedString(ports)).append("\n");
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

