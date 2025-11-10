package com.codevision.codevisionbackend.analysis.persistence;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "entity_field")
public class AnalysisEntityFieldRecord {

    @EmbeddedId
    private AnalysisEntityFieldId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("entityId")
    @JoinColumn(name = "entity_id")
    private AnalysisEntityRecord entity;

    private String type;

    @jakarta.persistence.Column(name = "is_join")
    private boolean joinField;

    public AnalysisEntityFieldId getId() {
        return id;
    }

    public void setId(AnalysisEntityFieldId id) {
        this.id = id;
    }

    public AnalysisEntityRecord getEntity() {
        return entity;
    }

    public void setEntity(AnalysisEntityRecord entity) {
        this.entity = entity;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isJoinField() {
        return joinField;
    }

    public void setJoinField(boolean joinField) {
        this.joinField = joinField;
    }
}
