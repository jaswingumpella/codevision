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
 * ImageAsset
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-06T22:33:59.387668-05:00[America/New_York]", comments = "Generator version: 7.5.0")
public class ImageAsset {

  private String fileName;

  private String relativePath;

  private Long sizeBytes;

  private String sha256;

  public ImageAsset() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ImageAsset(String fileName, String relativePath) {
    this.fileName = fileName;
    this.relativePath = relativePath;
  }

  public ImageAsset fileName(String fileName) {
    this.fileName = fileName;
    return this;
  }

  /**
   * Image file name.
   * @return fileName
  */
  @NotNull 
  @Schema(name = "fileName", description = "Image file name.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("fileName")
  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public ImageAsset relativePath(String relativePath) {
    this.relativePath = relativePath;
    return this;
  }

  /**
   * Path to the asset relative to the repository root.
   * @return relativePath
  */
  @NotNull 
  @Schema(name = "relativePath", description = "Path to the asset relative to the repository root.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("relativePath")
  public String getRelativePath() {
    return relativePath;
  }

  public void setRelativePath(String relativePath) {
    this.relativePath = relativePath;
  }

  public ImageAsset sizeBytes(Long sizeBytes) {
    this.sizeBytes = sizeBytes;
    return this;
  }

  /**
   * File size in bytes.
   * @return sizeBytes
  */
  
  @Schema(name = "sizeBytes", description = "File size in bytes.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("sizeBytes")
  public Long getSizeBytes() {
    return sizeBytes;
  }

  public void setSizeBytes(Long sizeBytes) {
    this.sizeBytes = sizeBytes;
  }

  public ImageAsset sha256(String sha256) {
    this.sha256 = sha256;
    return this;
  }

  /**
   * SHA-256 hash of the asset for integrity verification.
   * @return sha256
  */
  
  @Schema(name = "sha256", description = "SHA-256 hash of the asset for integrity verification.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("sha256")
  public String getSha256() {
    return sha256;
  }

  public void setSha256(String sha256) {
    this.sha256 = sha256;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ImageAsset imageAsset = (ImageAsset) o;
    return Objects.equals(this.fileName, imageAsset.fileName) &&
        Objects.equals(this.relativePath, imageAsset.relativePath) &&
        Objects.equals(this.sizeBytes, imageAsset.sizeBytes) &&
        Objects.equals(this.sha256, imageAsset.sha256);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fileName, relativePath, sizeBytes, sha256);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ImageAsset {\n");
    sb.append("    fileName: ").append(toIndentedString(fileName)).append("\n");
    sb.append("    relativePath: ").append(toIndentedString(relativePath)).append("\n");
    sb.append("    sizeBytes: ").append(toIndentedString(sizeBytes)).append("\n");
    sb.append("    sha256: ").append(toIndentedString(sha256)).append("\n");
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

