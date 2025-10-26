package com.codevision.codevisionbackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security")
@Data
public class SecurityProperties {

    private String apiKey;
}
