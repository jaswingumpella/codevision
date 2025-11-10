package com.codevision.codevisionbackend.analysis.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "class_dep")
public class ClassDependencyRecord {

    @EmbeddedId
    private ClassDependencyId id;

    @Column(name = "package_filter")
    private String packageFilter;

    public ClassDependencyRecord() {}

    public ClassDependencyRecord(ClassDependencyId id, String packageFilter) {
        this.id = id;
        this.packageFilter = packageFilter;
    }

    public ClassDependencyId getId() {
        return id;
    }

    public void setId(ClassDependencyId id) {
        this.id = id;
    }

    public String getPackageFilter() {
        return packageFilter;
    }

    public void setPackageFilter(String packageFilter) {
        this.packageFilter = packageFilter;
    }
}
