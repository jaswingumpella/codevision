package com.codevision.codevisionbackend.analyze;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AssetInventory(List<ImageAsset> images) {

    public AssetInventory {
        images = images == null ? List.of() : List.copyOf(images);
    }

    public static AssetInventory empty() {
        return new AssetInventory(List.of());
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ImageAsset(String fileName, String relativePath, long sizeBytes, String sha256) {}
}

