package com.codevision.fixtures.persistence;

import com.codevision.fixtures.domain.FixtureEntity;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class FixtureRepository {

    public List<String> fetchAll() {
        return List.of("alpha", "beta");
    }

    public void store(FixtureEntity entity) {
        entity.touch();
    }

    public void audit(String payload) {
        // no-op
    }

    public void batch() {
        // no-op
    }
}
