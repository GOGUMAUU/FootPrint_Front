package com.example.footprint.data.model;

import java.io.Serializable;

public class MapFilterState implements Serializable {
    private String period;   // null(전체), "7d", "30d"
    private String ageGroup; // null(전체), "10s", "20s", "30s"
    private String sort;     // null(기본), "latest", "popular"

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public String getAgeGroup() { return ageGroup; }
    public void setAgeGroup(String ageGroup) { this.ageGroup = ageGroup; }

    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }

    public void reset() {
        this.period = null;
        this.ageGroup = null;
        this.sort = null;
    }
}
