package com.codevision.codevisionbackend.analysis.web.model;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class CompiledAnalysisRequest {

    @NotBlank
    private String repoPath;

    private List<String> acceptPackages;

    private Boolean includeDependencies;

    public String getRepoPath() {
        return repoPath;
    }

    public void setRepoPath(String repoPath) {
        this.repoPath = repoPath;
    }

    public List<String> getAcceptPackages() {
        return acceptPackages;
    }

    public void setAcceptPackages(List<String> acceptPackages) {
        this.acceptPackages = acceptPackages;
    }

    public Boolean getIncludeDependencies() {
        return includeDependencies;
    }

    public void setIncludeDependencies(Boolean includeDependencies) {
        this.includeDependencies = includeDependencies;
    }
}
