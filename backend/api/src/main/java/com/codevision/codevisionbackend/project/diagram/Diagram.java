package com.codevision.codevisionbackend.project.diagram;

import com.codevision.codevisionbackend.project.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "diagram")
@Getter
@Setter
@NoArgsConstructor
public class Diagram {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(name = "diagram_type", nullable = false, length = 32)
    private DiagramType diagramType;

    @Column(name = "title", nullable = false, length = 256)
    private String title;

    @Column(name = "sequence_order")
    private int sequenceOrder;

    @Lob
    @Column(name = "plantuml_source")
    private String plantumlSource;

    @Lob
    @Column(name = "mermaid_source")
    private String mermaidSource;

    @Column(name = "svg_path", length = 1024)
    private String svgPath;

    @Lob
    @Column(name = "metadata_json")
    private String metadataJson;
}
