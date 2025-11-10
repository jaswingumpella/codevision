package com.codevision.codevisionbackend.api.model;

import java.net.URI;
import java.util.Objects;
import com.codevision.codevisionbackend.api.model.ApiEndpoint;
import com.codevision.codevisionbackend.api.model.AssetInventory;
import com.codevision.codevisionbackend.api.model.BuildInfo;
import com.codevision.codevisionbackend.api.model.ClassMetadataSummary;
import com.codevision.codevisionbackend.api.model.DbAnalysis;
import com.codevision.codevisionbackend.api.model.DiagramDescriptor;
import com.codevision.codevisionbackend.api.model.GherkinFeature;
import com.codevision.codevisionbackend.api.model.LoggerInsight;
import com.codevision.codevisionbackend.api.model.MetadataDump;
import com.codevision.codevisionbackend.api.model.PiiPciFinding;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-10T00:09:30.786805-05:00[America/New_York]", comments = "Generator version: 7.5.0")
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

  private DbAnalysis dbAnalysis;

  @Valid
  private List<@Valid ApiEndpoint> apiEndpoints = new ArrayList<>();

  private AssetInventory assets;

  @Valid
  private List<@Valid LoggerInsight> loggerInsights = new ArrayList<>();

  @Valid
  private List<@Valid PiiPciFinding> piiPciScan = new ArrayList<>();

  @Valid
  private List<@Valid GherkinFeature> gherkinFeatures = new ArrayList<>();

  @Valid
  private Map<String, List<String>> callFlows = new HashMap<>();

  @Valid
  private List<@Valid DiagramDescriptor> diagrams = new ArrayList<>();

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

  public ParsedDataResponse dbAnalysis(DbAnalysis dbAnalysis) {
    this.dbAnalysis = dbAnalysis;
    return this;
  }

  /**
   * Get dbAnalysis
   * @return dbAnalysis
  */
  @Valid 
  @Schema(name = "dbAnalysis", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("dbAnalysis")
  public DbAnalysis getDbAnalysis() {
    return dbAnalysis;
  }

  public void setDbAnalysis(DbAnalysis dbAnalysis) {
    this.dbAnalysis = dbAnalysis;
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

  public ParsedDataResponse loggerInsights(List<@Valid LoggerInsight> loggerInsights) {
    this.loggerInsights = loggerInsights;
    return this;
  }

  public ParsedDataResponse addLoggerInsightsItem(LoggerInsight loggerInsightsItem) {
    if (this.loggerInsights == null) {
      this.loggerInsights = new ArrayList<>();
    }
    this.loggerInsights.add(loggerInsightsItem);
    return this;
  }

  /**
   * Catalog of log statements detected during analysis.
   * @return loggerInsights
  */
  @Valid 
  @Schema(name = "loggerInsights", description = "Catalog of log statements detected during analysis.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("loggerInsights")
  public List<@Valid LoggerInsight> getLoggerInsights() {
    return loggerInsights;
  }

  public void setLoggerInsights(List<@Valid LoggerInsight> loggerInsights) {
    this.loggerInsights = loggerInsights;
  }

  public ParsedDataResponse piiPciScan(List<@Valid PiiPciFinding> piiPciScan) {
    this.piiPciScan = piiPciScan;
    return this;
  }

  public ParsedDataResponse addPiiPciScanItem(PiiPciFinding piiPciScanItem) {
    if (this.piiPciScan == null) {
      this.piiPciScan = new ArrayList<>();
    }
    this.piiPciScan.add(piiPciScanItem);
    return this;
  }

  /**
   * Potential PCI/PII findings detected during analysis.
   * @return piiPciScan
  */
  @Valid 
  @Schema(name = "piiPciScan", description = "Potential PCI/PII findings detected during analysis.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("piiPciScan")
  public List<@Valid PiiPciFinding> getPiiPciScan() {
    return piiPciScan;
  }

  public void setPiiPciScan(List<@Valid PiiPciFinding> piiPciScan) {
    this.piiPciScan = piiPciScan;
  }

  public ParsedDataResponse gherkinFeatures(List<@Valid GherkinFeature> gherkinFeatures) {
    this.gherkinFeatures = gherkinFeatures;
    return this;
  }

  public ParsedDataResponse addGherkinFeaturesItem(GherkinFeature gherkinFeaturesItem) {
    if (this.gherkinFeatures == null) {
      this.gherkinFeatures = new ArrayList<>();
    }
    this.gherkinFeatures.add(gherkinFeaturesItem);
    return this;
  }

  /**
   * BDD feature files and scenarios discovered during analysis.
   * @return gherkinFeatures
  */
  @Valid 
  @Schema(name = "gherkinFeatures", description = "BDD feature files and scenarios discovered during analysis.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("gherkinFeatures")
  public List<@Valid GherkinFeature> getGherkinFeatures() {
    return gherkinFeatures;
  }

  public void setGherkinFeatures(List<@Valid GherkinFeature> gherkinFeatures) {
    this.gherkinFeatures = gherkinFeatures;
  }

  public ParsedDataResponse callFlows(Map<String, List<String>> callFlows) {
    this.callFlows = callFlows;
    return this;
  }

  public ParsedDataResponse putCallFlowsItem(String key, List<String> callFlowsItem) {
    if (this.callFlows == null) {
      this.callFlows = new HashMap<>();
    }
    this.callFlows.put(key, callFlowsItem);
    return this;
  }

  /**
   * High-level call flow summaries derived from the call graph.
   * @return callFlows
  */
  @Valid 
  @Schema(name = "callFlows", description = "High-level call flow summaries derived from the call graph.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("callFlows")
  public Map<String, List<String>> getCallFlows() {
    return callFlows;
  }

  public void setCallFlows(Map<String, List<String>> callFlows) {
    this.callFlows = callFlows;
  }

  public ParsedDataResponse diagrams(List<@Valid DiagramDescriptor> diagrams) {
    this.diagrams = diagrams;
    return this;
  }

  public ParsedDataResponse addDiagramsItem(DiagramDescriptor diagramsItem) {
    if (this.diagrams == null) {
      this.diagrams = new ArrayList<>();
    }
    this.diagrams.add(diagramsItem);
    return this;
  }

  /**
   * Persisted diagram definitions (PlantUML, Mermaid, SVG metadata).
   * @return diagrams
  */
  @Valid 
  @Schema(name = "diagrams", description = "Persisted diagram definitions (PlantUML, Mermaid, SVG metadata).", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("diagrams")
  public List<@Valid DiagramDescriptor> getDiagrams() {
    return diagrams;
  }

  public void setDiagrams(List<@Valid DiagramDescriptor> diagrams) {
    this.diagrams = diagrams;
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
        Objects.equals(this.dbAnalysis, parsedDataResponse.dbAnalysis) &&
        Objects.equals(this.apiEndpoints, parsedDataResponse.apiEndpoints) &&
        Objects.equals(this.assets, parsedDataResponse.assets) &&
        Objects.equals(this.loggerInsights, parsedDataResponse.loggerInsights) &&
        Objects.equals(this.piiPciScan, parsedDataResponse.piiPciScan) &&
        Objects.equals(this.gherkinFeatures, parsedDataResponse.gherkinFeatures) &&
        Objects.equals(this.callFlows, parsedDataResponse.callFlows) &&
        Objects.equals(this.diagrams, parsedDataResponse.diagrams);
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectId, projectName, repoUrl, analyzedAt, buildInfo, classes, metadataDump, dbAnalysis, apiEndpoints, assets, loggerInsights, piiPciScan, gherkinFeatures, callFlows, diagrams);
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
    sb.append("    dbAnalysis: ").append(toIndentedString(dbAnalysis)).append("\n");
    sb.append("    apiEndpoints: ").append(toIndentedString(apiEndpoints)).append("\n");
    sb.append("    assets: ").append(toIndentedString(assets)).append("\n");
    sb.append("    loggerInsights: ").append(toIndentedString(loggerInsights)).append("\n");
    sb.append("    piiPciScan: ").append(toIndentedString(piiPciScan)).append("\n");
    sb.append("    gherkinFeatures: ").append(toIndentedString(gherkinFeatures)).append("\n");
    sb.append("    callFlows: ").append(toIndentedString(callFlows)).append("\n");
    sb.append("    diagrams: ").append(toIndentedString(diagrams)).append("\n");
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

