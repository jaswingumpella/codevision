package com.codevision.codevisionbackend.analysis.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class AnalysisEntityFieldId implements Serializable {

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "name")
    private String name;

    public AnalysisEntityFieldId() {}

    public AnalysisEntityFieldId(Long entityId, String name) {
        this.entityId = entityId;
        this.name = name;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnalysisEntityFieldId that = (AnalysisEntityFieldId) o;
        return Objects.equals(entityId, that.entityId) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityId, name);
    }
}
