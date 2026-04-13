package com.codevision.codevisionbackend.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ConfidenceLevel")
class ConfidenceLevelTest {

    @Nested
    @DisplayName("Given EXTRACTED confidence")
    class Given_ExtractedConfidence {

        @Nested
        @DisplayName("When getting score")
        class When_GettingScore {

            @Test
            @DisplayName("Then returns 1.0")
            void Then_Returns1Point0() {
                assertEquals(1.0, ConfidenceLevel.EXTRACTED.score(), 0.0001);
            }
        }
    }

    @Nested
    @DisplayName("Given RESOLVED confidence")
    class Given_ResolvedConfidence {

        @Nested
        @DisplayName("When getting score")
        class When_GettingScore {

            @Test
            @DisplayName("Then returns 0.95")
            void Then_Returns0Point95() {
                assertEquals(0.95, ConfidenceLevel.RESOLVED.score(), 0.0001);
            }
        }
    }

    @Nested
    @DisplayName("Given INFERRED confidence")
    class Given_InferredConfidence {

        @Nested
        @DisplayName("When getting score")
        class When_GettingScore {

            @Test
            @DisplayName("Then returns 0.7")
            void Then_Returns0Point7() {
                assertEquals(0.7, ConfidenceLevel.INFERRED.score(), 0.0001);
            }
        }
    }

    @Nested
    @DisplayName("Given AMBIGUOUS confidence")
    class Given_AmbiguousConfidence {

        @Nested
        @DisplayName("When getting score")
        class When_GettingScore {

            @Test
            @DisplayName("Then returns 0.3")
            void Then_Returns0Point3() {
                assertEquals(0.3, ConfidenceLevel.AMBIGUOUS.score(), 0.0001);
            }
        }
    }

    @Nested
    @DisplayName("Given all confidence levels")
    class Given_AllConfidenceLevels {

        @Test
        @DisplayName("Then scores are in descending order")
        void Then_ScoresInDescendingOrder() {
            double extracted = ConfidenceLevel.EXTRACTED.score();
            double resolved = ConfidenceLevel.RESOLVED.score();
            double inferred = ConfidenceLevel.INFERRED.score();
            double ambiguous = ConfidenceLevel.AMBIGUOUS.score();

            assertEquals(true, extracted > resolved);
            assertEquals(true, resolved > inferred);
            assertEquals(true, inferred > ambiguous);
        }

        @Test
        @DisplayName("Then there are exactly four values")
        void Then_ExactlyFourValues() {
            assertEquals(4, ConfidenceLevel.values().length);
        }
    }
}
