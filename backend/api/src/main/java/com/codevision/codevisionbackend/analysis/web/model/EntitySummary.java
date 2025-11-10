package com.codevision.codevisionbackend.analysis.web.model;

public class EntitySummary {

    private String className;
    private String packageName;
    private String tableName;
    private String origin;
    private Long sccId;
    private boolean inCycle;

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

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
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
