package com.codevision.codevisionbackend.analyze.scanner;

public record ImageAssetRecord(String fileName, String relativePath, long sizeBytes, String sha256) {}

