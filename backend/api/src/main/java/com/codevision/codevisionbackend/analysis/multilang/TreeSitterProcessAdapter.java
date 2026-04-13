package com.codevision.codevisionbackend.analysis.multilang;

import com.codevision.codevisionbackend.config.TreeSitterProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Production tree-sitter adapter that shells out to a Node.js script to perform
 * real AST parsing. Annotated {@code @Primary} so that Spring injects this
 * instead of the placeholder {@link TreeSitterBridge}.
 *
 * <p>If Node.js or the tree-sitter grammars are not available, this adapter
 * gracefully falls back to the placeholder behaviour from the superclass.</p>
 */
@Component
@Primary
public class TreeSitterProcessAdapter extends TreeSitterBridge {

    private static final Logger log = LoggerFactory.getLogger(TreeSitterProcessAdapter.class);
    private static final int OUTPUT_BYTES_LIMIT = 50 * 1024 * 1024; // 50 MB

    private final TreeSitterProperties properties;
    private volatile boolean available;

    public TreeSitterProcessAdapter(LanguageRegistry registry,
                                    TreeSitterProperties properties) {
        super(registry);
        this.properties = properties;
    }

    @PostConstruct
    void checkAvailability() {
        if (!properties.enabled()) {
            log.info("Tree-sitter integration disabled via configuration");
            available = false;
            return;
        }
        try {
            var process = new ProcessBuilder(properties.nodePath(), "--version")
                    .redirectErrorStream(true)
                    .start();
            // Read stdout before waitFor to prevent pipe buffer deadlock
            var output = readBounded(process.getInputStream());
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("Tree-sitter bridge: Node.js availability check timed out");
                available = false;
                return;
            }
            if (process.exitValue() == 0) {
                var version = output.trim();
                log.info("Tree-sitter bridge: Node.js {} available", version);
                available = true;
            } else {
                log.warn("Tree-sitter bridge: Node.js not available (exit={})", process.exitValue());
                available = false;
            }
        } catch (IOException | InterruptedException e) {
            log.warn("Tree-sitter bridge: Node.js not available — {}", e.getMessage());
            available = false;
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Returns whether the Node.js tree-sitter bridge is available.
     */
    public boolean isAvailable() {
        return available;
    }

    @Override
    protected List<ParsedNode> parseWithGrammar(String source, String language) {
        if (!available) {
            return super.parseWithGrammar(source, language);
        }

        // Validate language against allowlist pattern
        if (!language.matches("[a-z_]+")) {
            log.warn("Invalid tree-sitter language name: {}", language);
            return super.parseWithGrammar(source, language);
        }

        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("ts-parse-", "-" + language);
            Files.writeString(tempFile, source);

            var scriptPath = Path.of(properties.scriptPath()).toAbsolutePath().toString();
            var pb = new ProcessBuilder(
                    properties.nodePath(),
                    scriptPath,
                    tempFile.toAbsolutePath().toString(),
                    language
            );
            pb.redirectErrorStream(false);

            var process = pb.start();

            // Read stdout before waitFor to prevent pipe buffer deadlock
            var stdout = readBounded(process.getInputStream());
            var stderr = readBounded(process.getErrorStream());

            boolean finished = process.waitFor(properties.timeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("Tree-sitter parse timed out after {}s for language {}",
                        properties.timeoutSeconds(), language);
                return super.parseWithGrammar(source, language);
            }

            if (process.exitValue() != 0) {
                log.warn("Tree-sitter parse failed (exit={}) for language {}: {}",
                        process.exitValue(), language,
                        truncate(stdout + " " + stderr, 500));
                return super.parseWithGrammar(source, language);
            }

            return TreeSitterJsonMapper.parse(stdout);

        } catch (IOException e) {
            log.warn("Tree-sitter parse I/O error for language {}: {}", language, e.getMessage());
            return super.parseWithGrammar(source, language);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Tree-sitter parse interrupted for language {}", language);
            return super.parseWithGrammar(source, language);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    log.debug("Failed to delete temp file {}: {}", tempFile, e.getMessage());
                }
            }
        }
    }

    /**
     * Reads up to {@link #OUTPUT_BYTES_LIMIT} from an input stream, preventing
     * unbounded memory consumption from large process outputs.
     */
    private static String readBounded(InputStream inputStream) throws IOException {
        var bytes = inputStream.readNBytes(OUTPUT_BYTES_LIMIT);
        return new String(bytes);
    }

    private static String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...[truncated]";
    }
}
