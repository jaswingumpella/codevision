package com.codevision.codevisionbackend.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "diagram.storage")
public class DiagramStorageProperties {

    /**
     * Root directory where rendered diagram assets (SVGs) will be written. Defaults to
     * {@code ./data/diagrams} relative to the backend module.
     */
    private String root = "./data/diagrams";

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        if (root == null || root.isBlank()) {
            return;
        }
        this.root = root;
    }

    public Path resolveRoot() {
        return Paths.get(root).toAbsolutePath().normalize();
    }
}
