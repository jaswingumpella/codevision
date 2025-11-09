package com.codevision.codevisionbackend.analyze;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GherkinScenarioSummary(String name, String scenarioType, List<String> steps) {

    public GherkinScenarioSummary {
        steps = steps == null ? List.of() : List.copyOf(steps);
    }
}
