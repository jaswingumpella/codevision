package com.codevision.codevisionbackend.analysis;

import java.nio.file.Path;

public record ExportedFile(String name, long size, Path path) {}
