package com.codevision.codevisionbackend.project.api;

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
@Table(name = "api_endpoint")
@Getter
@Setter
@NoArgsConstructor
public class ApiEndpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "protocol", nullable = false)
    private String protocol;

    @Column(name = "http_method")
    private String httpMethod;

    @Column(name = "path_or_operation", nullable = false, length = 512)
    private String pathOrOperation;

    @Column(name = "controller_class", nullable = false, length = 512)
    private String controllerClass;

    @Column(name = "controller_method", length = 512)
    private String controllerMethod;

    @Column(name = "spec_artifacts_json", columnDefinition = "text")
    private String specArtifactsJson;
}
