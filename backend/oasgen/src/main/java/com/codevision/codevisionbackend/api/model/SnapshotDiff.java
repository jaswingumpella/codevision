package com.codevision.codevisionbackend.api.model;

import java.net.URI;
import java.util.Objects;
import com.codevision.codevisionbackend.api.model.SnapshotClassRef;
import com.codevision.codevisionbackend.api.model.SnapshotDbEntityRef;
import com.codevision.codevisionbackend.api.model.SnapshotEndpointRef;
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
 * SnapshotDiff
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-02-01T02:54:38.384001-05:00[America/New_York]", comments = "Generator version: 7.5.0")
public class SnapshotDiff {

  private Long baseSnapshotId;

  private Long compareSnapshotId;

  private String baseCommitHash;

  private String compareCommitHash;

  @Valid
  private List<@Valid SnapshotClassRef> addedClasses = new ArrayList<>();

  @Valid
  private List<@Valid SnapshotClassRef> removedClasses = new ArrayList<>();

  @Valid
  private List<@Valid SnapshotEndpointRef> addedEndpoints = new ArrayList<>();

  @Valid
  private List<@Valid SnapshotEndpointRef> removedEndpoints = new ArrayList<>();

  @Valid
  private List<@Valid SnapshotDbEntityRef> addedEntities = new ArrayList<>();

  @Valid
  private List<@Valid SnapshotDbEntityRef> removedEntities = new ArrayList<>();

  public SnapshotDiff baseSnapshotId(Long baseSnapshotId) {
    this.baseSnapshotId = baseSnapshotId;
    return this;
  }

  /**
   * Get baseSnapshotId
   * @return baseSnapshotId
  */
  
  @Schema(name = "baseSnapshotId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("baseSnapshotId")
  public Long getBaseSnapshotId() {
    return baseSnapshotId;
  }

  public void setBaseSnapshotId(Long baseSnapshotId) {
    this.baseSnapshotId = baseSnapshotId;
  }

  public SnapshotDiff compareSnapshotId(Long compareSnapshotId) {
    this.compareSnapshotId = compareSnapshotId;
    return this;
  }

  /**
   * Get compareSnapshotId
   * @return compareSnapshotId
  */
  
  @Schema(name = "compareSnapshotId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("compareSnapshotId")
  public Long getCompareSnapshotId() {
    return compareSnapshotId;
  }

  public void setCompareSnapshotId(Long compareSnapshotId) {
    this.compareSnapshotId = compareSnapshotId;
  }

  public SnapshotDiff baseCommitHash(String baseCommitHash) {
    this.baseCommitHash = baseCommitHash;
    return this;
  }

  /**
   * Get baseCommitHash
   * @return baseCommitHash
  */
  
  @Schema(name = "baseCommitHash", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("baseCommitHash")
  public String getBaseCommitHash() {
    return baseCommitHash;
  }

  public void setBaseCommitHash(String baseCommitHash) {
    this.baseCommitHash = baseCommitHash;
  }

  public SnapshotDiff compareCommitHash(String compareCommitHash) {
    this.compareCommitHash = compareCommitHash;
    return this;
  }

  /**
   * Get compareCommitHash
   * @return compareCommitHash
  */
  
  @Schema(name = "compareCommitHash", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("compareCommitHash")
  public String getCompareCommitHash() {
    return compareCommitHash;
  }

  public void setCompareCommitHash(String compareCommitHash) {
    this.compareCommitHash = compareCommitHash;
  }

  public SnapshotDiff addedClasses(List<@Valid SnapshotClassRef> addedClasses) {
    this.addedClasses = addedClasses;
    return this;
  }

  public SnapshotDiff addAddedClassesItem(SnapshotClassRef addedClassesItem) {
    if (this.addedClasses == null) {
      this.addedClasses = new ArrayList<>();
    }
    this.addedClasses.add(addedClassesItem);
    return this;
  }

  /**
   * Get addedClasses
   * @return addedClasses
  */
  @Valid 
  @Schema(name = "addedClasses", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("addedClasses")
  public List<@Valid SnapshotClassRef> getAddedClasses() {
    return addedClasses;
  }

  public void setAddedClasses(List<@Valid SnapshotClassRef> addedClasses) {
    this.addedClasses = addedClasses;
  }

  public SnapshotDiff removedClasses(List<@Valid SnapshotClassRef> removedClasses) {
    this.removedClasses = removedClasses;
    return this;
  }

  public SnapshotDiff addRemovedClassesItem(SnapshotClassRef removedClassesItem) {
    if (this.removedClasses == null) {
      this.removedClasses = new ArrayList<>();
    }
    this.removedClasses.add(removedClassesItem);
    return this;
  }

  /**
   * Get removedClasses
   * @return removedClasses
  */
  @Valid 
  @Schema(name = "removedClasses", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("removedClasses")
  public List<@Valid SnapshotClassRef> getRemovedClasses() {
    return removedClasses;
  }

  public void setRemovedClasses(List<@Valid SnapshotClassRef> removedClasses) {
    this.removedClasses = removedClasses;
  }

  public SnapshotDiff addedEndpoints(List<@Valid SnapshotEndpointRef> addedEndpoints) {
    this.addedEndpoints = addedEndpoints;
    return this;
  }

  public SnapshotDiff addAddedEndpointsItem(SnapshotEndpointRef addedEndpointsItem) {
    if (this.addedEndpoints == null) {
      this.addedEndpoints = new ArrayList<>();
    }
    this.addedEndpoints.add(addedEndpointsItem);
    return this;
  }

  /**
   * Get addedEndpoints
   * @return addedEndpoints
  */
  @Valid 
  @Schema(name = "addedEndpoints", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("addedEndpoints")
  public List<@Valid SnapshotEndpointRef> getAddedEndpoints() {
    return addedEndpoints;
  }

  public void setAddedEndpoints(List<@Valid SnapshotEndpointRef> addedEndpoints) {
    this.addedEndpoints = addedEndpoints;
  }

  public SnapshotDiff removedEndpoints(List<@Valid SnapshotEndpointRef> removedEndpoints) {
    this.removedEndpoints = removedEndpoints;
    return this;
  }

  public SnapshotDiff addRemovedEndpointsItem(SnapshotEndpointRef removedEndpointsItem) {
    if (this.removedEndpoints == null) {
      this.removedEndpoints = new ArrayList<>();
    }
    this.removedEndpoints.add(removedEndpointsItem);
    return this;
  }

  /**
   * Get removedEndpoints
   * @return removedEndpoints
  */
  @Valid 
  @Schema(name = "removedEndpoints", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("removedEndpoints")
  public List<@Valid SnapshotEndpointRef> getRemovedEndpoints() {
    return removedEndpoints;
  }

  public void setRemovedEndpoints(List<@Valid SnapshotEndpointRef> removedEndpoints) {
    this.removedEndpoints = removedEndpoints;
  }

  public SnapshotDiff addedEntities(List<@Valid SnapshotDbEntityRef> addedEntities) {
    this.addedEntities = addedEntities;
    return this;
  }

  public SnapshotDiff addAddedEntitiesItem(SnapshotDbEntityRef addedEntitiesItem) {
    if (this.addedEntities == null) {
      this.addedEntities = new ArrayList<>();
    }
    this.addedEntities.add(addedEntitiesItem);
    return this;
  }

  /**
   * Get addedEntities
   * @return addedEntities
  */
  @Valid 
  @Schema(name = "addedEntities", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("addedEntities")
  public List<@Valid SnapshotDbEntityRef> getAddedEntities() {
    return addedEntities;
  }

  public void setAddedEntities(List<@Valid SnapshotDbEntityRef> addedEntities) {
    this.addedEntities = addedEntities;
  }

  public SnapshotDiff removedEntities(List<@Valid SnapshotDbEntityRef> removedEntities) {
    this.removedEntities = removedEntities;
    return this;
  }

  public SnapshotDiff addRemovedEntitiesItem(SnapshotDbEntityRef removedEntitiesItem) {
    if (this.removedEntities == null) {
      this.removedEntities = new ArrayList<>();
    }
    this.removedEntities.add(removedEntitiesItem);
    return this;
  }

  /**
   * Get removedEntities
   * @return removedEntities
  */
  @Valid 
  @Schema(name = "removedEntities", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("removedEntities")
  public List<@Valid SnapshotDbEntityRef> getRemovedEntities() {
    return removedEntities;
  }

  public void setRemovedEntities(List<@Valid SnapshotDbEntityRef> removedEntities) {
    this.removedEntities = removedEntities;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SnapshotDiff snapshotDiff = (SnapshotDiff) o;
    return Objects.equals(this.baseSnapshotId, snapshotDiff.baseSnapshotId) &&
        Objects.equals(this.compareSnapshotId, snapshotDiff.compareSnapshotId) &&
        Objects.equals(this.baseCommitHash, snapshotDiff.baseCommitHash) &&
        Objects.equals(this.compareCommitHash, snapshotDiff.compareCommitHash) &&
        Objects.equals(this.addedClasses, snapshotDiff.addedClasses) &&
        Objects.equals(this.removedClasses, snapshotDiff.removedClasses) &&
        Objects.equals(this.addedEndpoints, snapshotDiff.addedEndpoints) &&
        Objects.equals(this.removedEndpoints, snapshotDiff.removedEndpoints) &&
        Objects.equals(this.addedEntities, snapshotDiff.addedEntities) &&
        Objects.equals(this.removedEntities, snapshotDiff.removedEntities);
  }

  @Override
  public int hashCode() {
    return Objects.hash(baseSnapshotId, compareSnapshotId, baseCommitHash, compareCommitHash, addedClasses, removedClasses, addedEndpoints, removedEndpoints, addedEntities, removedEntities);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SnapshotDiff {\n");
    sb.append("    baseSnapshotId: ").append(toIndentedString(baseSnapshotId)).append("\n");
    sb.append("    compareSnapshotId: ").append(toIndentedString(compareSnapshotId)).append("\n");
    sb.append("    baseCommitHash: ").append(toIndentedString(baseCommitHash)).append("\n");
    sb.append("    compareCommitHash: ").append(toIndentedString(compareCommitHash)).append("\n");
    sb.append("    addedClasses: ").append(toIndentedString(addedClasses)).append("\n");
    sb.append("    removedClasses: ").append(toIndentedString(removedClasses)).append("\n");
    sb.append("    addedEndpoints: ").append(toIndentedString(addedEndpoints)).append("\n");
    sb.append("    removedEndpoints: ").append(toIndentedString(removedEndpoints)).append("\n");
    sb.append("    addedEntities: ").append(toIndentedString(addedEntities)).append("\n");
    sb.append("    removedEntities: ").append(toIndentedString(removedEntities)).append("\n");
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

