package com.codevision.fixtures.config;

import com.codevision.fixtures.controller.FixtureController;
import com.codevision.fixtures.service.FixtureService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FixtureConfig {

    @Bean
    public FixtureController fixtureController(FixtureService service) {
        return new FixtureController(service);
    }
}
