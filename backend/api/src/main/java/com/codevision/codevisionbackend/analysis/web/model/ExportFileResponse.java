package com.codevision.codevisionbackend.analysis.web.model;

public class ExportFileResponse {

    private String name;
    private long size;
    private String downloadUrl;

    public ExportFileResponse() {}

    public ExportFileResponse(String name, long size, String downloadUrl) {
        this.name = name;
        this.size = size;
        this.downloadUrl = downloadUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }
}
