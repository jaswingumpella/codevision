package com.codevision.codevisionbackend.project.metadata;

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
@Table(name = "class_metadata")
@Getter
@Setter
@NoArgsConstructor
public class ClassMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "fully_qualified_name", nullable = false)
    private String fullyQualifiedName;

    @Column(name = "package_name")
    private String packageName;

    @Column(name = "class_name", nullable = false)
    private String className;

    @Column(name = "stereotype", nullable = false)
    private String stereotype;

    @Column(name = "source_set", nullable = false)
    private String sourceSet;

    @Column(name = "relative_path")
    private String relativePath;

    @Column(name = "user_code", nullable = false)
    private boolean userCode;

    @Column(name = "annotations_json", columnDefinition = "text")
    private String annotationsJson;

    @Column(name = "interfaces_json", columnDefinition = "text")
    private String interfacesJson;
}
