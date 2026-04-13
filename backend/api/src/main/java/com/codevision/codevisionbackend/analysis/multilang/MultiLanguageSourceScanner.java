package com.codevision.codevisionbackend.analysis.multilang;

import com.codevision.codevisionbackend.config.AnalysisSafetyProperties;
import com.codevision.codevisionbackend.graph.KnowledgeGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;

/**
 * Orchestrator that walks a directory tree, parses every supported source file
 * through the {@link TreeSitterBridge}, and dispatches each parsed tree to the
 * matching {@link LanguageAnalyzer}(s).
 *
 * <p>Directory traversal uses a visited-set for cycle detection (symlink loops)
 * and a time-based deadline to avoid runaway scans. Both are
 * configuration-driven via {@code application.yml}.</p>
 */
@Component
public class MultiLanguageSourceScanner {

    private static final Logger log = LoggerFactory.getLogger(MultiLanguageSourceScanner.class);

    private final TreeSitterBridge bridge;
    private final List<LanguageAnalyzer> analyzers;
    private final LanguageRegistry registry;
    private final long maxRuntimeSeconds;

    public MultiLanguageSourceScanner(
            TreeSitterBridge bridge,
            List<LanguageAnalyzer> analyzers,
            LanguageRegistry registry,
            AnalysisSafetyProperties safetyProperties) {
        this.bridge = bridge;
        this.analyzers = analyzers;
        this.registry = registry;
        this.maxRuntimeSeconds = safetyProperties.maxRuntimeSeconds();
    }

    /**
     * Recursively scans a directory, parsing all supported source files and
     * aggregating the results into a single {@link KnowledgeGraph}.
     *
     * @param directory the root directory to scan
     * @return aggregated knowledge graph
     * @throws UncheckedIOException if the directory cannot be read
     */
    public KnowledgeGraph scanDirectory(Path directory) {
        var graph = new KnowledgeGraph();
        var deadline = Instant.now().plusSeconds(maxRuntimeSeconds);
        var visited = new HashSet<Path>();

        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    // Cycle detection via canonical path
                    var canonical = dir.toRealPath();
                    if (!visited.add(canonical)) {
                        log.debug("Skipping already-visited directory: {}", canonical);
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    // Time-based deadline
                    if (Instant.now().isAfter(deadline)) {
                        log.warn("Scan deadline reached; aborting directory walk");
                        return FileVisitResult.TERMINATE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (Instant.now().isAfter(deadline)) {
                        log.warn("Scan deadline reached; aborting directory walk");
                        return FileVisitResult.TERMINATE;
                    }
                    parseAndAnalyze(file, graph);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    log.warn("Failed to access file: {}", file, exc);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to walk directory: " + directory, e);
        }

        return graph;
    }

    /**
     * Parses and analyses a single source file.
     *
     * @param file the file to scan
     * @return knowledge graph containing any discovered nodes/edges
     */
    public KnowledgeGraph scanFile(Path file) {
        var graph = new KnowledgeGraph();
        parseAndAnalyze(file, graph);
        return graph;
    }

    private void parseAndAnalyze(Path file, KnowledgeGraph graph) {
        var filename = file.getFileName().toString();
        if (!bridge.isSupported(filename)) {
            return;
        }
        try {
            var sourceCode = Files.readString(file);
            var treeOpt = bridge.parse(sourceCode, filename);
            treeOpt.ifPresent(tree -> {
                boolean handled = false;
                for (var analyzer : analyzers) {
                    if (!(analyzer instanceof GenericTreeSitterAnalyzer)
                            && tree.languageName().equals(analyzer.languageName())) {
                        analyzer.analyze(tree, graph);
                        handled = true;
                    }
                }
                // Fallback: if no dedicated analyzer matched, use the generic one
                if (!handled) {
                    for (var analyzer : analyzers) {
                        if (analyzer instanceof GenericTreeSitterAnalyzer generic) {
                            generic.analyzeGeneric(tree, graph);
                            break;
                        }
                    }
                }
            });
        } catch (IOException e) {
            log.warn("Failed to read file: {}", file, e);
        }
    }
}
