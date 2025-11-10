package com.codevision.codevisionbackend.analysis.persistence;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "entity_uses_sequence")
public class EntitySequenceUsageRecord {

    @EmbeddedId
    private EntitySequenceUsageId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("entityId")
    @JoinColumn(name = "entity_id")
    private AnalysisEntityRecord entity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sequence_id")
    private SequenceRecord sequence;

    public EntitySequenceUsageId getId() {
        return id;
    }

    public void setId(EntitySequenceUsageId id) {
        this.id = id;
    }

    public AnalysisEntityRecord getEntity() {
        return entity;
    }

    public void setEntity(AnalysisEntityRecord entity) {
        this.entity = entity;
    }

    public SequenceRecord getSequence() {
        return sequence;
    }

    public void setSequence(SequenceRecord sequence) {
        this.sequence = sequence;
    }
}
