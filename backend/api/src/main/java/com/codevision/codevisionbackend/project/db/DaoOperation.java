package com.codevision.codevisionbackend.project.db;

import com.codevision.codevisionbackend.project.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "dao_operation")
@Getter
@Setter
@NoArgsConstructor
public class DaoOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "repository_class", nullable = false, length = 512)
    private String repositoryClass;

    @Column(name = "method_name", nullable = false, length = 255)
    private String methodName;

    @Column(name = "operation_type", nullable = false, length = 64)
    private String operationType;

    @Column(name = "target_descriptor", length = 512)
    private String targetDescriptor;

    @Lob
    @Column(name = "query_snippet", columnDefinition = "CLOB")
    private String querySnippet;
}

