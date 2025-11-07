package com.codevision.codevisionbackend.project.security;

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
@Table(name = "pii_pci_finding")
@Getter
@Setter
@NoArgsConstructor
public class PiiPciFinding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "line_number")
    private Integer lineNumber;

    @Lob
    @Column(name = "snippet", columnDefinition = "CLOB")
    private String snippet;

    @Column(name = "match_type", nullable = false)
    private String matchType;

    @Column(name = "severity", nullable = false)
    private String severity;

    @Column(name = "ignored", nullable = false)
    private boolean ignored;
}
