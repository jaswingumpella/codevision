package com.codevision.codevisionbackend.analyze.scanner;

import java.nio.file.Path;
import java.util.List;

public interface DaoAnalysisService {

    DbAnalysisResult analyze(Path repoRoot, List<Path> moduleRoots, List<DbEntityRecord> entities);
}

