package com.codevision.codevisionbackend.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.HashMap;
import java.util.Map;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * DiagramDescriptor
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-06T22:23:43.718706-05:00[America/New_York]", comments = "Generator version: 7.5.0")
public class DiagramDescriptor {

  private Long diagramId;

  private String diagramType;

  private String title;

  private String plantumlSource;

  private String mermaidSource;

  private Boolean svgAvailable;

  private String svgDownloadUrl;

  @Valid
  private Map<String, Object> metadata = new HashMap<>();

  public DiagramDescriptor() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public DiagramDescriptor(Long diagramId, String diagramType, String title) {
    this.diagramId = diagramId;
    this.diagramType = diagramType;
    this.title = title;
  }

  public DiagramDescriptor diagramId(Long diagramId) {
    this.diagramId = diagramId;
    return this;
  }

  /**
   * Identifier of the stored diagram.
   * @return diagramId
  */
  @NotNull 
  @Schema(name = "diagramId", description = "Identifier of the stored diagram.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("diagramId")
  public Long getDiagramId() {
    return diagramId;
  }

  public void setDiagramId(Long diagramId) {
    this.diagramId = diagramId;
  }

  public DiagramDescriptor diagramType(String diagramType) {
    this.diagramType = diagramType;
    return this;
  }

  /**
   * Diagram classification (CLASS, COMPONENT, USE_CASE, ERD, DB_SCHEMA, SEQUENCE).
   * @return diagramType
  */
  @NotNull 
  @Schema(name = "diagramType", description = "Diagram classification (CLASS, COMPONENT, USE_CASE, ERD, DB_SCHEMA, SEQUENCE).", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("diagramType")
  public String getDiagramType() {
    return diagramType;
  }

  public void setDiagramType(String diagramType) {
    this.diagramType = diagramType;
  }

  public DiagramDescriptor title(String title) {
    this.title = title;
    return this;
  }

  /**
   * Short label shown in UI tabs.
   * @return title
  */
  @NotNull 
  @Schema(name = "title", description = "Short label shown in UI tabs.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public DiagramDescriptor plantumlSource(String plantumlSource) {
    this.plantumlSource = plantumlSource;
    return this;
  }

  /**
   * PlantUML source text for the diagram.
   * @return plantumlSource
  */
  
  @Schema(name = "plantumlSource", description = "PlantUML source text for the diagram.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("plantumlSource")
  public String getPlantumlSource() {
    return plantumlSource;
  }

  public void setPlantumlSource(String plantumlSource) {
    this.plantumlSource = plantumlSource;
  }

  public DiagramDescriptor mermaidSource(String mermaidSource) {
    this.mermaidSource = mermaidSource;
    return this;
  }

  /**
   * Mermaid source text for the diagram.
   * @return mermaidSource
  */
  
  @Schema(name = "mermaidSource", description = "Mermaid source text for the diagram.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("mermaidSource")
  public String getMermaidSource() {
    return mermaidSource;
  }

  public void setMermaidSource(String mermaidSource) {
    this.mermaidSource = mermaidSource;
  }

  public DiagramDescriptor svgAvailable(Boolean svgAvailable) {
    this.svgAvailable = svgAvailable;
    return this;
  }

  /**
   * Indicates whether an SVG rendering is stored.
   * @return svgAvailable
  */
  
  @Schema(name = "svgAvailable", description = "Indicates whether an SVG rendering is stored.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("svgAvailable")
  public Boolean getSvgAvailable() {
    return svgAvailable;
  }

  public void setSvgAvailable(Boolean svgAvailable) {
    this.svgAvailable = svgAvailable;
  }

  public DiagramDescriptor svgDownloadUrl(String svgDownloadUrl) {
    this.svgDownloadUrl = svgDownloadUrl;
    return this;
  }

  /**
   * Relative URL to download the rendered SVG when available.
   * @return svgDownloadUrl
  */
  
  @Schema(name = "svgDownloadUrl", description = "Relative URL to download the rendered SVG when available.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("svgDownloadUrl")
  public String getSvgDownloadUrl() {
    return svgDownloadUrl;
  }

  public void setSvgDownloadUrl(String svgDownloadUrl) {
    this.svgDownloadUrl = svgDownloadUrl;
  }

  public DiagramDescriptor metadata(Map<String, Object> metadata) {
    this.metadata = metadata;
    return this;
  }

  public DiagramDescriptor putMetadataItem(String key, Object metadataItem) {
    if (this.metadata == null) {
      this.metadata = new HashMap<>();
    }
    this.metadata.put(key, metadataItem);
    return this;
  }

  /**
   * Diagram-specific metadata (e.g., includeExternal flag for sequences).
   * @return metadata
  */
  
  @Schema(name = "metadata", description = "Diagram-specific metadata (e.g., includeExternal flag for sequences).", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("metadata")
  public Map<String, Object> getMetadata() {
    return metadata;
  }

  public void setMetadata(Map<String, Object> metadata) {
    this.metadata = metadata;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DiagramDescriptor diagramDescriptor = (DiagramDescriptor) o;
    return Objects.equals(this.diagramId, diagramDescriptor.diagramId) &&
        Objects.equals(this.diagramType, diagramDescriptor.diagramType) &&
        Objects.equals(this.title, diagramDescriptor.title) &&
        Objects.equals(this.plantumlSource, diagramDescriptor.plantumlSource) &&
        Objects.equals(this.mermaidSource, diagramDescriptor.mermaidSource) &&
        Objects.equals(this.svgAvailable, diagramDescriptor.svgAvailable) &&
        Objects.equals(this.svgDownloadUrl, diagramDescriptor.svgDownloadUrl) &&
        Objects.equals(this.metadata, diagramDescriptor.metadata);
  }

  @Override
  public int hashCode() {
    return Objects.hash(diagramId, diagramType, title, plantumlSource, mermaidSource, svgAvailable, svgDownloadUrl, metadata);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DiagramDescriptor {\n");
    sb.append("    diagramId: ").append(toIndentedString(diagramId)).append("\n");
    sb.append("    diagramType: ").append(toIndentedString(diagramType)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    plantumlSource: ").append(toIndentedString(plantumlSource)).append("\n");
    sb.append("    mermaidSource: ").append(toIndentedString(mermaidSource)).append("\n");
    sb.append("    svgAvailable: ").append(toIndentedString(svgAvailable)).append("\n");
    sb.append("    svgDownloadUrl: ").append(toIndentedString(svgDownloadUrl)).append("\n");
    sb.append("    metadata: ").append(toIndentedString(metadata)).append("\n");
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

