package com.codevision.codevisionbackend.analysis;

import com.codevision.codevisionbackend.analysis.persistence.CompiledAnalysisRun;

public record CompiledAnalysisResult(CompiledAnalysisRun run, AnalysisOutputPaths outputs, GraphModel graphModel) {}
