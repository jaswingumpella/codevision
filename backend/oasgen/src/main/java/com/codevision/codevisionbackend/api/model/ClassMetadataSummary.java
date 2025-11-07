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
 * ClassMetadataSummary
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-06T23:14:24.607561-05:00[America/New_York]", comments = "Generator version: 7.5.0")
public class ClassMetadataSummary {

  private String fullyQualifiedName;

  private String packageName;

  private String className;

  private String stereotype;

  private Boolean userCode;

  private String sourceSet;

  private String relativePath;

  @Valid
  private List<String> annotations = new ArrayList<>();

  @Valid
  private List<String> interfacesImplemented = new ArrayList<>();

  public ClassMetadataSummary() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ClassMetadataSummary(String fullyQualifiedName, String className, Boolean userCode) {
    this.fullyQualifiedName = fullyQualifiedName;
    this.className = className;
    this.userCode = userCode;
  }

  public ClassMetadataSummary fullyQualifiedName(String fullyQualifiedName) {
    this.fullyQualifiedName = fullyQualifiedName;
    return this;
  }

  /**
   * Fully qualified class name.
   * @return fullyQualifiedName
  */
  @NotNull 
  @Schema(name = "fullyQualifiedName", description = "Fully qualified class name.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("fullyQualifiedName")
  public String getFullyQualifiedName() {
    return fullyQualifiedName;
  }

  public void setFullyQualifiedName(String fullyQualifiedName) {
    this.fullyQualifiedName = fullyQualifiedName;
  }

  public ClassMetadataSummary packageName(String packageName) {
    this.packageName = packageName;
    return this;
  }

  /**
   * Package that declares the class.
   * @return packageName
  */
  
  @Schema(name = "packageName", description = "Package that declares the class.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("packageName")
  public String getPackageName() {
    return packageName;
  }

  public void setPackageName(String packageName) {
    this.packageName = packageName;
  }

  public ClassMetadataSummary className(String className) {
    this.className = className;
    return this;
  }

  /**
   * Simple class name without package information.
   * @return className
  */
  @NotNull 
  @Schema(name = "className", description = "Simple class name without package information.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("className")
  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public ClassMetadataSummary stereotype(String stereotype) {
    this.stereotype = stereotype;
    return this;
  }

  /**
   * Functional stereotype inferred from the class metadata.
   * @return stereotype
  */
  
  @Schema(name = "stereotype", description = "Functional stereotype inferred from the class metadata.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("stereotype")
  public String getStereotype() {
    return stereotype;
  }

  public void setStereotype(String stereotype) {
    this.stereotype = stereotype;
  }

  public ClassMetadataSummary userCode(Boolean userCode) {
    this.userCode = userCode;
    return this;
  }

  /**
   * Indicates whether the class originates from user authored code.
   * @return userCode
  */
  @NotNull 
  @Schema(name = "userCode", description = "Indicates whether the class originates from user authored code.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("userCode")
  public Boolean getUserCode() {
    return userCode;
  }

  public void setUserCode(Boolean userCode) {
    this.userCode = userCode;
  }

  public ClassMetadataSummary sourceSet(String sourceSet) {
    this.sourceSet = sourceSet;
    return this;
  }

  /**
   * Source set where the class resides, such as MAIN or TEST.
   * @return sourceSet
  */
  
  @Schema(name = "sourceSet", description = "Source set where the class resides, such as MAIN or TEST.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("sourceSet")
  public String getSourceSet() {
    return sourceSet;
  }

  public void setSourceSet(String sourceSet) {
    this.sourceSet = sourceSet;
  }

  public ClassMetadataSummary relativePath(String relativePath) {
    this.relativePath = relativePath;
    return this;
  }

  /**
   * Path to the class relative to the repository root.
   * @return relativePath
  */
  
  @Schema(name = "relativePath", description = "Path to the class relative to the repository root.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("relativePath")
  public String getRelativePath() {
    return relativePath;
  }

  public void setRelativePath(String relativePath) {
    this.relativePath = relativePath;
  }

  public ClassMetadataSummary annotations(List<String> annotations) {
    this.annotations = annotations;
    return this;
  }

  public ClassMetadataSummary addAnnotationsItem(String annotationsItem) {
    if (this.annotations == null) {
      this.annotations = new ArrayList<>();
    }
    this.annotations.add(annotationsItem);
    return this;
  }

  /**
   * Fully qualified annotations present on the class.
   * @return annotations
  */
  
  @Schema(name = "annotations", description = "Fully qualified annotations present on the class.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("annotations")
  public List<String> getAnnotations() {
    return annotations;
  }

  public void setAnnotations(List<String> annotations) {
    this.annotations = annotations;
  }

  public ClassMetadataSummary interfacesImplemented(List<String> interfacesImplemented) {
    this.interfacesImplemented = interfacesImplemented;
    return this;
  }

  public ClassMetadataSummary addInterfacesImplementedItem(String interfacesImplementedItem) {
    if (this.interfacesImplemented == null) {
      this.interfacesImplemented = new ArrayList<>();
    }
    this.interfacesImplemented.add(interfacesImplementedItem);
    return this;
  }

  /**
   * Interfaces implemented by the class.
   * @return interfacesImplemented
  */
  
  @Schema(name = "interfacesImplemented", description = "Interfaces implemented by the class.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("interfacesImplemented")
  public List<String> getInterfacesImplemented() {
    return interfacesImplemented;
  }

  public void setInterfacesImplemented(List<String> interfacesImplemented) {
    this.interfacesImplemented = interfacesImplemented;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClassMetadataSummary classMetadataSummary = (ClassMetadataSummary) o;
    return Objects.equals(this.fullyQualifiedName, classMetadataSummary.fullyQualifiedName) &&
        Objects.equals(this.packageName, classMetadataSummary.packageName) &&
        Objects.equals(this.className, classMetadataSummary.className) &&
        Objects.equals(this.stereotype, classMetadataSummary.stereotype) &&
        Objects.equals(this.userCode, classMetadataSummary.userCode) &&
        Objects.equals(this.sourceSet, classMetadataSummary.sourceSet) &&
        Objects.equals(this.relativePath, classMetadataSummary.relativePath) &&
        Objects.equals(this.annotations, classMetadataSummary.annotations) &&
        Objects.equals(this.interfacesImplemented, classMetadataSummary.interfacesImplemented);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fullyQualifiedName, packageName, className, stereotype, userCode, sourceSet, relativePath, annotations, interfacesImplemented);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ClassMetadataSummary {\n");
    sb.append("    fullyQualifiedName: ").append(toIndentedString(fullyQualifiedName)).append("\n");
    sb.append("    packageName: ").append(toIndentedString(packageName)).append("\n");
    sb.append("    className: ").append(toIndentedString(className)).append("\n");
    sb.append("    stereotype: ").append(toIndentedString(stereotype)).append("\n");
    sb.append("    userCode: ").append(toIndentedString(userCode)).append("\n");
    sb.append("    sourceSet: ").append(toIndentedString(sourceSet)).append("\n");
    sb.append("    relativePath: ").append(toIndentedString(relativePath)).append("\n");
    sb.append("    annotations: ").append(toIndentedString(annotations)).append("\n");
    sb.append("    interfacesImplemented: ").append(toIndentedString(interfacesImplemented)).append("\n");
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

