package com.codevision.codevisionbackend.project.db;

import com.codevision.codevisionbackend.project.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "db_entity")
@Getter
@Setter
@NoArgsConstructor
public class DbEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "entity_name", nullable = false, length = 256)
    private String entityName;

    @Column(name = "fully_qualified_name", length = 512)
    private String fullyQualifiedName;

    @Column(name = "table_name", length = 256)
    private String tableName;

    @Column(name = "primary_keys_json", columnDefinition = "text")
    private String primaryKeysJson;

    @Column(name = "fields_json", columnDefinition = "text")
    private String fieldsJson;

    @Column(name = "relationships_json", columnDefinition = "text")
    private String relationshipsJson;
}
