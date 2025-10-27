package com.codevision.codevisionbackend.analyze;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BuildInfo(
        String groupId,
        String artifactId,
        String version,
        String javaVersion) {

    public static BuildInfo empty() {
        return new BuildInfo(null, null, null, null);
    }

    public BuildInfo merge(BuildInfo other) {
        if (other == null) {
            return this;
        }
        return new BuildInfo(
                firstNonBlank(other.groupId, groupId),
                firstNonBlank(other.artifactId, artifactId),
                firstNonBlank(other.version, version),
                firstNonBlank(other.javaVersion, javaVersion));
    }

    private static String firstNonBlank(String candidate, String fallback) {
        return (candidate != null && !candidate.isBlank()) ? candidate : fallback;
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
        return Objects.equals(groupId, buildInfo.groupId)
                && Objects.equals(artifactId, buildInfo.artifactId)
                && Objects.equals(version, buildInfo.version)
                && Objects.equals(javaVersion, buildInfo.javaVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, version, javaVersion);
    }
}
