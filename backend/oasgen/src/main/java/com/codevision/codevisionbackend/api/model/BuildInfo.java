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
 * BuildInfo
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-09T19:10:45.782948-05:00[America/New_York]", comments = "Generator version: 7.5.0")
public class BuildInfo {

  private String groupId;

  private String artifactId;

  private String version;

  private String javaVersion;

  public BuildInfo groupId(String groupId) {
    this.groupId = groupId;
    return this;
  }

  /**
   * Maven group identifier resolved from the repository build metadata.
   * @return groupId
  */
  
  @Schema(name = "groupId", description = "Maven group identifier resolved from the repository build metadata.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("groupId")
  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  public BuildInfo artifactId(String artifactId) {
    this.artifactId = artifactId;
    return this;
  }

  /**
   * Maven artifact identifier resolved from the repository build metadata.
   * @return artifactId
  */
  
  @Schema(name = "artifactId", description = "Maven artifact identifier resolved from the repository build metadata.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("artifactId")
  public String getArtifactId() {
    return artifactId;
  }

  public void setArtifactId(String artifactId) {
    this.artifactId = artifactId;
  }

  public BuildInfo version(String version) {
    this.version = version;
    return this;
  }

  /**
   * Maven version resolved from the repository build metadata.
   * @return version
  */
  
  @Schema(name = "version", description = "Maven version resolved from the repository build metadata.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("version")
  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public BuildInfo javaVersion(String javaVersion) {
    this.javaVersion = javaVersion;
    return this;
  }

  /**
   * Java version targeted by the project build configuration.
   * @return javaVersion
  */
  
  @Schema(name = "javaVersion", description = "Java version targeted by the project build configuration.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("javaVersion")
  public String getJavaVersion() {
    return javaVersion;
  }

  public void setJavaVersion(String javaVersion) {
    this.javaVersion = javaVersion;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BuildInfo buildInfo = (BuildInfo) o;
    return Objects.equals(this.groupId, buildInfo.groupId) &&
        Objects.equals(this.artifactId, buildInfo.artifactId) &&
        Objects.equals(this.version, buildInfo.version) &&
        Objects.equals(this.javaVersion, buildInfo.javaVersion);
  }

  @Override
  public int hashCode() {
    return Objects.hash(groupId, artifactId, version, javaVersion);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BuildInfo {\n");
    sb.append("    groupId: ").append(toIndentedString(groupId)).append("\n");
    sb.append("    artifactId: ").append(toIndentedString(artifactId)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("    javaVersion: ").append(toIndentedString(javaVersion)).append("\n");
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

