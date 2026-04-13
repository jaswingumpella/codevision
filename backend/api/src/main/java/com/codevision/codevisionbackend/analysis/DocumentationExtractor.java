package com.codevision.codevisionbackend.analysis;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Extracts Javadoc documentation from Java AST nodes parsed by JavaParser.
 */
@Component
public class DocumentationExtractor {

    /**
     * Extracts the main description from a class/interface/enum Javadoc comment.
     */
    public Optional<String> extractClassDocumentation(TypeDeclaration<?> type) {
        return type.getJavadoc().map(this::descriptionText).filter(s -> !s.isBlank());
    }

    /**
     * Extracts the main description from a method Javadoc comment.
     */
    public Optional<String> extractMethodDocumentation(MethodDeclaration method) {
        return method.getJavadoc().map(this::descriptionText).filter(s -> !s.isBlank());
    }

    /**
     * Extracts the @author tag value from a type's Javadoc.
     */
    public Optional<String> extractAuthor(TypeDeclaration<?> type) {
        return extractBlockTag(type, "author");
    }

    /**
     * Extracts the @since tag value from a type's Javadoc.
     */
    public Optional<String> extractSince(TypeDeclaration<?> type) {
        return extractBlockTag(type, "since");
    }

    private String descriptionText(Javadoc javadoc) {
        return javadoc.getDescription().toText().trim();
    }

    private Optional<String> extractBlockTag(TypeDeclaration<?> type, String tagName) {
        return type.getJavadoc()
                .flatMap(javadoc -> javadoc.getBlockTags().stream()
                        .filter(tag -> tag.getTagName().equals(tagName))
                        .map(JavadocBlockTag::getContent)
                        .map(content -> content.toText().trim())
                        .filter(text -> !text.isBlank())
                        .findFirst());
    }
}
