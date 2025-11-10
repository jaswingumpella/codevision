package com.codevision.codevisionbackend.analysis.web.model;

import java.util.List;

public class PageResponse<T> {

    private List<T> items;
    private long totalElements;
    private int page;
    private int size;

    public PageResponse() {}

    public PageResponse(List<T> items, long totalElements, int page, int size) {
        this.items = items;
        this.totalElements = totalElements;
        this.page = page;
        this.size = size;
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
