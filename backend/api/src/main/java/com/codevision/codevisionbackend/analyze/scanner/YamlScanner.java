package com.codevision.codevisionbackend.analyze.scanner;

import com.codevision.codevisionbackend.analyze.MetadataDump;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class YamlScanner {

    private static final Logger log = LoggerFactory.getLogger(YamlScanner.class);
    private static final Set<String> IGNORED_DIRECTORIES =
            Set.of(".git", "target", "build", "node_modules", ".idea", ".gradle");

    private final WsdlInspector wsdlInspector;

    public YamlScanner(WsdlInspector wsdlInspector) {
        this.wsdlInspector = wsdlInspector;
    }

    public MetadataDump scan(Path repoRoot) {
        if (!Files.exists(repoRoot)) {
            return MetadataDump.empty();
        }

        List<MetadataDump.OpenApiSpec> openApiSpecs = new ArrayList<>();
        List<MetadataDump.SpecDocument> wsdlDocuments = new ArrayList<>();
        List<MetadataDump.SpecDocument> xsdDocuments = new ArrayList<>();
        List<MetadataDump.SoapServiceSummary> soapServices = new ArrayList<>();

        FileVisitor<Path> visitor = new SimpleFileVisitor<>() {

            private final Set<Path> visitedDirectories = new HashSet<>();

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                Path dirName = dir.getFileName();
                if (dirName != null && IGNORED_DIRECTORIES.contains(dirName.toString())) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                Path normalized = dir.toAbsolutePath().normalize();
                if (!visitedDirectories.add(normalized)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (!attrs.isRegularFile()) {
                    return FileVisitResult.CONTINUE;
                }

                String filename = file.getFileName().toString();
                String normalized = filename.toLowerCase(Locale.ROOT);
                try {
                    if (isOpenApiFile(normalized)) {
                        readFile(file).ifPresent(content -> openApiSpecs.add(
                                new MetadataDump.OpenApiSpec(filename, content)));
                    } else if (normalized.endsWith(".wsdl")) {
                        readFile(file).ifPresent(content -> {
                            wsdlDocuments.add(new MetadataDump.SpecDocument(filename, content));
                            soapServices.addAll(wsdlInspector.inspect(content, filename));
                        });
                    } else if (normalized.endsWith(".xsd")) {
                        readFile(file)
                                .ifPresent(content -> xsdDocuments.add(new MetadataDump.SpecDocument(filename, content)));
                    }
                } catch (Exception ex) {
                    log.debug("Failed processing file {}: {}", file, ex.getMessage());
                }
                return FileVisitResult.CONTINUE;
            }
        };

        try {
            Files.walkFileTree(repoRoot, visitor);
        } catch (IOException e) {
            log.warn("Failed walking YAML/XML files at {}: {}", repoRoot, e.getMessage());
        }

        if (openApiSpecs.isEmpty() && wsdlDocuments.isEmpty() && xsdDocuments.isEmpty() && soapServices.isEmpty()) {
            return MetadataDump.empty();
        }

        return new MetadataDump(openApiSpecs, wsdlDocuments, xsdDocuments, soapServices);
    }

    private boolean isOpenApiFile(String normalizedName) {
        return normalizedName.startsWith("openapi") && (normalizedName.endsWith(".yml") || normalizedName.endsWith(".yaml"));
    }

    private java.util.Optional<String> readFile(Path file) {
        try {
            return java.util.Optional.of(Files.readString(file));
        } catch (IOException e) {
            log.debug("Failed reading {}: {}", file, e.getMessage());
            return java.util.Optional.empty();
        }
    }
}
