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
 * GherkinScenario
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-10T00:23:13.435459-05:00[America/New_York]", comments = "Generator version: 7.5.0")
public class GherkinScenario {

  private String name;

  private String scenarioType;

  @Valid
  private List<String> steps = new ArrayList<>();

  public GherkinScenario name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Scenario or background name.
   * @return name
  */
  
  @Schema(name = "name", description = "Scenario or background name.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public GherkinScenario scenarioType(String scenarioType) {
    this.scenarioType = scenarioType;
    return this;
  }

  /**
   * Scenario classification (SCENARIO, BACKGROUND, SCENARIO_OUTLINE, etc.).
   * @return scenarioType
  */
  
  @Schema(name = "scenarioType", description = "Scenario classification (SCENARIO, BACKGROUND, SCENARIO_OUTLINE, etc.).", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("scenarioType")
  public String getScenarioType() {
    return scenarioType;
  }

  public void setScenarioType(String scenarioType) {
    this.scenarioType = scenarioType;
  }

  public GherkinScenario steps(List<String> steps) {
    this.steps = steps;
    return this;
  }

  public GherkinScenario addStepsItem(String stepsItem) {
    if (this.steps == null) {
      this.steps = new ArrayList<>();
    }
    this.steps.add(stepsItem);
    return this;
  }

  /**
   * Ordered list of steps captured from the scenario body.
   * @return steps
  */
  
  @Schema(name = "steps", description = "Ordered list of steps captured from the scenario body.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("steps")
  public List<String> getSteps() {
    return steps;
  }

  public void setSteps(List<String> steps) {
    this.steps = steps;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GherkinScenario gherkinScenario = (GherkinScenario) o;
    return Objects.equals(this.name, gherkinScenario.name) &&
        Objects.equals(this.scenarioType, gherkinScenario.scenarioType) &&
        Objects.equals(this.steps, gherkinScenario.steps);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, scenarioType, steps);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GherkinScenario {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    scenarioType: ").append(toIndentedString(scenarioType)).append("\n");
    sb.append("    steps: ").append(toIndentedString(steps)).append("\n");
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

