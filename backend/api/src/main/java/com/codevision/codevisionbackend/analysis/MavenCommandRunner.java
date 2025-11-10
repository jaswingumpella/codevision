package com.codevision.codevisionbackend.analysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Executes Maven commands with logging, heap safeguards, and timeout enforcement.
 */
@Component
public class MavenCommandRunner {

    private static final Logger log = LoggerFactory.getLogger(MavenCommandRunner.class);

    public void run(Path workingDir, List<String> command, Duration timeout, int maxHeapMb) {
        Instant start = Instant.now();
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workingDir.toFile());
        builder.redirectErrorStream(true);
        builder.environment().putIfAbsent("MAVEN_OPTS", "-Xmx" + maxHeapMb + "m");

        try {
            Process process = builder.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("[mvn] {}", line);
                }
            }
            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("Maven command timed out after " + timeout.getSeconds() + " seconds");
            }
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new IllegalStateException("Maven command failed with exit code " + exitCode);
            }
            log.info("Command {} completed in {} ms", command, Duration.between(start, Instant.now()).toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Maven command interrupted: " + ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed running command " + command + ": " + ex.getMessage(), ex);
        }
    }
}
