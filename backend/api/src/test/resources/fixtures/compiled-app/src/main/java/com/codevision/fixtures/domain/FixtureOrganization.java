package com.codevision.fixtures.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fixture_organizations")
public class FixtureOrganization {

    @Id
    private Long id;

    @OneToMany
    private List<FixtureEntity> members = new ArrayList<>();
}
