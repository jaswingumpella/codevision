package com.codevision.codevisionbackend.project.logger;

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
@Table(name = "log_statement")
@Getter
@Setter
@NoArgsConstructor
public class LogStatement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "class_name", nullable = false)
    private String className;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "log_level", nullable = false)
    private String logLevel;

    @Column(name = "line_number")
    private Integer lineNumber;

    @Column(name = "message_template", length = 2000)
    private String messageTemplate;

    @Lob
    @Column(name = "variables_json", columnDefinition = "CLOB")
    private String variablesJson;

    @Column(name = "pii_risk", nullable = false)
    private boolean piiRisk;

    @Column(name = "pci_risk", nullable = false)
    private boolean pciRisk;
}
