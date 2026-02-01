package com.codevision.codevisionbackend.api.model;

import java.net.URI;
import java.util.Objects;
import com.codevision.codevisionbackend.api.model.ImageAsset;
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
 * AssetInventory
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-02-01T02:54:38.384001-05:00[America/New_York]", comments = "Generator version: 7.5.0")
public class AssetInventory {

  @Valid
  private List<@Valid ImageAsset> images = new ArrayList<>();

  public AssetInventory images(List<@Valid ImageAsset> images) {
    this.images = images;
    return this;
  }

  public AssetInventory addImagesItem(ImageAsset imagesItem) {
    if (this.images == null) {
      this.images = new ArrayList<>();
    }
    this.images.add(imagesItem);
    return this;
  }

  /**
   * Image assets discovered in the repository.
   * @return images
  */
  @Valid 
  @Schema(name = "images", description = "Image assets discovered in the repository.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("images")
  public List<@Valid ImageAsset> getImages() {
    return images;
  }

  public void setImages(List<@Valid ImageAsset> images) {
    this.images = images;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AssetInventory assetInventory = (AssetInventory) o;
    return Objects.equals(this.images, assetInventory.images);
  }

  @Override
  public int hashCode() {
    return Objects.hash(images);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AssetInventory {\n");
    sb.append("    images: ").append(toIndentedString(images)).append("\n");
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

