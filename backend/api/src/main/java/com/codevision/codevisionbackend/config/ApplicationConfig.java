package com.codevision.codevisionbackend.config;

import com.codevision.codevisionbackend.analysis.config.CompiledAnalysisProperties;
import com.codevision.codevisionbackend.graph.export.PdfExportProperties;
import com.codevision.codevisionbackend.graph.export.SvgExportProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
    GitAuthProperties.class,
    SecurityProperties.class,
    SecurityScanProperties.class,
    DiagramStorageProperties.class,
    AnalysisJobExecutorProperties.class,
    CompiledAnalysisProperties.class,
    PdfExportProperties.class,
    SvgExportProperties.class,
    AnalysisSafetyProperties.class,
    TreeSitterProperties.class
})
public class ApplicationConfig {}
