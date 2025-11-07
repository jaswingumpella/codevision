package com.codevision.codevisionbackend.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.scan")
@Data
public class SecurityScanProperties {

    private List<Rule> rules = new ArrayList<>();
    private List<String> ignorePatterns = new ArrayList<>();

    @Data
    public static class Rule {

        /**
         * Literal keyword to search for (case insensitive). Optional when {@link #regex} is provided.
         */
        private String keyword;

        /**
         * Regular expression applied to each line. Optional when {@link #keyword} is provided.
         */
        private String regex;

        /**
         * Classification for the match, e.g. {@code PII} or {@code PCI}.
         */
        private String type;

        /**
         * Severity indicator such as {@code LOW}, {@code MEDIUM}, or {@code HIGH}.
         */
        private String severity;
    }
}
