package com.codevision.codevisionbackend.analyze.scanner;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AssetScanner {

    private static final Logger log = LoggerFactory.getLogger(AssetScanner.class);
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg", ".gif", ".svg");
    private static final Set<String> IGNORED_DIRECTORIES =
            Set.of(".git", "target", "build", "node_modules", ".idea", ".gradle");

    public List<ImageAssetRecord> scan(Path repoRoot) {
        if (repoRoot == null || !Files.exists(repoRoot)) {
            return List.of();
        }

        Path normalizedRoot = repoRoot.toAbsolutePath().normalize();
        List<ImageAssetRecord> assets = new ArrayList<>();

        FileVisitor<Path> visitor = new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                Path name = dir.getFileName();
                if (name != null && IGNORED_DIRECTORIES.contains(name.toString())) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (!attrs.isRegularFile()) {
                    return FileVisitResult.CONTINUE;
                }
                String fileName = file.getFileName().toString();
                String lower = fileName.toLowerCase(Locale.ROOT);
                if (!isImage(lower)) {
                    return FileVisitResult.CONTINUE;
                }
                try {
                    long size = Files.size(file);
                    String relativePath = computeRelativePath(normalizedRoot, file);
                    String sha256 = computeSha256(file);
                    assets.add(new ImageAssetRecord(fileName, relativePath, size, sha256));
                } catch (IOException ex) {
                    log.debug("Failed to analyze asset {}: {}", file, ex.getMessage());
                }
                return FileVisitResult.CONTINUE;
            }
        };

        try {
            Files.walkFileTree(normalizedRoot, visitor);
        } catch (IOException e) {
            log.warn("Failed walking repository {} for assets: {}", repoRoot, e.getMessage());
        }
        return assets;
    }

    private boolean isImage(String fileName) {
        return IMAGE_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    private String computeRelativePath(Path root, Path file) {
        Path normalizedFile = file.toAbsolutePath().normalize();
        try {
            return root.relativize(normalizedFile).toString();
        } catch (IllegalArgumentException ex) {
            return file.getFileName().toString();
        }
    }

    private String computeSha256(Path file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(file);
                    DigestInputStream digestStream = new DigestInputStream(input, digest)) {
                byte[] buffer = new byte[8192];
                while (digestStream.read(buffer) != -1) {
                    // consume stream to update digest
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException ex) {
            log.debug("Failed to compute hash for {}: {}", file, ex.getMessage());
            return null;
        }
    }
}

