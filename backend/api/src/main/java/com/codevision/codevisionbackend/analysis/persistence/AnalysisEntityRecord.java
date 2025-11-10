package com.codevision.codevisionbackend.analysis.persistence;

import com.codevision.codevisionbackend.analysis.GraphModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "entity")
public class AnalysisEntityRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_name", nullable = false, unique = true)
    private String className;

    @Column(name = "package_name", nullable = false)
    private String packageName;

    @Column(name = "jar_or_dir")
    private String jarOrDir;

    @Column(name = "table_name")
    private String tableName;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin", nullable = false)
    private GraphModel.Origin origin;

    @Column(name = "scc_id")
    private Long sccId;

    @Column(name = "in_cycle")
    private boolean inCycle;

    public Long getId() {
        return id;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getJarOrDir() {
        return jarOrDir;
    }

    public void setJarOrDir(String jarOrDir) {
        this.jarOrDir = jarOrDir;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public GraphModel.Origin getOrigin() {
        return origin;
    }

    public void setOrigin(GraphModel.Origin origin) {
        this.origin = origin;
    }

    public Long getSccId() {
        return sccId;
    }

    public void setSccId(Long sccId) {
        this.sccId = sccId;
    }

    public boolean isInCycle() {
        return inCycle;
    }

    public void setInCycle(boolean inCycle) {
        this.inCycle = inCycle;
    }
}
