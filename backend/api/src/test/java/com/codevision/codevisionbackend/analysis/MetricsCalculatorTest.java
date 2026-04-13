package com.codevision.codevisionbackend.analysis;

import static org.junit.jupiter.api.Assertions.*;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MetricsCalculatorTest {

    private MetricsCalculator calculator;
    private JavaParser parser;

    @BeforeEach
    void setUp() {
        calculator = new MetricsCalculator();
        parser = new JavaParser(new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE));
    }

    @Nested
    class Given_SimpleMethod {

        @Nested
        class When_CalculatingCyclomaticComplexity {

            @Test
            void Then_ReturnsOneForEmptyMethod() {
                var method = parseMethod("void empty() {}");
                assertEquals(1, calculator.cyclomaticComplexity(method));
            }

            @Test
            void Then_IncrementsForIfStatement() {
                var method = parseMethod("void m() { if (x) {} }");
                assertEquals(2, calculator.cyclomaticComplexity(method));
            }

            @Test
            void Then_IncrementsForElseIfChain() {
                var method = parseMethod("void m() { if (a) {} else if (b) {} else {} }");
                assertEquals(3, calculator.cyclomaticComplexity(method));
            }

            @Test
            void Then_IncrementsForForLoop() {
                var method = parseMethod("void m() { for (int i = 0; i < 10; i++) {} }");
                assertEquals(2, calculator.cyclomaticComplexity(method));
            }

            @Test
            void Then_IncrementsForWhileLoop() {
                var method = parseMethod("void m() { while (true) {} }");
                assertEquals(2, calculator.cyclomaticComplexity(method));
            }

            @Test
            void Then_IncrementsForDoWhileLoop() {
                var method = parseMethod("void m() { do {} while (true); }");
                assertEquals(2, calculator.cyclomaticComplexity(method));
            }

            @Test
            void Then_IncrementsForSwitchCases() {
                var method = parseMethod("void m() { switch(x) { case 1: break; case 2: break; default: break; } }");
                assertEquals(3, calculator.cyclomaticComplexity(method));
            }

            @Test
            void Then_IncrementsForCatchBlocks() {
                var method = parseMethod("void m() { try {} catch (Exception e) {} catch (Error e) {} }");
                assertEquals(3, calculator.cyclomaticComplexity(method));
            }

            @Test
            void Then_IncrementsForConditionalExpression() {
                var method = parseMethod("int m() { return a ? 1 : 2; }");
                assertEquals(2, calculator.cyclomaticComplexity(method));
            }

            @Test
            void Then_IncrementsForLogicalAnd() {
                var method = parseMethod("void m() { if (a && b) {} }");
                assertEquals(3, calculator.cyclomaticComplexity(method));
            }

            @Test
            void Then_IncrementsForLogicalOr() {
                var method = parseMethod("void m() { if (a || b) {} }");
                assertEquals(3, calculator.cyclomaticComplexity(method));
            }
        }
    }

    @Nested
    class Given_ComplexMethod {

        @Nested
        class When_CalculatingCyclomaticComplexity {

            @Test
            void Then_AccumulatesAllBranches() {
                var method = parseMethod("""
                        void complex() {
                            if (a) {
                                for (int i = 0; i < n; i++) {
                                    if (b || c) {
                                        try { } catch (Exception e) { }
                                    }
                                }
                            } else if (d) {
                                while (e) { }
                            }
                        }
                        """);
                // 1 (base) + 1 (if a) + 1 (for) + 1 (if b) + 1 (|| c) + 1 (catch) + 1 (else if d) + 1 (while e) = 8
                assertEquals(8, calculator.cyclomaticComplexity(method));
            }
        }
    }

    @Nested
    class Given_CognitiveComplexity {

        @Nested
        class When_CalculatingSimpleMethod {

            @Test
            void Then_ReturnsZeroForEmptyMethod() {
                var method = parseMethod("void empty() {}");
                assertEquals(0, calculator.cognitiveComplexity(method));
            }

            @Test
            void Then_IncrementsForIf() {
                var method = parseMethod("void m() { if (x) {} }");
                assertEquals(1, calculator.cognitiveComplexity(method));
            }

            @Test
            void Then_NestingIncreasesIncrement() {
                var method = parseMethod("void m() { if (a) { if (b) {} } }");
                // if(a) = 1 (nesting 0), if(b) = 2 (nesting 1) = total 3
                assertEquals(3, calculator.cognitiveComplexity(method));
            }

            @Test
            void Then_ElseAddsOne() {
                var method = parseMethod("void m() { if (a) {} else {} }");
                // if = 1, else = 1
                assertEquals(2, calculator.cognitiveComplexity(method));
            }
        }
    }

    @Nested
    class Given_LinesOfCode {

        @Nested
        class When_CountingLinesForClass {

            @Test
            void Then_CountsNonBlankNonCommentLines() {
                var unit = parseUnit("""
                        package com.example;

                        // This is a comment
                        public class Foo {
                            private int x;

                            public void bar() {
                                x = 1;
                            }
                        }
                        """);
                var type = unit.getType(0);
                int loc = calculator.linesOfCode(type);
                assertTrue(loc > 0, "LOC should be positive");
                assertTrue(loc <= 10, "LOC should not count blanks as full lines");
            }
        }

        @Nested
        class When_CountingLinesForMethod {

            @Test
            void Then_CountsMethodLines() {
                var method = parseMethod("void m() { int x = 1; int y = 2; return; }");
                int loc = calculator.linesOfCode(method);
                assertTrue(loc >= 1, "Method should have at least 1 LOC");
            }
        }
    }

    private MethodDeclaration parseMethod(String methodSource) {
        String classSource = "class Dummy { " + methodSource + " }";
        var result = parser.parse(classSource);
        assertTrue(result.getResult().isPresent(), "Parse should succeed");
        return result.getResult().get().getType(0)
                .getMethods().get(0);
    }

    private CompilationUnit parseUnit(String source) {
        var result = parser.parse(source);
        assertTrue(result.getResult().isPresent(), "Parse should succeed");
        return result.getResult().get();
    }
}
