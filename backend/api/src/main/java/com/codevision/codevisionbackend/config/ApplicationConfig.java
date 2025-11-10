package com.codevision.codevisionbackend.config;

import com.codevision.codevisionbackend.analysis.config.CompiledAnalysisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
    GitAuthProperties.class,
    SecurityProperties.class,
    SecurityScanProperties.class,
    DiagramStorageProperties.class,
    AnalysisJobExecutorProperties.class,
    CompiledAnalysisProperties.class
})
public class ApplicationConfig {}
