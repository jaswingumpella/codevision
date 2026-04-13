package com.codevision.codevisionbackend.analysis;

import static org.junit.jupiter.api.Assertions.*;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DocumentationExtractorTest {

    private DocumentationExtractor extractor;
    private JavaParser parser;

    @BeforeEach
    void setUp() {
        extractor = new DocumentationExtractor();
        parser = new JavaParser(new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE));
    }

    @Nested
    class Given_ClassWithJavadoc {

        @Nested
        class When_ExtractingDocumentation {

            @Test
            void Then_ExtractsMainDescription() {
                var type = parseType("""
                        /**
                         * This is the main description.
                         */
                        public class Foo {}
                        """);
                var doc = extractor.extractClassDocumentation(type);
                assertTrue(doc.isPresent());
                assertTrue(doc.get().contains("This is the main description"));
            }

            @Test
            void Then_ExtractsMultiLineDescription() {
                var type = parseType("""
                        /**
                         * Line one.
                         * Line two.
                         */
                        public class Foo {}
                        """);
                var doc = extractor.extractClassDocumentation(type);
                assertTrue(doc.isPresent());
                assertTrue(doc.get().contains("Line one"));
                assertTrue(doc.get().contains("Line two"));
            }
        }
    }

    @Nested
    class Given_ClassWithoutJavadoc {

        @Nested
        class When_ExtractingDocumentation {

            @Test
            void Then_ReturnsEmpty() {
                var type = parseType("public class Foo {}");
                var doc = extractor.extractClassDocumentation(type);
                assertTrue(doc.isEmpty());
            }
        }
    }

    @Nested
    class Given_MethodWithJavadoc {

        @Nested
        class When_ExtractingDocumentation {

            @Test
            void Then_ExtractsDescription() {
                var unit = parseUnit("""
                        public class Foo {
                            /**
                             * Does something important.
                             * @param x the input
                             * @return the result
                             */
                            public int bar(int x) { return x; }
                        }
                        """);
                var method = unit.getType(0).getMethods().get(0);
                var doc = extractor.extractMethodDocumentation(method);
                assertTrue(doc.isPresent());
                assertTrue(doc.get().contains("Does something important"));
            }
        }
    }

    @Nested
    class Given_MethodWithoutJavadoc {

        @Nested
        class When_ExtractingDocumentation {

            @Test
            void Then_ReturnsEmpty() {
                var unit = parseUnit("""
                        public class Foo {
                            public int bar(int x) { return x; }
                        }
                        """);
                var method = unit.getType(0).getMethods().get(0);
                var doc = extractor.extractMethodDocumentation(method);
                assertTrue(doc.isEmpty());
            }
        }
    }

    @Nested
    class Given_ClassWithAuthorTag {

        @Nested
        class When_ExtractingAuthor {

            @Test
            void Then_ReturnsAuthor() {
                var type = parseType("""
                        /**
                         * Some class.
                         * @author John Doe
                         */
                        public class Foo {}
                        """);
                var author = extractor.extractAuthor(type);
                assertTrue(author.isPresent());
                assertEquals("John Doe", author.get());
            }
        }
    }

    @Nested
    class Given_ClassWithSinceTag {

        @Nested
        class When_ExtractingSince {

            @Test
            void Then_ReturnsSince() {
                var type = parseType("""
                        /**
                         * Some class.
                         * @since 1.0
                         */
                        public class Foo {}
                        """);
                var since = extractor.extractSince(type);
                assertTrue(since.isPresent());
                assertEquals("1.0", since.get());
            }
        }
    }

    private TypeDeclaration<?> parseType(String source) {
        var result = parser.parse(source);
        assertTrue(result.getResult().isPresent(), "Parse should succeed");
        return result.getResult().get().getType(0);
    }

    private CompilationUnit parseUnit(String source) {
        var result = parser.parse(source);
        assertTrue(result.getResult().isPresent(), "Parse should succeed");
        return result.getResult().get();
    }
}
