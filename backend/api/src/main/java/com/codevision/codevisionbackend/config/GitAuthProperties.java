package com.codevision.codevisionbackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "git.auth")
@Data
public class GitAuthProperties {

    private String username;
    private String token;
}
