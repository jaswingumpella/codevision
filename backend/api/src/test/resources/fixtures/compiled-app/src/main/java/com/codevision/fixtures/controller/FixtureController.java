package com.codevision.fixtures.controller;

import com.codevision.fixtures.service.FixtureService;
import java.util.List;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/fixtures", produces = "application/json")
public class FixtureController {

    private final FixtureService fixtureService;

    public FixtureController(FixtureService fixtureService) {
        this.fixtureService = fixtureService;
    }

    @GetMapping
    public List<String> getFixtures() {
        return fixtureService.fetchNames();
    }

    @PostMapping(path = "/{id}", consumes = "application/json")
    public void updateFixture(@PathVariable Long id) {
        fixtureService.save(id);
    }

    @KafkaListener(topics = {"fixtures.events", "audit.events"})
    public void onKafkaMessage(String payload) {
        fixtureService.record(payload);
    }

    @Scheduled(cron = "0/5 * * * * *")
    public void scheduledRun() {
        fixtureService.runBatch();
    }
}
