package com.codevision.codevisionbackend.dependency;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Strategy interface for resolving project dependencies into a tree structure.
 * Each build system (Maven, Gradle, npm, pip, etc.) implements this interface.
 */
public interface DependencyResolver {

    /**
     * Returns true if this resolver can handle the project at the given root.
     */
    boolean supports(Path projectRoot);

    /**
     * Resolves the full transitive dependency tree for the project.
     *
     * @param projectRoot the root directory of the project
     * @param exclusionConfig patterns for filtering out known/unneeded dependencies
     * @return the dependency tree, or empty if resolution fails
     */
    Optional<DependencyTree> resolve(Path projectRoot, ExclusionConfig exclusionConfig);

    /**
     * Returns the build system name (e.g., "maven", "gradle", "npm").
     */
    String buildSystem();
}
