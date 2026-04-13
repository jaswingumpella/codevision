package com.codevision.codevisionbackend.analysis;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.WhileStmt;
import org.springframework.stereotype.Component;

/**
 * Calculates cyclomatic complexity, cognitive complexity, and lines of code for Java AST nodes.
 */
@Component
public class MetricsCalculator {

    /**
     * McCabe cyclomatic complexity: 1 (base) + number of decision points.
     */
    public int cyclomaticComplexity(MethodDeclaration method) {
        int complexity = 1;
        for (var node : method.findAll(Node.class)) {
            if (node instanceof IfStmt) {
                complexity++;
            } else if (node instanceof ForStmt || node instanceof ForEachStmt) {
                complexity++;
            } else if (node instanceof WhileStmt) {
                complexity++;
            } else if (node instanceof DoStmt) {
                complexity++;
            } else if (node instanceof SwitchEntry entry) {
                if (!entry.getLabels().isEmpty()) {
                    complexity++;
                }
            } else if (node instanceof CatchClause) {
                complexity++;
            } else if (node instanceof ConditionalExpr) {
                complexity++;
            } else if (node instanceof BinaryExpr binaryExpr) {
                if (binaryExpr.getOperator() == BinaryExpr.Operator.AND
                        || binaryExpr.getOperator() == BinaryExpr.Operator.OR) {
                    complexity++;
                }
            }
        }
        return complexity;
    }

    /**
     * Cognitive complexity: increments for each flow break, with nesting adding to the increment.
     */
    public int cognitiveComplexity(MethodDeclaration method) {
        return cognitiveWalk(method, 0);
    }

    private int cognitiveWalk(Node node, int nesting) {
        int total = 0;
        for (var child : node.getChildNodes()) {
            if (child instanceof IfStmt ifStmt) {
                total += 1 + nesting;
                // Walk the then-branch at increased nesting
                total += cognitiveWalk(ifStmt.getThenStmt(), nesting + 1);
                // else-if doesn't increase nesting, else adds 1
                if (ifStmt.getElseStmt().isPresent()) {
                    var elseStmt = ifStmt.getElseStmt().get();
                    if (elseStmt instanceof IfStmt) {
                        // else-if: count as a separate IfStmt at same nesting
                        total += cognitiveWalk(elseStmt, nesting);
                    } else {
                        total += 1; // else keyword
                        total += cognitiveWalk(elseStmt, nesting + 1);
                    }
                }
            } else if (child instanceof ForStmt || child instanceof ForEachStmt
                    || child instanceof WhileStmt || child instanceof DoStmt) {
                total += 1 + nesting;
                total += cognitiveWalk(child, nesting + 1);
            } else if (child instanceof CatchClause) {
                total += 1 + nesting;
                total += cognitiveWalk(child, nesting + 1);
            } else {
                total += cognitiveWalk(child, nesting);
            }
        }
        return total;
    }

    /**
     * Counts lines of code for a type declaration (non-blank lines).
     */
    public int linesOfCode(TypeDeclaration<?> type) {
        return countNonBlankLines(type);
    }

    /**
     * Counts lines of code for a method declaration.
     */
    public int linesOfCode(MethodDeclaration method) {
        return countNonBlankLines(method);
    }

    private int countNonBlankLines(Node node) {
        var range = node.getRange();
        if (range.isEmpty()) {
            return 0;
        }
        var begin = range.get().begin.line;
        var end = range.get().end.line;
        String source = node.toString();
        int count = 0;
        for (String line : source.split("\n")) {
            if (!line.isBlank()) {
                count++;
            }
        }
        return count;
    }
}
