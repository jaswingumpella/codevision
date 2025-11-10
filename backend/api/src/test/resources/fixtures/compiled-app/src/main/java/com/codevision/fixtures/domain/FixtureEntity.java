package com.codevision.fixtures.domain;

import com.codevision.fixtures.service.FixtureService;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

@Entity
@Table(name = "fixture_entities")
@SequenceGenerator(
        name = "fixture_seq",
        sequenceName = "fixture_seq",
        allocationSize = 50,
        initialValue = 200)
public class FixtureEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fixture_seq")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "org_id")
    private FixtureOrganization organization;

    @Autowired
    private FixtureService service;

    @OneToMany
    private List<FixtureOrganization> relatedOrganizations = new ArrayList<>();

    public FixtureEntity() {}

    public FixtureEntity(Long id) {
        this.id = id;
    }

    public void touch() {
        if (service != null) {
            service.record("entity-touch" + id);
        }
    }
}
