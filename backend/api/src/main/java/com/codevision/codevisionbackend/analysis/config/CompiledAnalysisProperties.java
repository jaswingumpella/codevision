package com.codevision.codevisionbackend.analysis.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties that control the compiled artefact analysis pipeline.
 */
@ConfigurationProperties(prefix = "analysis")
public class CompiledAnalysisProperties {

    private List<String> acceptPackages = new ArrayList<>(List.of("com.barclays", "com.codeviz2"));
    private boolean includeDependencies = true;
    private int maxCallDepth = 8;
    private CompileProperties compile = new CompileProperties();
    private OutputProperties output = new OutputProperties();
    private SafetyProperties safety = new SafetyProperties();
    private FiltersProperties filters = new FiltersProperties();

    public List<String> getAcceptPackages() {
        return acceptPackages;
    }

    public void setAcceptPackages(List<String> acceptPackages) {
        this.acceptPackages = acceptPackages == null ? new ArrayList<>() : new ArrayList<>(acceptPackages);
    }

    public boolean isIncludeDependencies() {
        return includeDependencies;
    }

    public void setIncludeDependencies(boolean includeDependencies) {
        this.includeDependencies = includeDependencies;
    }

    public int getMaxCallDepth() {
        return maxCallDepth;
    }

    public void setMaxCallDepth(int maxCallDepth) {
        this.maxCallDepth = maxCallDepth;
    }

    public CompileProperties getCompile() {
        return compile;
    }

    public void setCompile(CompileProperties compile) {
        this.compile = compile;
    }

    public OutputProperties getOutput() {
        return output;
    }

    public void setOutput(OutputProperties output) {
        this.output = output;
    }

    public SafetyProperties getSafety() {
        return safety;
    }

    public void setSafety(SafetyProperties safety) {
        this.safety = safety;
    }

    public FiltersProperties getFilters() {
        return filters;
    }

    public void setFilters(FiltersProperties filters) {
        this.filters = filters;
    }

    public static class CompileProperties {
        private boolean auto = true;
        private String mvnExecutable = "mvn";

        public boolean isAuto() {
            return auto;
        }

        public void setAuto(boolean auto) {
            this.auto = auto;
        }

        public String getMvnExecutable() {
            return mvnExecutable;
        }

        public void setMvnExecutable(String mvnExecutable) {
            this.mvnExecutable = mvnExecutable;
        }
    }

    public static class OutputProperties {
        private List<String> formats = new ArrayList<>(Arrays.asList("json", "csv", "plantuml", "mermaid"));

        public List<String> getFormats() {
            return formats;
        }

        public void setFormats(List<String> formats) {
            this.formats = formats == null ? new ArrayList<>() : new ArrayList<>(formats);
        }
    }

    public static class SafetyProperties {
        private long maxRuntimeSeconds = 600;
        private int maxHeapMb = 1500;

        public long getMaxRuntimeSeconds() {
            return maxRuntimeSeconds;
        }

        public void setMaxRuntimeSeconds(long maxRuntimeSeconds) {
            this.maxRuntimeSeconds = maxRuntimeSeconds;
        }

        public int getMaxHeapMb() {
            return maxHeapMb;
        }

        public void setMaxHeapMb(int maxHeapMb) {
            this.maxHeapMb = maxHeapMb;
        }
    }

    public static class FiltersProperties {
        private List<String> excludeJars = new ArrayList<>(List.of("*junit*", "*hamcrest*", "*mockito*"));

        public List<String> getExcludeJars() {
            return excludeJars;
        }

        public void setExcludeJars(List<String> excludeJars) {
            this.excludeJars = excludeJars == null ? new ArrayList<>() : new ArrayList<>(excludeJars);
        }
    }
}
