package com.codevision.codevisionbackend.dependency;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ExclusionConfigTest {

    @Nested
    class Given_DefaultExclusions {

        @Nested
        class When_CheckingSpringFramework {

            @Test
            void Then_SpringCoreIsExcluded() {
                var config = ExclusionConfig.defaults();
                var artifact = new ResolvedArtifact("org.springframework", "spring-core", "6.1.0");
                assertTrue(config.isExcluded(artifact));
            }

            @Test
            void Then_SpringBootStarterIsExcluded() {
                var config = ExclusionConfig.defaults();
                var artifact = new ResolvedArtifact("org.springframework.boot", "spring-boot-starter", "3.2.0");
                assertTrue(config.isExcluded(artifact));
            }
        }

        @Nested
        class When_CheckingJakarta {

            @Test
            void Then_JakartaIsExcluded() {
                var config = ExclusionConfig.defaults();
                var artifact = new ResolvedArtifact("jakarta.persistence", "jakarta.persistence-api", "3.1.0");
                assertTrue(config.isExcluded(artifact));
            }
        }

        @Nested
        class When_CheckingUserCode {

            @Test
            void Then_CustomGroupIsNotExcluded() {
                var config = ExclusionConfig.defaults();
                var artifact = new ResolvedArtifact("com.mycompany", "my-library", "1.0.0");
                assertFalse(config.isExcluded(artifact));
            }
        }
    }

    @Nested
    class Given_NoExclusions {

        @Nested
        class When_CheckingAnyArtifact {

            @Test
            void Then_NothingIsExcluded() {
                var config = ExclusionConfig.none();
                var artifact = new ResolvedArtifact("org.springframework", "spring-core", "6.1.0");
                assertFalse(config.isExcluded(artifact));
            }
        }
    }

    @Nested
    class Given_CustomPatterns {

        @Nested
        class When_CheckingMatchingArtifact {

            @Test
            void Then_ExactMatchExcludes() {
                var config = new ExclusionConfig(List.of("com.internal:secret-lib"));
                var artifact = new ResolvedArtifact("com.internal", "secret-lib", "1.0.0");
                assertTrue(config.isExcluded(artifact));
            }

            @Test
            void Then_GlobMatchExcludes() {
                var config = new ExclusionConfig(List.of("com.internal.*"));
                var artifact = new ResolvedArtifact("com.internal.utils", "helper", "1.0.0");
                assertTrue(config.isExcluded(artifact));
            }

            @Test
            void Then_NonMatchingArtifactIsNotExcluded() {
                var config = new ExclusionConfig(List.of("com.internal.*"));
                var artifact = new ResolvedArtifact("com.external", "lib", "1.0.0");
                assertFalse(config.isExcluded(artifact));
            }
        }
    }

    @Nested
    class Given_NullArtifact {

        @Nested
        class When_Checking {

            @Test
            void Then_NullIsExcluded() {
                var config = ExclusionConfig.defaults();
                assertTrue(config.isExcluded(null));
            }
        }
    }

    @Nested
    class Given_GroupExclusions {

        @Nested
        class When_CheckingGroup {

            @Test
            void Then_ExcludedGroupReturnsTrue() {
                var config = ExclusionConfig.defaults();
                assertTrue(config.isExcludedGroup("org.springframework.boot"));
            }

            @Test
            void Then_NullGroupReturnsTrue() {
                var config = ExclusionConfig.defaults();
                assertTrue(config.isExcludedGroup(null));
            }

            @Test
            void Then_CustomGroupReturnsFalse() {
                var config = ExclusionConfig.defaults();
                assertFalse(config.isExcludedGroup("com.mycompany"));
            }
        }
    }
}
