package com.example.footprint.data.model;

import java.util.List;

public class PageResponse<T> {
    public List<T> content;
    private int number;

    private int page;
    private int size;
    public boolean hasNext;
    private long totalElements;
    private int totalPages;
    private boolean last;

    public List<T> getContent() { return content; }
    public int getNumber() { return number; }
    public boolean isLast() { return last; }
    public long getTotalElements() { return totalElements; }
}