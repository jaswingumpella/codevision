package com.codevision.codevisionbackend.analysis.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class EntitySequenceUsageId implements Serializable {

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "field_name")
    private String fieldName;

    public EntitySequenceUsageId() {}

    public EntitySequenceUsageId(Long entityId, String fieldName) {
        this.entityId = entityId;
        this.fieldName = fieldName;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntitySequenceUsageId that = (EntitySequenceUsageId) o;
        return Objects.equals(entityId, that.entityId) && Objects.equals(fieldName, that.fieldName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityId, fieldName);
    }
}
