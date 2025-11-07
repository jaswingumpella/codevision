package com.codevision.codevisionbackend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({GitAuthProperties.class, SecurityProperties.class, SecurityScanProperties.class})
public class ApplicationConfig {
}
