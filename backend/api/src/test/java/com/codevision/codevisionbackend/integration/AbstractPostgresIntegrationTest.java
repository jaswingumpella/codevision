package com.codevision.codevisionbackend.integration;

import com.codevision.codevisionbackend.analysis.TestFixtures;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class that wires a reusable Postgres Testcontainer and common Spring Boot overrides so the
 * integration tests can run against a deterministic database + filesystem layout.
 */
abstract class AbstractPostgresIntegrationTest {

    private static final boolean DOCKER_AVAILABLE = isDockerAvailable();
    private static final PostgreSQLContainer<?> POSTGRES = DOCKER_AVAILABLE
            ? new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("codevision_it")
                    .withUsername("codevision")
                    .withPassword("codevision")
            : null;
    private static final Path COMPILED_OUTPUT_DIR = createTempDirectory("codevision-compiled-output-");
    private static final Path DIAGRAM_OUTPUT_DIR = createTempDirectory("codevision-diagram-output-");
    private static final String API_KEY = "test-api-key";

    static {
        if (POSTGRES != null) {
            POSTGRES.start();
        }
    }

    @BeforeAll
    static void requireDocker() {
        Assumptions.assumeTrue(DOCKER_AVAILABLE, "Docker is required for backend integration tests");
    }

    @DynamicPropertySource
    static void registerIntegrationProperties(DynamicPropertyRegistry registry) {
        if (DOCKER_AVAILABLE) {
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES::getUsername);
            registry.add("spring.datasource.password", POSTGRES::getPassword);
            registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
            registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        } else {
            registry.add("spring.datasource.url", () -> "jdbc:h2:mem:codevision-it;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
            registry.add("spring.datasource.driverClassName", () -> "org.h2.Driver");
            registry.add("spring.datasource.username", () -> "sa");
            registry.add("spring.datasource.password", () -> "");
            registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
            registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.H2Dialect");
        }
        registry.add("analysis.accept-packages", () -> "com.codevision.fixtures");
        registry.add("analysis.include-dependencies", () -> "false");
        registry.add("analysis.compile.auto", () -> "false");
        registry.add("analysis.output.root", () -> COMPILED_OUTPUT_DIR.toString());
        registry.add("diagram.storage.root", () -> DIAGRAM_OUTPUT_DIR.toString());
        registry.add("diagram.svg.enabled", () -> "false");
        registry.add("security.apiKey", () -> API_KEY);
    }

    protected Path copyCompiledFixture(Path tempDir) {
        try {
            return TestFixtures.copyCompiledFixture(tempDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to copy compiled fixture", e);
        }
    }

    protected String apiKey() {
        return API_KEY;
    }

    protected static boolean isDockerAvailable() {
        try {
            DockerClientFactory.instance().client();
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Path createTempDirectory(String prefix) {
        try {
            Path dir = Files.createTempDirectory(prefix);
            dir.toFile().deleteOnExit();
            return dir;
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to create temp directory for integration tests", e);
        }
    }
}
