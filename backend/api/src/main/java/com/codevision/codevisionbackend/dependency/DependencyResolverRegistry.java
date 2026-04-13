package com.codevision.codevisionbackend.dependency;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Registry that auto-detects the build system and delegates to the appropriate resolver.
 */
@Component
public class DependencyResolverRegistry {

    private static final Logger log = LoggerFactory.getLogger(DependencyResolverRegistry.class);

    private final List<DependencyResolver> resolvers;

    public DependencyResolverRegistry(List<DependencyResolver> resolvers) {
        this.resolvers = resolvers;
    }

    /**
     * Finds the first resolver that supports the given project root.
     */
    public Optional<DependencyResolver> detect(Path projectRoot) {
        for (DependencyResolver resolver : resolvers) {
            if (resolver.supports(projectRoot)) {
                log.info("Detected {} build system at {}", resolver.buildSystem(), projectRoot);
                return Optional.of(resolver);
            }
        }
        log.warn("No dependency resolver supports project at {}", projectRoot);
        return Optional.empty();
    }

    /**
     * Resolves dependencies using the auto-detected resolver.
     */
    public Optional<DependencyTree> resolveFor(Path projectRoot, ExclusionConfig exclusionConfig) {
        return detect(projectRoot).flatMap(resolver -> resolver.resolve(projectRoot, exclusionConfig));
    }

    /**
     * Returns the list of registered resolvers.
     */
    public List<DependencyResolver> registeredResolvers() {
        return List.copyOf(resolvers);
    }
}
