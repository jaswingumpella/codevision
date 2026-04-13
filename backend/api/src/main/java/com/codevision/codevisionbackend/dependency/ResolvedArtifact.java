package com.codevision.codevisionbackend.dependency;

/**
 * Represents a resolved dependency artifact with full coordinates.
 */
public record ResolvedArtifact(
        String groupId,
        String artifactId,
        String version,
        String scope,
        String type,
        String classifier,
        boolean optional) {

    public String coordinates() {
        var sb = new StringBuilder(groupId).append(":").append(artifactId);
        if (classifier != null && !classifier.isBlank()) {
            sb.append(":").append(classifier);
        }
        sb.append(":").append(version);
        return sb.toString();
    }

    public ResolvedArtifact(String groupId, String artifactId, String version) {
        this(groupId, artifactId, version, "compile", "jar", null, false);
    }
}
