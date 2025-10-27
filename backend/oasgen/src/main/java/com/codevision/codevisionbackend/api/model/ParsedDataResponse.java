package com.codevision.codevisionbackend.api.model;

import java.net.URI;
import java.util.Objects;
import com.codevision.codevisionbackend.api.model.ApiEndpoint;
import com.codevision.codevisionbackend.api.model.AssetInventory;
import com.codevision.codevisionbackend.api.model.BuildInfo;
import com.codevision.codevisionbackend.api.model.ClassMetadataSummary;
import com.codevision.codevisionbackend.api.model.MetadataDump;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * ParsedDataResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-10-27T11:05:22.938178-04:00[America/New_York]", comments = "Generator version: 7.5.0")
public class ParsedDataResponse {

  private Long projectId;

  private String projectName;

  private URI repoUrl;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime analyzedAt;

  private BuildInfo buildInfo;

  @Valid
  private List<@Valid ClassMetadataSummary> classes = new ArrayList<>();

  private MetadataDump metadataDump;

  @Valid
  private List<@Valid ApiEndpoint> apiEndpoints = new ArrayList<>();

  private AssetInventory assets;

  public ParsedDataResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ParsedDataResponse(Long projectId, String projectName, URI repoUrl, OffsetDateTime analyzedAt) {
    this.projectId = projectId;
    this.projectName = projectName;
    this.repoUrl = repoUrl;
    this.analyzedAt = analyzedAt;
  }

  public ParsedDataResponse projectId(Long projectId) {
    this.projectId = projectId;
    return this;
  }

  /**
   * Identifier of the associated project.
   * @return projectId
  */
  @NotNull 
  @Schema(name = "projectId", description = "Identifier of the associated project.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("projectId")
  public Long getProjectId() {
    return projectId;
  }

  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }

  public ParsedDataResponse projectName(String projectName) {
    this.projectName = projectName;
    return this;
  }

  /**
   * Name of the analyzed project.
   * @return projectName
  */
  @NotNull 
  @Schema(name = "projectName", description = "Name of the analyzed project.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("projectName")
  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public ParsedDataResponse repoUrl(URI repoUrl) {
    this.repoUrl = repoUrl;
    return this;
  }

  /**
   * Git repository URL that was analyzed.
   * @return repoUrl
  */
  @NotNull @Valid 
  @Schema(name = "repoUrl", description = "Git repository URL that was analyzed.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("repoUrl")
  public URI getRepoUrl() {
    return repoUrl;
  }

  public void setRepoUrl(URI repoUrl) {
    this.repoUrl = repoUrl;
  }

  public ParsedDataResponse analyzedAt(OffsetDateTime analyzedAt) {
    this.analyzedAt = analyzedAt;
    return this;
  }

  /**
   * Timestamp when the repository was last analyzed.
   * @return analyzedAt
  */
  @NotNull @Valid 
  @Schema(name = "analyzedAt", description = "Timestamp when the repository was last analyzed.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("analyzedAt")
  public OffsetDateTime getAnalyzedAt() {
    return analyzedAt;
  }

  public void setAnalyzedAt(OffsetDateTime analyzedAt) {
    this.analyzedAt = analyzedAt;
  }

  public ParsedDataResponse buildInfo(BuildInfo buildInfo) {
    this.buildInfo = buildInfo;
    return this;
  }

  /**
   * Get buildInfo
   * @return buildInfo
  */
  @Valid 
  @Schema(name = "buildInfo", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("buildInfo")
  public BuildInfo getBuildInfo() {
    return buildInfo;
  }

  public void setBuildInfo(BuildInfo buildInfo) {
    this.buildInfo = buildInfo;
  }

  public ParsedDataResponse classes(List<@Valid ClassMetadataSummary> classes) {
    this.classes = classes;
    return this;
  }

  public ParsedDataResponse addClassesItem(ClassMetadataSummary classesItem) {
    if (this.classes == null) {
      this.classes = new ArrayList<>();
    }
    this.classes.add(classesItem);
    return this;
  }

  /**
   * Summaries for classes detected during analysis.
   * @return classes
  */
  @Valid 
  @Schema(name = "classes", description = "Summaries for classes detected during analysis.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("classes")
  public List<@Valid ClassMetadataSummary> getClasses() {
    return classes;
  }

  public void setClasses(List<@Valid ClassMetadataSummary> classes) {
    this.classes = classes;
  }

  public ParsedDataResponse metadataDump(MetadataDump metadataDump) {
    this.metadataDump = metadataDump;
    return this;
  }

  /**
   * Get metadataDump
   * @return metadataDump
  */
  @Valid 
  @Schema(name = "metadataDump", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("metadataDump")
  public MetadataDump getMetadataDump() {
    return metadataDump;
  }

  public void setMetadataDump(MetadataDump metadataDump) {
    this.metadataDump = metadataDump;
  }

  public ParsedDataResponse apiEndpoints(List<@Valid ApiEndpoint> apiEndpoints) {
    this.apiEndpoints = apiEndpoints;
    return this;
  }

  public ParsedDataResponse addApiEndpointsItem(ApiEndpoint apiEndpointsItem) {
    if (this.apiEndpoints == null) {
      this.apiEndpoints = new ArrayList<>();
    }
    this.apiEndpoints.add(apiEndpointsItem);
    return this;
  }

  /**
   * Catalog of API endpoints discovered in the project.
   * @return apiEndpoints
  */
  @Valid 
  @Schema(name = "apiEndpoints", description = "Catalog of API endpoints discovered in the project.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("apiEndpoints")
  public List<@Valid ApiEndpoint> getApiEndpoints() {
    return apiEndpoints;
  }

  public void setApiEndpoints(List<@Valid ApiEndpoint> apiEndpoints) {
    this.apiEndpoints = apiEndpoints;
  }

  public ParsedDataResponse assets(AssetInventory assets) {
    this.assets = assets;
    return this;
  }

  /**
   * Get assets
   * @return assets
  */
  @Valid 
  @Schema(name = "assets", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("assets")
  public AssetInventory getAssets() {
    return assets;
  }

  public void setAssets(AssetInventory assets) {
    this.assets = assets;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ParsedDataResponse parsedDataResponse = (ParsedDataResponse) o;
    return Objects.equals(this.projectId, parsedDataResponse.projectId) &&
        Objects.equals(this.projectName, parsedDataResponse.projectName) &&
        Objects.equals(this.repoUrl, parsedDataResponse.repoUrl) &&
        Objects.equals(this.analyzedAt, parsedDataResponse.analyzedAt) &&
        Objects.equals(this.buildInfo, parsedDataResponse.buildInfo) &&
        Objects.equals(this.classes, parsedDataResponse.classes) &&
        Objects.equals(this.metadataDump, parsedDataResponse.metadataDump) &&
        Objects.equals(this.apiEndpoints, parsedDataResponse.apiEndpoints) &&
        Objects.equals(this.assets, parsedDataResponse.assets);
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectId, projectName, repoUrl, analyzedAt, buildInfo, classes, metadataDump, apiEndpoints, assets);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ParsedDataResponse {\n");
    sb.append("    projectId: ").append(toIndentedString(projectId)).append("\n");
    sb.append("    projectName: ").append(toIndentedString(projectName)).append("\n");
    sb.append("    repoUrl: ").append(toIndentedString(repoUrl)).append("\n");
    sb.append("    analyzedAt: ").append(toIndentedString(analyzedAt)).append("\n");
    sb.append("    buildInfo: ").append(toIndentedString(buildInfo)).append("\n");
    sb.append("    classes: ").append(toIndentedString(classes)).append("\n");
    sb.append("    metadataDump: ").append(toIndentedString(metadataDump)).append("\n");
    sb.append("    apiEndpoints: ").append(toIndentedString(apiEndpoints)).append("\n");
    sb.append("    assets: ").append(toIndentedString(assets)).append("\n");
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

