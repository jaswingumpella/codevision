package com.codevision.codevisionbackend.api.model;

import java.net.URI;
import java.util.Objects;
import com.codevision.codevisionbackend.api.model.OpenApiSpec;
import com.codevision.codevisionbackend.api.model.SoapServiceSummary;
import com.codevision.codevisionbackend.api.model.SpecDocument;
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
 * MetadataDump
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-09T00:07:15.283463-05:00[America/New_York]", comments = "Generator version: 7.5.0")
public class MetadataDump {

  @Valid
  private List<@Valid OpenApiSpec> openApiSpecs = new ArrayList<>();

  @Valid
  private List<@Valid SpecDocument> wsdlDocuments = new ArrayList<>();

  @Valid
  private List<@Valid SpecDocument> xsdDocuments = new ArrayList<>();

  @Valid
  private List<@Valid SoapServiceSummary> soapServices = new ArrayList<>();

  public MetadataDump openApiSpecs(List<@Valid OpenApiSpec> openApiSpecs) {
    this.openApiSpecs = openApiSpecs;
    return this;
  }

  public MetadataDump addOpenApiSpecsItem(OpenApiSpec openApiSpecsItem) {
    if (this.openApiSpecs == null) {
      this.openApiSpecs = new ArrayList<>();
    }
    this.openApiSpecs.add(openApiSpecsItem);
    return this;
  }

  /**
   * Collection of OpenAPI specification files detected within the repository.
   * @return openApiSpecs
  */
  @Valid 
  @Schema(name = "openApiSpecs", description = "Collection of OpenAPI specification files detected within the repository.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("openApiSpecs")
  public List<@Valid OpenApiSpec> getOpenApiSpecs() {
    return openApiSpecs;
  }

  public void setOpenApiSpecs(List<@Valid OpenApiSpec> openApiSpecs) {
    this.openApiSpecs = openApiSpecs;
  }

  public MetadataDump wsdlDocuments(List<@Valid SpecDocument> wsdlDocuments) {
    this.wsdlDocuments = wsdlDocuments;
    return this;
  }

  public MetadataDump addWsdlDocumentsItem(SpecDocument wsdlDocumentsItem) {
    if (this.wsdlDocuments == null) {
      this.wsdlDocuments = new ArrayList<>();
    }
    this.wsdlDocuments.add(wsdlDocumentsItem);
    return this;
  }

  /**
   * Collection of WSDL documents discovered in the repository.
   * @return wsdlDocuments
  */
  @Valid 
  @Schema(name = "wsdlDocuments", description = "Collection of WSDL documents discovered in the repository.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("wsdlDocuments")
  public List<@Valid SpecDocument> getWsdlDocuments() {
    return wsdlDocuments;
  }

  public void setWsdlDocuments(List<@Valid SpecDocument> wsdlDocuments) {
    this.wsdlDocuments = wsdlDocuments;
  }

  public MetadataDump xsdDocuments(List<@Valid SpecDocument> xsdDocuments) {
    this.xsdDocuments = xsdDocuments;
    return this;
  }

  public MetadataDump addXsdDocumentsItem(SpecDocument xsdDocumentsItem) {
    if (this.xsdDocuments == null) {
      this.xsdDocuments = new ArrayList<>();
    }
    this.xsdDocuments.add(xsdDocumentsItem);
    return this;
  }

  /**
   * Collection of XSD schema artifacts related to SOAP services.
   * @return xsdDocuments
  */
  @Valid 
  @Schema(name = "xsdDocuments", description = "Collection of XSD schema artifacts related to SOAP services.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("xsdDocuments")
  public List<@Valid SpecDocument> getXsdDocuments() {
    return xsdDocuments;
  }

  public void setXsdDocuments(List<@Valid SpecDocument> xsdDocuments) {
    this.xsdDocuments = xsdDocuments;
  }

  public MetadataDump soapServices(List<@Valid SoapServiceSummary> soapServices) {
    this.soapServices = soapServices;
    return this;
  }

  public MetadataDump addSoapServicesItem(SoapServiceSummary soapServicesItem) {
    if (this.soapServices == null) {
      this.soapServices = new ArrayList<>();
    }
    this.soapServices.add(soapServicesItem);
    return this;
  }

  /**
   * Summary of SOAP services, ports, and operations extracted from WSDL files.
   * @return soapServices
  */
  @Valid 
  @Schema(name = "soapServices", description = "Summary of SOAP services, ports, and operations extracted from WSDL files.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("soapServices")
  public List<@Valid SoapServiceSummary> getSoapServices() {
    return soapServices;
  }

  public void setSoapServices(List<@Valid SoapServiceSummary> soapServices) {
    this.soapServices = soapServices;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MetadataDump metadataDump = (MetadataDump) o;
    return Objects.equals(this.openApiSpecs, metadataDump.openApiSpecs) &&
        Objects.equals(this.wsdlDocuments, metadataDump.wsdlDocuments) &&
        Objects.equals(this.xsdDocuments, metadataDump.xsdDocuments) &&
        Objects.equals(this.soapServices, metadataDump.soapServices);
  }

  @Override
  public int hashCode() {
    return Objects.hash(openApiSpecs, wsdlDocuments, xsdDocuments, soapServices);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MetadataDump {\n");
    sb.append("    openApiSpecs: ").append(toIndentedString(openApiSpecs)).append("\n");
    sb.append("    wsdlDocuments: ").append(toIndentedString(wsdlDocuments)).append("\n");
    sb.append("    xsdDocuments: ").append(toIndentedString(xsdDocuments)).append("\n");
    sb.append("    soapServices: ").append(toIndentedString(soapServices)).append("\n");
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

