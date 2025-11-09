package com.codevision.codevisionbackend.api.model;

import java.net.URI;
import java.util.Objects;
import com.codevision.codevisionbackend.api.model.GherkinScenario;
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
 * GherkinFeature
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-08T10:09:44.785384-05:00[America/New_York]", comments = "Generator version: 7.5.0")
public class GherkinFeature {

  private String featureFile;

  private String featureTitle;

  @Valid
  private List<@Valid GherkinScenario> scenarios = new ArrayList<>();

  public GherkinFeature featureFile(String featureFile) {
    this.featureFile = featureFile;
    return this;
  }

  /**
   * Relative path to the feature file.
   * @return featureFile
  */
  
  @Schema(name = "featureFile", description = "Relative path to the feature file.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("featureFile")
  public String getFeatureFile() {
    return featureFile;
  }

  public void setFeatureFile(String featureFile) {
    this.featureFile = featureFile;
  }

  public GherkinFeature featureTitle(String featureTitle) {
    this.featureTitle = featureTitle;
    return this;
  }

  /**
   * Title declared in the `Feature:` header.
   * @return featureTitle
  */
  
  @Schema(name = "featureTitle", description = "Title declared in the `Feature:` header.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("featureTitle")
  public String getFeatureTitle() {
    return featureTitle;
  }

  public void setFeatureTitle(String featureTitle) {
    this.featureTitle = featureTitle;
  }

  public GherkinFeature scenarios(List<@Valid GherkinScenario> scenarios) {
    this.scenarios = scenarios;
    return this;
  }

  public GherkinFeature addScenariosItem(GherkinScenario scenariosItem) {
    if (this.scenarios == null) {
      this.scenarios = new ArrayList<>();
    }
    this.scenarios.add(scenariosItem);
    return this;
  }

  /**
   * Scenarios declared inside the feature file.
   * @return scenarios
  */
  @Valid 
  @Schema(name = "scenarios", description = "Scenarios declared inside the feature file.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("scenarios")
  public List<@Valid GherkinScenario> getScenarios() {
    return scenarios;
  }

  public void setScenarios(List<@Valid GherkinScenario> scenarios) {
    this.scenarios = scenarios;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GherkinFeature gherkinFeature = (GherkinFeature) o;
    return Objects.equals(this.featureFile, gherkinFeature.featureFile) &&
        Objects.equals(this.featureTitle, gherkinFeature.featureTitle) &&
        Objects.equals(this.scenarios, gherkinFeature.scenarios);
  }

  @Override
  public int hashCode() {
    return Objects.hash(featureFile, featureTitle, scenarios);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GherkinFeature {\n");
    sb.append("    featureFile: ").append(toIndentedString(featureFile)).append("\n");
    sb.append("    featureTitle: ").append(toIndentedString(featureTitle)).append("\n");
    sb.append("    scenarios: ").append(toIndentedString(scenarios)).append("\n");
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

