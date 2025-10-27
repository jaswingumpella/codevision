package com.codevision.codevisionbackend.analyze.scanner;

import com.codevision.codevisionbackend.analyze.BuildInfo;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BuildMetadataExtractor {

    private static final Logger log = LoggerFactory.getLogger(BuildMetadataExtractor.class);
    private static final String DEFAULT_JAVA_VERSION = "unknown";

    public BuildMetadata extract(Path repoRoot) {
        Path normalizedRoot = repoRoot.toAbsolutePath().normalize();
        Set<Path> moduleRoots = new LinkedHashSet<>();
        moduleRoots.add(normalizedRoot);

        Path rootPom = normalizedRoot.resolve("pom.xml");
        if (!Files.exists(rootPom)) {
            log.info("No pom.xml found at {}. Returning empty build metadata.", rootPom);
            return new BuildMetadata(BuildInfo.empty(), List.copyOf(moduleRoots));
        }

        try {
            Model model = readModel(rootPom);
            if (model == null) {
                return new BuildMetadata(BuildInfo.empty(), List.copyOf(moduleRoots));
            }
            BuildInfo buildInfo = new BuildInfo(
                    resolveGroupId(model),
                    safeTrim(model.getArtifactId()),
                    resolveVersion(model),
                    resolveJavaVersion(model));

            collectModules(normalizedRoot, model, moduleRoots, new LinkedHashSet<>());
            return new BuildMetadata(buildInfo, List.copyOf(moduleRoots));
        } catch (IOException e) {
            log.warn("Failed reading build metadata from {}", rootPom, e);
            return new BuildMetadata(BuildInfo.empty(), List.copyOf(moduleRoots));
        }
    }

    private void collectModules(Path moduleBase, Model model, Set<Path> moduleRoots, Set<Path> visited) {
        Path normalizedBase = moduleBase.toAbsolutePath().normalize();
        if (!visited.add(normalizedBase)) {
            return;
        }

        List<String> modules = Optional.ofNullable(model.getModules()).orElse(List.of());
        for (String module : modules) {
            if (module == null || module.isBlank()) {
                continue;
            }
            Path modulePath = normalizedBase.resolve(module).normalize();
            if (!Files.isDirectory(modulePath)) {
                continue;
            }
            moduleRoots.add(modulePath);
            Path modulePom = modulePath.resolve("pom.xml");
            if (!Files.exists(modulePom)) {
                continue;
            }
            Model childModel = readModelQuietly(modulePom);
            if (childModel != null) {
                collectModules(modulePath, childModel, moduleRoots, visited);
            }
        }
    }

    private Model readModel(Path pomPath) throws IOException {
        try (Reader reader = Files.newBufferedReader(pomPath)) {
            MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();
            return xpp3Reader.read(reader);
        } catch (Exception e) {
            throw new IOException("Failed to parse " + pomPath, e);
        }
    }

    private Model readModelQuietly(Path pomPath) {
        try {
            return readModel(pomPath);
        } catch (IOException ex) {
            log.warn("Failed to parse child module pom at {}", pomPath, ex);
            return null;
        }
    }

    private String resolveGroupId(Model model) {
        String directGroupId = safeTrim(model.getGroupId());
        if (directGroupId != null) {
            return directGroupId;
        }
        Parent parent = model.getParent();
        return parent != null ? safeTrim(parent.getGroupId()) : null;
    }

    private String resolveVersion(Model model) {
        String version = safeTrim(model.getVersion());
        if (version != null) {
            return version;
        }
        Parent parent = model.getParent();
        return parent != null ? safeTrim(parent.getVersion()) : null;
    }

    private String resolveJavaVersion(Model model) {
        Properties properties = model.getProperties();
        if (properties != null) {
            String propVersion = firstNonBlank(
                    properties.getProperty("java.version"),
                    properties.getProperty("maven.compiler.release"),
                    properties.getProperty("maven.compiler.target"),
                    properties.getProperty("maven.compiler.source"));
            if (propVersion != null) {
                return propVersion;
            }
        }

        Build build = model.getBuild();
        if (build != null) {
            for (Plugin plugin : build.getPlugins()) {
                if (!"maven-compiler-plugin".equals(plugin.getArtifactId())) {
                    continue;
                }
                String pluginVersion = resolveJavaVersionFromPlugin(plugin.getConfiguration());
                if (pluginVersion != null) {
                    return pluginVersion;
                }
            }
        }
        return DEFAULT_JAVA_VERSION;
    }

    private String resolveJavaVersionFromPlugin(Object configuration) {
        if (configuration == null) {
            return null;
        }
        String configText = configuration.toString();
        String release = extractTagValue(configText, "release");
        if (release != null) {
            return release;
        }
        String target = extractTagValue(configText, "target");
        if (target != null) {
            return target;
        }
        return extractTagValue(configText, "source");
    }

    private String extractTagValue(String xml, String tagName) {
        if (xml == null || xml.isBlank()) {
            return null;
        }
        String open = "<" + tagName + ">";
        String close = "</" + tagName + ">";
        int start = xml.indexOf(open);
        int end = xml.indexOf(close);
        if (start >= 0 && end > start) {
            return safeTrim(xml.substring(start + open.length(), end));
        }
        return null;
    }

    private String safeTrim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String firstNonBlank(String... candidates) {
        return Arrays.stream(candidates)
                .map(this::safeTrim)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    public record BuildMetadata(BuildInfo buildInfo, List<Path> moduleRoots) {

        public BuildMetadata {
            buildInfo = Objects.requireNonNullElse(buildInfo, BuildInfo.empty());
            moduleRoots = moduleRoots == null ? List.of() : List.copyOf(moduleRoots);
        }
    }
}
