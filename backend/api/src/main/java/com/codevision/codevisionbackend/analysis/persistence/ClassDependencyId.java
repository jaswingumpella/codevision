package com.codevision.codevisionbackend.analysis.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ClassDependencyId implements Serializable {

    @Column(name = "caller")
    private String caller;

    @Column(name = "callee")
    private String callee;

    public ClassDependencyId() {}

    public ClassDependencyId(String caller, String callee) {
        this.caller = caller;
        this.callee = callee;
    }

    public String getCaller() {
        return caller;
    }

    public void setCaller(String caller) {
        this.caller = caller;
    }

    public String getCallee() {
        return callee;
    }

    public void setCallee(String callee) {
        this.callee = callee;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassDependencyId that = (ClassDependencyId) o;
        return Objects.equals(caller, that.caller) && Objects.equals(callee, that.callee);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caller, callee);
    }
}
