package com.codevision.codevisionbackend.analyze;

import com.codevision.codevisionbackend.project.Project;

public record AnalysisOutcome(Project project, ParsedDataResponse parsedData) {
}
