package com.codevision.codevisionbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AnalysisJobExecutorConfig {

    private final AnalysisJobExecutorProperties properties;

    public AnalysisJobExecutorConfig(AnalysisJobExecutorProperties properties) {
        this.properties = properties;
    }

    @Bean(name = "analysisJobExecutor")
    public ThreadPoolTaskExecutor analysisJobExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("analysis-job-");
        int corePoolSize = Math.max(1, properties.getCorePoolSize());
        int maxPoolSize = Math.max(corePoolSize, properties.getMaxPoolSize());
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(Math.max(0, properties.getQueueCapacity()));
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
