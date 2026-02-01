package com.codevision.codevisionbackend.analyze.scanner;

import java.nio.file.Path;
import java.util.Locale;

public final class AnalysisExclusions {

    private static final String[] TEST_PATH_MARKERS = {
            "/src/test/",
            "/src/it/",
            "/src/integration-test/",
            "/src/integrationtest/"
    };
    private static final String[] MOCK_PATH_MARKERS = {
            "/mock/",
            "/mocks/"
    };

    private AnalysisExclusions() {
    }

    public static boolean isTestPath(Path path) {
        return isTestPath(normalizePath(path));
    }

    public static boolean isTestPath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        String normalized = normalizePath(path);
        for (String marker : TEST_PATH_MARKERS) {
            if (normalized.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isMockPath(Path path) {
        return isMockPath(normalizePath(path));
    }

    public static boolean isMockPath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        String normalized = normalizePath(path);
        for (String marker : MOCK_PATH_MARKERS) {
            if (normalized.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isMockClassName(String className) {
        if (className == null || className.isBlank()) {
            return false;
        }
        String simple = extractSimpleName(className);
        return simple.toLowerCase(Locale.ROOT).contains("mock");
    }

    public static boolean isExcludedPath(Path path) {
        return isTestPath(path) || isMockPath(path);
    }

    public static boolean isExcludedPath(String path) {
        return isTestPath(path) || isMockPath(path);
    }

    public static boolean shouldExclude(Path sourceFile, String className) {
        return isExcludedPath(sourceFile) || isMockClassName(className);
    }

    private static String extractSimpleName(String className) {
        String normalized = className;
        int lastDot = normalized.lastIndexOf('.');
        if (lastDot >= 0 && lastDot + 1 < normalized.length()) {
            normalized = normalized.substring(lastDot + 1);
        }
        int lastDollar = normalized.lastIndexOf('$');
        if (lastDollar >= 0 && lastDollar + 1 < normalized.length()) {
            normalized = normalized.substring(lastDollar + 1);
        }
        return normalized;
    }

    private static String normalizePath(Path path) {
        if (path == null) {
            return "";
        }
        return normalizePath(path.toString());
    }

    private static String normalizePath(String path) {
        if (path == null) {
            return "";
        }
        return path.replace('\\', '/').toLowerCase(Locale.ROOT);
    }
}
