package com.codevision.fixtures.service;

import com.codevision.fixtures.domain.FixtureEntity;
import com.codevision.fixtures.persistence.FixtureRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FixtureService {

    @Autowired
    private FixtureRepository repository;

    public List<String> fetchNames() {
        return repository.fetchAll();
    }

    public void save(Long id) {
        repository.store(new FixtureEntity(id));
    }

    public void record(String payload) {
        repository.audit(payload);
    }

    public void runBatch() {
        repository.batch();
    }
}
