package com.codevision.codevisionbackend.analyze;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GherkinFeatureSummary(
        String featureFile, String featureTitle, List<GherkinScenarioSummary> scenarios) {

    public GherkinFeatureSummary {
        scenarios = scenarios == null ? List.of() : List.copyOf(scenarios);
    }
}
